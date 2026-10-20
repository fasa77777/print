package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKyocera;

import co.kr.up.util.Mask;

/**
 * 교세라 사이버머니 결제 (/stech/kyocera/postCyberMoneyPayment).
 */
public class CyberMoneyKyoceraPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyKyoceraPaymentSQL.class);

	/**
	 * 교세라 사이버머니로 결제한다. CyberMoneyPaymentSQL 과 같고,
	 * 컬러 값을 1(컬러)/2(흑백)에서 true/false 로 바꿔 기록하는 점만 다르다.
	 */
	public CyberMoneyResult setPayment(PaymentKyocera payment) {
		String card = payment.getUserCard();
		Integer amount = CyberMoneyLedger.parseMoney(payment.getTotalAmount());
		if (amount == null || amount < 0) {
			logger.info("사이버머니 결제 금액 오류: [" + payment.getTotalAmount() + "]");
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

			// 같은 결제번호로 이미 처리된 결제는 다시 차감하지 않는다.
			if (CyberMoneyLedger.isDuplicate(conn, card, payment.getCyberNo())) {
				conn.commit();
				return new CyberMoneyResult(Status.DUPLICATE, balance == null ? 0 : balance);
			}

			if (balance == null) {
				CyberMoneyLedger.registerCard(conn, card, payment.getTerminalID());
				conn.commit();
				return new CyberMoneyResult(Status.NO_CARD, 0);
			}

			if (!CyberMoneyLedger.deduct(conn, card, amount)) {
				conn.commit();
				logger.info("사이버머니 잔액 부족 card=" + Mask.card(card) + " balance=" + balance + " amount=" + amount);
				return new CyberMoneyResult(Status.INSUFFICIENT, balance);
			}

			// 교세라 에이전트는 컬러를 1(컬러) / 2(흑백)로 보낸다.
			if ("1".equals(payment.getColor())) {
				payment.setColor("true");
			} else if ("2".equals(payment.getColor())) {
				payment.setColor("false");
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
			logger.error("CyberMoneyKyoceraPaymentSQL 처리 중 오류", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return new CyberMoneyResult(Status.ERROR, 0);
		} finally {
			dbcp.freeConnection(conn);
		}
	}

	/** 카드의 현재 잔액. 등록되지 않았거나 오류면 빈 문자열. */
	public String reMoney(String card) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return "";
		}
		try {
			Integer balance = CyberMoneyLedger.findBalance(conn, card);
			return balance == null ? "" : String.valueOf(balance);
		} catch (Exception e) {
			logger.error("CyberMoneyKyoceraPaymentSQL 처리 중 오류", e);
			return "";
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
