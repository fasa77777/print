package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.RecordStatus;
import com.jetwork.model.PrintNuriPay;

/** 프린트누리 결제 기록 (/jetwork/printnuri/payment). */
public class PrintNuriCreaditPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(PrintNuriCreaditPaymentSQL.class);

	/**
	 * 결제 한 건을 jw_payment 에 기록하고 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 갱신한다.
	 * payType 이 Y 면 체크카드, 아니면 신용카드. payTypeTM 이 TM/EB 면 티머니/캐시비.
	 */
	public RecordStatus setPayment(PrintNuriPay payment) {
		String payType = CardPaymentLedger.payType(payment.getPayType(), payment.getPayTypeTM());
		String cardName = CardPaymentLedger.cardName(payType, payment.getCraditName()); // 카드사명 (모르면 신용카드는 해외카드, 체크카드는 체크카드), 티머니·캐시비는 그 이름

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
