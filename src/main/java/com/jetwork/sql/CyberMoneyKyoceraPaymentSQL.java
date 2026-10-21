package com.jetwork.sql;

import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.PaymentKyocera;

/**
 * 교세라 사이버머니 결제 (/jetwork/kyocera/postCyberMoneyPayment).
 * CyberMoneyPaymentSQL 과 같은 처리이고, 컬러 값을 1(컬러)/2(흑백)에서 true/false 로 바꿔 기록하는 점만 다르다.
 */
public class CyberMoneyKyoceraPaymentSQL {

	public CyberMoneyResult setPayment(PaymentKyocera payment) {
		return CyberMoneyPaymentSQL.pay("CyberMoneyKyoceraPaymentSQL", payment.getTerminalID(), payment.getUserCard(),
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
}
