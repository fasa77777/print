package com.jetwork.sql;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKicc;

import co.kr.up.util.Mask;

/** 사이버머니 결제 (/printnuri/postCyberMoneyPayment). */
public class CyberMoneyPaymentSQL {

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
