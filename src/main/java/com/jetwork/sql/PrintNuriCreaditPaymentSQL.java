package com.jetwork.sql;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PrintNuriPay;
import com.jetwork.model.RecordStatus;

/** 프린트누리 결제 기록 (/jetwork/printnuri/payment). */
public class PrintNuriCreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(PrintNuriCreaditPaymentSQL.class);

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 갱신한다.
	 * payType 이 Y 면 체크카드, 아니면 신용카드. payTypeTM 이 TM/EB 면 티머니/캐시비.
	 */
	public RecordStatus setPayment(PrintNuriPay payment) {
		String payType = "Y".equals(payment.getPayType()) ? "체크카드" : "신용카드";
		if ("TM".equals(payment.getPayTypeTM())) {
			payType = "티머니";
		} else if ("EB".equals(payment.getPayTypeTM())) {
			payType = "캐시비";
		}

		// 카드 결제는 카드사명을 모르면 '해외카드'로 기록한다.
		// 티머니·캐시비는 카드사명 자리에 "티머니"·"캐시비"를 기록한다 (/postCraditPayment 와 같게. 예전에는 빈칸)
		boolean isCard = "체크카드".equals(payType) || "신용카드".equals(payType);
		String cardName = isCard ? CardCompanyName.getCardName(payment.getCraditName()) : payType;

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getPrice(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getPayOK(), "print",
					cardName, payment.getCount());
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			DbLog.error(logger, "PrintNuriCreaditPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
