package com.jetwork.sql;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PrintNuriPay;

/** 프린트누리 결제 기록 (/printnuri/printnuri/payment). */
public class PrintNuriCreaditPaymentSQL {

	public String setPayment(PrintNuriPay payment) {
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
			return "";
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getPrice(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getPayOK(), "print",
					cardName, payment.getCount());
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
		return "";
	}
}
