package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CyberMoneyResult;
import com.jetwork.model.CyberMoneyResult.Status;
import com.jetwork.model.PaymentKicc;

import co.kr.up.util.Mask;

/** 사이버머니 결제 (/stech/postCyberMoneyPayment). */
public class CyberMoneyPaymentSQL {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyPaymentSQL.class);

	/**
	 * 사이버머니로 결제한다.
	 * 1. 금액이 숫자가 아니면 ERROR, 카드번호가 비었으면 NO_CARD
	 * 2. 같은 결제번호(cyberNo)가 이미 있어도 연속 결제로 보고 처리한다 (로그만 남김)
	 * 3. 처음 보는 카드면 0원으로 등록하고 NO_CARD
	 * 4. 잔액이 금액보다 적으면 INSUFFICIENT (차감하지 않음, balance = 잔액 - 금액)
	 * 5. 차감하고 jw_payment 에 기록, 단말기 last_dt 갱신 후 OK (처리 후 잔액 포함). 잔액과 금액이 같으면 0원으로 결제된다
	 */
	public CyberMoneyResult setPayment(PaymentKicc payment) {
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

	private CyberMoneyResult pay(PaymentKicc payment, String card, int amount) {
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
				// 처음 보는 카드는 0원으로 등록하고 '등록되지 않은 카드'로 응답한다.
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
			DbLog.error(logger, "CyberMoneyPaymentSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return new CyberMoneyResult(Status.ERROR, 0);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
