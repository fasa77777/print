package com.jetwork.sql;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.RecordStatus;
import com.jetwork.model.PaymentCreaditKicc;

/** KICC 단말기 결제 기록 (/stech/kicc/postCraditPayment). */
public class KiccCreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(KiccCreaditPaymentSQL.class);

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 last_dt 를 갱신한다.
	 * R10 이 Y 면 체크카드, 아니면 신용카드. R33 이 TM/EB 면 티머니/캐시비. jw_terminal 에 없는 단말기면 기록하지 않고 NO_TERMINAL.
	 */
	public RecordStatus setPayment(PaymentCreaditKicc payment) {
		String payType = "Y".equals(payment.getR10()) ? "체크카드" : "신용카드";
		String r33 = CardPaymentLedger.trim(payment.getR33()); // "TM " 처럼 공백이 붙어 와도 구분되게
		if ("TM".equals(r33)) {
			payType = "티머니";
		} else if ("EB".equals(r33)) {
			payType = "캐시비";
		}

		// 카드 결제는 카드사명을 모르면 '해외카드'로 기록한다. 티머니·캐시비는 카드사명을 쓰지 않는다.
		boolean isCard = "체크카드".equals(payType) || "신용카드".equals(payType);
		String cardName = isCard ? CardCompanyName.getCardName(payment.getR12())
				: CardCompanyName.getName(CardPaymentLedger.trim(payment.getR12()));

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			// 등록되지 않은 단말기의 결제는 기록하지 않고 FAIL 로 응답한다.
			if (!CardPaymentLedger.terminalExists(conn, payment.getTerminalID())) {
				logger.warn("미등록 단말기의 결제 거절 terminalID={} amount={}", payment.getTerminalID(), payment.getTotalAmount());
				return RecordStatus.NO_TERMINAL;
			}
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getR09(), payment.getJobType(),
					cardName, null);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			logger.error("KiccCreaditPaymentSQL 처리 중 오류", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
