package com.jetwork.sql;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.RecordStatus;
import com.jetwork.model.PaymentCreaditKicc;

/** KICC 단말기 결제 기록 (/jetwork/kicc/postCraditPayment). */
public class KiccCreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(KiccCreaditPaymentSQL.class);

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 갱신한다.
	 * R10 이 Y 면 체크카드, 아니면 신용카드. R33 이 TM/EB 면 티머니/캐시비.
	 */
	public RecordStatus setPayment(PaymentCreaditKicc payment) {
		// R33 은 "TM " 처럼 공백이 붙어 와도 구분되게 공백을 뗀다
		String payType = CardPaymentLedger.payType(payment.getR10(), CardPaymentLedger.trim(payment.getR33()));
		String cardName = CardPaymentLedger.cardName(payType, payment.getR12()); // 카드사명 (모르면 신용카드는 해외카드, 체크카드는 빈칸), 티머니·캐시비는 그 이름

		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return RecordStatus.ERROR;
		}
		try {
			conn.setAutoCommit(false);
			CardPaymentLedger.insert(conn, payment.getTerminalID(), payment.getTotalAmount(), payType,
					payment.getPaperSize(), payment.getColor(), payment.getR09(), payment.getJobType(),
					cardName, null);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			return RecordStatus.OK;
		} catch (Exception e) {
			DbLog.error(logger, "KiccCreaditPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return RecordStatus.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
