package com.jetwork.sql;

import java.sql.Connection;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentKicc;
import com.jetwork.model.RecordStatus;

/** 신용·체크카드, 티머니, 캐시비 결제 기록 (/jetwork/postCraditPayment). */
public class CreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CreaditPaymentSQL.class);

	/** 승인번호별 잠금. 승인번호를 해시해서 256개 잠금 중 하나를 쓴다 (메모리가 늘지 않게) */
	private static final Object[] APPROVAL_LOCKS = new Object[256];
	static {
		for (int i = 0; i < APPROVAL_LOCKS.length; i++) {
			APPROVAL_LOCKS[i] = new Object();
		}
	}

	private static Object approvalLock(String approvalNo) {
		return APPROVAL_LOCKS[(approvalNo.hashCode() & 0x7fffffff) % APPROVAL_LOCKS.length];
	}

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 갱신한다.
	 * - R11 이 Y/N: 체크/신용카드. 오늘 같은 승인번호(R09)가 없을 때만 기록 (승인번호가 비면 그대로 기록),
	 *   카드사명을 모르면 해외카드
	 * - 그 외: R33 이 TM 이면 티머니, EB 면 캐시비, 나머지는 교통카드로 기록
	 * 기록하면 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 갱신한다. (중복이라 기록하지 않으면 갱신하지 않음)
	 */
	public RecordStatus setPayment(PaymentKicc payment) {
		String r11 = payment.getR11();
		boolean isCard = "Y".equals(r11) || "N".equals(r11);

		if (isCard) {
			String payType = "Y".equals(r11) ? "체크카드" : "신용카드";

			String approvalNo = CardPaymentLedger.trim(payment.getR09());
			String cardName = CardCompanyName.getCardName(payment.getR12());
			if (approvalNo.isEmpty()) {
				// 승인번호가 없으면 중복을 가릴 수 없으므로 그대로 기록한다.
				// (예전에는 빈 승인번호끼리 중복으로 보아 하루 첫 건 말고는 버려졌다)
				logger.warn("승인번호(R09) 없는 카드 결제 terminalID={} amount={}", payment.getTerminalID(), payment.getTotalAmount());
				return insert(payment, payType, approvalNo, cardName);
			} else {
				// 오늘 같은 승인번호로 이미 기록된 결제가 있으면 다시 기록하지 않는다.
				// 같은 승인번호의 요청이 동시에 들어와도 한 건만 기록되도록, 승인번호별로 차례로 처리한다.
				// (예전에는 1초 기다린 뒤 중복을 지우는 방식이라 결제마다 1초씩 늦어졌다)
				synchronized (approvalLock(approvalNo)) {
					return insertIfNotExistsToday(payment, payType, approvalNo, cardName);
				}
			}
		} else {
			String r33 = CardPaymentLedger.trim(payment.getR33());
			// 교통카드 결제는 승인번호 대신 현재 시각(밀리초)을 기록한다.
			String now = String.valueOf(Calendar.getInstance().getTimeInMillis());
			if ("TM".equals(r33)) {
				return insert(payment, "티머니", now, "티머니");
			} else if ("EB".equals(r33)) {
				return insert(payment, "캐시비", now, "캐시비");
			} else {
				// 티머니·캐시비가 아닌 교통카드(레일플러스 등)는 예전에는 기록 없이 버려졌다.
				logger.info("기타 교통카드 결제 terminalID={} R33={}", payment.getTerminalID(), r33);
				return insert(payment, "교통카드", now, "교통카드");
			}
		}
	}

	/** 오늘 같은 승인번호의 기록이 없을 때만 기록한다. */
	private RecordStatus insertIfNotExistsToday(PaymentKicc payment, String payType, String paymentOk, String cardName) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			conn.setAutoCommit(false);
			if (CardPaymentLedger.existsToday(conn, paymentOk)) {
				logger.info("오늘 같은 승인번호 결제가 이미 있어 기록하지 않음 terminalID={} R09={}", payment.getTerminalID(), paymentOk);
				conn.commit();
				return RecordStatus.DUPLICATE;
			}
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), paymentOk, payment.getJobType(), cardName,
					payment.getTotalPages());
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			DbLog.error(logger, "CreaditPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}

	/** 결제 한 건을 바로 기록한다. */
	private RecordStatus insert(PaymentKicc payment, String payType, String paymentOk, String cardName) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), paymentOk, payment.getJobType(), cardName,
					payment.getTotalPages());
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			DbLog.error(logger, "CreaditPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
