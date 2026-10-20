package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.RecordStatus;
import com.jetwork.model.PrintNuriPay;

/** 프린트누리 결제 기록 (/stech/printnuri/payment). */
public class PrintNuriCreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(PrintNuriCreaditPaymentSQL.class);

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 last_dt 를 갱신한다.
	 * payType 이 Y 면 체크카드, 아니면 신용카드. payTypeTM 이 TM/EB 면 티머니/캐시비. jw_terminal 에 없는 단말기면 기록하지 않고 NO_TERMINAL.
	 */
	public RecordStatus setPayment(PrintNuriPay payment) {
		String payType = "Y".equals(payment.getPayType()) ? "체크카드" : "신용카드";
		if ("TM".equals(payment.getPayTypeTM())) {
			payType = "티머니";
		} else if ("EB".equals(payment.getPayTypeTM())) {
			payType = "캐시비";
		}

		// 카드 결제는 카드사명을 모르면 '해외카드'로 기록한다. 티머니·캐시비는 카드사명을 쓰지 않는다.
		boolean isCard = "체크카드".equals(payType) || "신용카드".equals(payType);
		String cardName = isCard ? CardCompanyName.getCardName(payment.getCraditName())
				: CardCompanyName.getName(CardPaymentLedger.trim(payment.getCraditName()));

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			// 등록되지 않은 단말기의 결제는 기록하지 않고 FAIL 로 응답한다.
			if (!CardPaymentLedger.terminalExists(conn, payment.getTerminalID())) {
				logger.warn("미등록 단말기의 결제 거절 terminalID={} amount={}", payment.getTerminalID(), payment.getPrice());
				return RecordStatus.NO_TERMINAL;
			}
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getPrice(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getPayOK(), "print",
					cardName, payment.getCount());
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			logger.error("PrintNuriCreaditPaymentSQL 처리 중 오류", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
