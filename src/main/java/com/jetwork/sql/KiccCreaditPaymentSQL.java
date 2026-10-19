package com.jetwork.sql;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentCreaditKicc;

/** KICC 단말기 결제 기록 (/printnuri/kicc/postCraditPayment). */
public class KiccCreaditPaymentSQL {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 last_dt 를 갱신한다.
	 * R10 이 Y 면 체크카드, 아니면 신용카드. R33 이 TM/EB 면 티머니/캐시비. 반환값은 쓰지 않는다.
	 */
	public String setPayment(PaymentCreaditKicc payment) {
		logger.info("R10=" + payment.getR10() + " R12=" + payment.getR12() + " R33=" + payment.getR33());

		String payType = "Y".equals(payment.getR10()) ? "체크카드" : "신용카드";
		if ("TM".equals(payment.getR33())) {
			payType = "티머니";
		} else if ("EB".equals(payment.getR33())) {
			payType = "캐시비";
		}

		// 카드 결제는 카드사명을 모르면 '해외카드'로 기록한다. 티머니·캐시비는 카드사명을 쓰지 않는다.
		boolean isCard = "체크카드".equals(payType) || "신용카드".equals(payType);
		String cardName = isCard ? CardCompanyName.getCardName(payment.getR12())
				: CardCompanyName.getName(CardPaymentLedger.trim(payment.getR12()));

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return "";
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getR09(), payment.getJobType(),
					cardName, null);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
		return "";
	}
}
