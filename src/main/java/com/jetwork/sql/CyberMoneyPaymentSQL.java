package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKicc;

import co.kr.up.util.Mask;

/** 사이버머니 결제 (/stech/postCyberMoneyPayment). */
public class CyberMoneyPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyPaymentSQL.class);

	/**
	 * 사이버머니로 결제한다.
	 * 1. 금액이 숫자가 아니면 ERROR, 카드번호가 비었으면 NO_CARD
	 * 2. 같은 결제번호(cyberNo)로 이미 처리됐으면 DUPLICATE (차감하지 않음)
	 * 3. 처음 보는 카드면 0원으로 등록하고 NO_CARD
	 * 4. 잔액이 금액보다 적으면 INSUFFICIENT (차감하지 않음, balance = 잔액 - 금액)
	 * 5. 차감하고 jw_payment 에 기록, 단말기 last_dt 갱신 후 OK (처리 후 잔액 포함). 잔액과 금액이 같으면 0원으로 결제된다
	 */
	public CyberMoneyResult setPayment(PaymentKicc payment) {
		String card = payment.getUserCard();
		Integer amount = CyberMoneyLedger.parseMoney(payment.getTotalAmount());
		if (amount == null || amount < 0) {
			logger.info("사이버머니 결제 금액 오류: [" + payment.getTotalAmount() + "]");
			return new CyberMoneyResult(Status.ERROR, 0);
		}
		// 카드번호가 비어 있으면 빈 카드번호로 cybermoney 에 새로 등록되던 것을 막는다.
		if (card == null || card.trim().isEmpty()) {
			logger.warn("카드번호 없는 사이버머니 결제 terminalID={}", payment.getTerminalID());
			return new CyberMoneyResult(Status.NO_CARD, 0);
		}

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return new CyberMoneyResult(Status.ERROR, 0);
		}
		try {
			conn.setAutoCommit(false);

			Integer balance = CyberMoneyLedger.findBalance(conn, card);

			// 같은 결제번호(cyberNo)로 이미 처리된 결제는 다시 차감하지 않는다.
			if (CyberMoneyLedger.isDuplicate(conn, card, payment.getCyberNo())) {
				conn.commit();
				return new CyberMoneyResult(Status.DUPLICATE, 0); // 응답 balance 는 실서버 jar 와 같이 0
			}

			if (balance == null) {
				// 처음 보는 카드는 0원으로 등록하고 '등록되지 않은 카드'로 응답한다.
				CyberMoneyLedger.registerCard(conn, card, payment.getTerminalID());
				conn.commit();
				return new CyberMoneyResult(Status.NO_CARD, 0);
			}

			if (!CyberMoneyLedger.deduct(conn, card, amount)) {
				conn.commit();
				logger.info("사이버머니 잔액 부족 card=" + Mask.card(card) + " balance=" + balance + " amount=" + amount);
				// 응답 balance 는 실서버 jar 와 같이 "잔액 - 결제 금액" (음수)
				return new CyberMoneyResult(Status.INSUFFICIENT, balance - amount);
			}

			String franName = CyberMoneyLedger.findFranName(conn, payment.getTerminalID());
			CyberMoneyLedger.insertPayment(conn, payment.getTerminalID(), String.valueOf(amount),
					payment.getPaperSize(), payment.getColor(), payment.getCyberNo(), payment.getJobType(), franName,
					card, payment.getCyberNo(), payment.getTotalPages());

			Integer after = CyberMoneyLedger.findBalance(conn, card);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			logger.info("사이버머니 결제 완료 card=" + Mask.card(card) + " amount=" + amount + " balance=" + after);
			return new CyberMoneyResult(Status.OK, after == null ? 0 : after);
		} catch (Exception e) {
			logger.error("CyberMoneyPaymentSQL 처리 중 오류", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return new CyberMoneyResult(Status.ERROR, 0);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
