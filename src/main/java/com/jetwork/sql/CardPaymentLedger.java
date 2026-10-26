package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.jetwork.model.CardCompanyName;

/** 카드·티머니·캐시비 결제 내역(jw_payment) SQL 모음. 호출하는 쪽이 트랜잭션을 관리한다. */
final class CardPaymentLedger {

	private static final Logger logger = LoggerFactory.getLogger(CardPaymentLedger.class);

	private CardPaymentLedger() {
	}

	/**
	 * 결제 종류. checkFlag 가 Y 면 체크카드, 아니면 신용카드. transit 이 TM/EB 면 티머니/캐시비.
	 * (KICC 는 R10·R33, 프린트누리는 payType·payTypeTM 을 넘긴다)
	 */
	static String payType(String checkFlag, String transit) {
		if ("TM".equals(transit)) {
			return "티머니";
		}
		if ("EB".equals(transit)) {
			return "캐시비";
		}
		return "Y".equals(checkFlag) ? "체크카드" : "신용카드";
	}

	/**
	 * jw_payment.payment_cardName 에 넣을 이름.
	 * - 신용카드: 카드사명. 모르면 '해외카드'
	 * - 체크카드: 카드사명. 모르면 '체크카드' (체크카드는 국내 카드라 해외카드로 보지 않는다)
	 * - 티머니·캐시비 등: 카드사명 자리에 그 이름 ("티머니"·"캐시비")
	 */
	static String cardName(String payType, String companyCode) {
		if ("신용카드".equals(payType)) {
			return CardCompanyName.getCardName(companyCode);
		}
		if ("체크카드".equals(payType)) {
			String name = CardCompanyName.getName(companyCode == null ? null : companyCode.trim());
			return name.isEmpty() ? "체크카드" : name;
		}
		return payType;
	}

	/**
	 * 결제 내역 한 건을 남긴다.
	 * totalCount 가 null 이면 payment_totalCount 컬럼은 넣지 않는다 (KICC 단말기 결제).
	 */
	static void insert(Connection conn, String terminalID, String pay, String payType, String paperType,
			String colorYN, String paymentOk, String printType, String cardName, String totalCount)
			throws SQLException {
		String sql = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
				+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName]"
				+ (totalCount != null ? ",[payment_totalCount]) VALUES (?, ?, ?, ?, ?, ?, ?, getdate(), ?, ?)"
						: ") VALUES (?, ?, ?, ?, ?, ?, ?, getdate(), ?)");
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, terminalID);
			ps.setString(2, pay);
			ps.setNString(3, payType);
			ps.setString(4, paperType);
			ps.setString(5, colorYN);
			ps.setString(6, paymentOk);
			ps.setString(7, printType);
			ps.setNString(8, cardName);
			if (totalCount != null) {
				ps.setString(9, totalCount);
			}
			ps.executeUpdate();
		}
	}

	/** 오늘 같은 승인번호(payment_ok)로 남은 결제 내역이 있는지. */
	static boolean existsToday(Connection conn, String paymentOk) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"SELECT 1 FROM [jw_payment] WHERE (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) "
						+ " AND payment_ok = ?")) {
			ps.setString(1, paymentOk);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * 단말기의 마지막 결제 시간(jw_terminal.last_dt)을 지금으로 갱신하고 커밋한다.
	 * 결제 기록을 커밋한 "뒤에" 호출한다. 여기서 실패해도 이미 커밋된 결제 기록에는 영향이 없고 로그만 남긴다.
	 */
	static void markLastPayment(Connection conn, String terminalID) {
		try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE [dbo].[jw_terminal] SET [last_dt] = getdate() WHERE [terminal_id] = ?")) {
			ps.setString(1, terminalID);
			int updated = ps.executeUpdate();
			conn.commit();
			if (updated == 0) {
				logger.warn("last_dt 갱신 대상 단말기 없음 terminalID={}", terminalID);
			}
		} catch (Exception e) {
			logger.warn("last_dt 갱신 실패 terminalID={} : {}", terminalID, e.getMessage());
			CyberMoneyLedger.rollbackQuietly(conn);
		}
	}

	static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
