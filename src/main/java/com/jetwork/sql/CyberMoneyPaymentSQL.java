package com.jetwork.sql;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKicc;

import co.kr.up.util.Mask;

/** 사이버머니 결제 (/printnuri/postCyberMoneyPayment). */
public class CyberMoneyPaymentSQL {

	/**
	 * 사이버머니로 결제한다.
	 * 1. 금액이 숫자가 아니면 ERROR
	 * 2. 처음 보는 카드면 0원으로 등록하고 NO_CARD
	 * 3. 잔액이 금액보다 적으면 INSUFFICIENT (차감하지 않음)
	 * 4. 차감하고 jw_payment 에 기록, last_dt 갱신 후 OK (처리 후 잔액 포함)
	 */
	public CyberMoneyResult setPayment(PaymentKicc payment) {
		String card = payment.getUserCard();
		Integer amount = CyberMoneyLedger.parseMoney(payment.getTotalAmount());
		if (amount == null || amount < 0) {
			System.out.println("사이버머니 결제 금액 오류: [" + payment.getTotalAmount() + "]");
			return new CyberMoneyResult(Status.ERROR, 0);
		}

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return new CyberMoneyResult(Status.ERROR, 0);
		}
		try {
			conn.setAutoCommit(false);

			Integer balance = CyberMoneyLedger.findBalance(conn, card);
			if (balance == null) {
				// 처음 보는 카드는 0원으로 등록하고 '등록되지 않은 카드'로 응답한다.
				CyberMoneyLedger.registerCard(conn, card, payment.getTerminalID());
				conn.commit();
				return new CyberMoneyResult(Status.NO_CARD, 0);
			}

			if (!CyberMoneyLedger.deduct(conn, card, amount)) {
				conn.commit();
				System.out.println("사이버머니 잔액 부족 card=" + Mask.card(card) + " balance=" + balance + " amount=" + amount);
				return new CyberMoneyResult(Status.INSUFFICIENT, balance);
			}

			String franName = CyberMoneyLedger.findFranName(conn, payment.getTerminalID());
			CyberMoneyLedger.insertPayment(conn, payment.getTerminalID(), String.valueOf(amount),
					payment.getPaperSize(), payment.getColor(), payment.getR09(), payment.getJobType(), franName,
					card, payment.getCyberNo(), payment.getTotalPages());

			Integer after = CyberMoneyLedger.findBalance(conn, card);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			System.out.println("사이버머니 결제 완료 card=" + Mask.card(card) + " amount=" + amount + " balance=" + after);
			return new CyberMoneyResult(Status.OK, after == null ? 0 : after);
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
			return new CyberMoneyResult(Status.ERROR, 0);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
