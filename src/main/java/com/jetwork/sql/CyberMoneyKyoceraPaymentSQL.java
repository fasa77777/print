package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKyocera;

import co.kr.up.util.Mask;

/**
 * 교세라 사이버머니 결제 (/stech/kyocera/postCyberMoneyPayment).
 */
public class CyberMoneyKyoceraPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyKyoceraPaymentSQL.class);

	/**
	 * 교세라 사이버머니로 결제한다. CyberMoneyPaymentSQL 과 같고,
	 * 컬러 값을 1(컬러)/2(흑백)에서 true/false 로 바꿔 기록하는 점만 다르다.
	 */
	public CyberMoneyResult setPayment(PaymentKyocera payment) {
		String card = payment.getUserCard();
		Integer amount = CyberMoneyLedger.parseMoney(payment.getTotalAmount());
		if (amount == null || amount < 0) {
			logger.info("사이버머니 결제 금액 오류: [" + payment.getTotalAmount() + "]");
			return new CyberMoneyResult(Status.ERROR, 0);
		}
		// 카드번호가 비어 있으면 빈 카드번호로 cybermoney 에 새로 등록되던 것을 막는다.
		if (card == null || card.trim().isEmpty()) {
			logger.warn("카드번호 없는 사이버머니 결제 terminalID={}", payment.getTerminalID());
			return new CyberMoneyResult(Status.NO_CARD, 0);
		}

		// 같은 카드의 결제는 이 서버 안에서 차례로 처리한다 (연속 결제가 동시에 와도 잔액을 하나씩 확인·차감·기록).
		synchronized (CyberMoneyLedger.cardLock(card)) {
			return pay(payment, card, amount);
		}
	}

	private CyberMoneyResult pay(PaymentKyocera payment, String card, int amount) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return new CyberMoneyResult(Status.ERROR, 0);
		}
		try {
			conn.setAutoCommit(false);

			Integer balance = CyberMoneyLedger.findBalance(conn, card);

			// 같은 카드·결제번호(cyberNo)가 이미 있어도 거절하지 않는다 (연속 결제).
			// 매장 에이전트는 결제번호로 초 단위 시각(yyyyMMddHHmmss)을 보내서, 같은 카드로 1초 안에 연속 결제하면 번호가 같다.
			// 예전(실서버 jar 포함)에는 두 번째 결제를 거절해서 정상 결제가 실패했다. 기록만 남겨 둔다.
			if (CyberMoneyLedger.isDuplicate(conn, card, payment.getCyberNo())) {
				logger.info("같은 카드·결제번호 연속 결제 card={} cyberNo={} amount={}", Mask.card(card), payment.getCyberNo(), amount);
			}

			if (balance == null) {
				CyberMoneyLedger.registerCard(conn, card, payment.getTerminalID());
				conn.commit();
				return new CyberMoneyResult(Status.NO_CARD, 0);
			}

			if (!CyberMoneyLedger.deduct(conn, card, amount)) {
				conn.commit();
				logger.info("사이버머니 잔액 부족 card=" + Mask.card(card) + " balance=" + balance + " amount=" + amount);
				// 응답 balance 는 실서버 jar 와 같이 "잔액 - 결제 금액" (음수)
				return new CyberMoneyResult(Status.INSUFFICIENT, balance - amount);
			}

			// 교세라 에이전트는 컬러를 1(컬러) / 2(흑백)로 보낸다.
			if ("1".equals(payment.getColor())) {
				payment.setColor("true");
			} else if ("2".equals(payment.getColor())) {
				payment.setColor("false");
			}

			String franName = CyberMoneyLedger.findFranName(conn, payment.getTerminalID());
			CyberMoneyLedger.insertPayment(conn, payment.getTerminalID(), String.valueOf(amount),
					payment.getPaperSize(), payment.getColor(), payment.getCyberNo(), payment.getJobType(), franName,
					card, payment.getCyberNo(), payment.getTotalPages());

			Integer after = CyberMoneyLedger.findBalance(conn, card);
			conn.commit();
			CardPaymentLedger.markLastPayment(conn, payment.getTerminalID());
			logger.info("사이버머니 결제 완료 card=" + Mask.card(card) + " amount=" + amount + " balance=" + after);
			return new CyberMoneyResult(Status.OK, after == null ? 0 : after);
		} catch (Exception e) {
			DbLog.error(logger, "CyberMoneyKyoceraPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return new CyberMoneyResult(Status.ERROR, 0);
		} finally {
			dbcp.freeConnection(conn);
		}
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
