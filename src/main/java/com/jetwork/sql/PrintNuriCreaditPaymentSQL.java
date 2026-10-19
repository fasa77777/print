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

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return "";
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getPrice(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getPayOK(), "print",
					CardCompanyName.getName(CardPaymentLedger.trim(payment.getCraditName())), payment.getCount());
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
