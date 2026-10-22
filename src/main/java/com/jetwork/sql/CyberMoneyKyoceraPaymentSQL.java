package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.PaymentKyocera;

/**
 * 교세라 사이버머니 결제 (/jetwork/kyocera/postCyberMoneyPayment).
 */
public class CyberMoneyKyoceraPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyKyoceraPaymentSQL.class);

	/**
	 * 교세라 사이버머니로 결제한다. 처리는 CyberMoneyPaymentSQL.pay 와 같고,
	 * 컬러 값을 1(컬러)/2(흑백)에서 true/false 로 바꿔 기록하는 점만 다르다.
	 */
	public CyberMoneyResult setPayment(PaymentKyocera payment) {
		return CyberMoneyPaymentSQL.pay(logger, "CyberMoneyKyoceraPaymentSQL", payment.getTerminalID(), payment.getUserCard(),
				payment.getTotalAmount(), payment.getCyberNo(), payment.getPaperSize(), color(payment.getColor()),
				payment.getJobType(), payment.getTotalPages());
	}

	/** 교세라 에이전트는 컬러를 1(컬러) / 2(흑백)로 보낸다. 그 밖의 값은 그대로 */
	static String color(String color) {
		if ("1".equals(color)) {
			return "true";
		}
		if ("2".equals(color)) {
			return "false";
		}
		return color;
	}

	/** 카드의 현재 잔액. 등록되지 않았거나 오류면 빈 문자열. */
	public String reMoney(String card) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return "";
		}
		try {
			Integer balance = CyberMoneyLedger.findBalance(conn, card);
			return balance == null ? "" : String.valueOf(balance);
		} catch (Exception e) {
			DbLog.error(logger, "CyberMoneyKyoceraPaymentSQL", e);
			return "";
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
