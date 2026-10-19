package com.jetwork.sql;

import java.sql.Connection;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentKicc;

/** 신용·체크카드, 티머니, 캐시비 결제 기록 (/printnuri/postCraditPayment). */
public class CreaditPaymentSQL {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public String setPayment(PaymentKicc payment) {
		logger.info("R09=" + payment.getR09() + " R11=" + payment.getR11() + " R33=" + payment.getR33());

		String r11 = payment.getR11();
		boolean isCard = "Y".equals(r11) || "N".equals(r11);

		if (isCard) {
			String payType = "Y".equals(r11) ? "체크카드" : "신용카드";

			// 오늘 같은 승인번호로 이미 기록된 결제가 있으면 다시 기록하지 않는다.
			PaymentCK ck = new PaymentCK();
			if (!ck.setTerminal(payment.getR09())) {
				return "";
			}
			ck.setTerminal1(payment.getR09());

			insertIfNotExistsToday(payment, payType, payment.getR09(), CardCompanyName.getName(payment.getR12()));
		} else {
			String r33 = CardPaymentLedger.trim(payment.getR33());
			// 티머니·캐시비는 승인번호 대신 현재 시각(밀리초)을 기록한다.
			String now = String.valueOf(Calendar.getInstance().getTimeInMillis());
			if ("TM".equals(r33)) {
				insert(payment, "티머니", now, "티머니");
			} else if ("EB".equals(r33)) {
				insert(payment, "캐시비", now, "캐시비");
			}
		}
		return "";
	}

	private void insertIfNotExistsToday(PaymentKicc payment, String payType, String paymentOk, String cardName) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return;
		}
		try {
			conn.setAutoCommit(false);
			if (CardPaymentLedger.existsToday(conn, paymentOk)) {
				System.out.println("중복");
			} else {
				CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
						payment.getPaperSize(), payment.getColor(), paymentOk, payment.getJobType(), cardName,
						payment.getTotalPages());
			}
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
	}

	private void insert(PaymentKicc payment, String payType, String paymentOk, String cardName) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return;
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), paymentOk, payment.getJobType(), cardName,
					payment.getTotalPages());
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
