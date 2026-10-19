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

	/**
	 * 결제 한 건을 jw_payment 에 기록한다.
	 * - R11 이 Y/N: 체크/신용카드. 오늘 같은 승인번호(R09)가 없을 때만 기록, 카드사명을 모르면 해외카드
	 * - 그 외: R33 이 TM 이면 티머니, EB 면 캐시비, 나머지는 교통카드로 기록
	 * 기록 후 단말기의 last_dt 를 갱신한다. 반환값은 쓰지 않는다 (항상 빈 문자열).
	 */
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

			insertIfNotExistsToday(payment, payType, payment.getR09(), CardCompanyName.getCardName(payment.getR12()));
		} else {
			String r33 = CardPaymentLedger.trim(payment.getR33());
			// 교통카드 결제는 승인번호 대신 현재 시각(밀리초)을 기록한다.
			String now = String.valueOf(Calendar.getInstance().getTimeInMillis());
			if ("TM".equals(r33)) {
				insert(payment, "티머니", now, "티머니");
			} else if ("EB".equals(r33)) {
				insert(payment, "캐시비", now, "캐시비");
			} else {
				// 티머니·캐시비가 아닌 교통카드(레일플러스 등)는 예전에는 기록 없이 버려졌다.
				logger.info("기타 교통카드 결제 R33=" + r33);
				insert(payment, "교통카드", now, "교통카드");
			}
		}
		return "";
	}

	/** 오늘 같은 승인번호의 기록이 없을 때만 기록한다. */
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
				conn.commit();
			} else {
				CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
						payment.getPaperSize(), payment.getColor(), paymentOk, payment.getJobType(), cardName,
						payment.getTotalPages());
				conn.commit();
				CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			}
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
	}

	/** 결제 한 건을 바로 기록한다. */
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
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
