package com.jetwork.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 카드·티머니·캐시비 결제 내역(jw_payment) SQL 모음. 호출하는 쪽이 트랜잭션을 관리한다. */
final class CardPaymentLedger {

	private CardPaymentLedger() {
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
	 * 단말기의 마지막 결제 시각(jw_terminal.last_dt)을 지금으로 갱신하고 커밋한다.
	 * 결제 기록을 커밋한 "뒤에" 호출한다. 여기서 실패해도 이미 커밋된 결제 기록에는 영향이 없고 로그만 남긴다.
	 */
	static void markLastPayment(Connection conn, String terminalID) {
		try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE [dbo].[jw_terminal] SET [last_dt] = getdate() WHERE [terminal_id] = ?")) {
			ps.setString(1, terminalID);
			int updated = ps.executeUpdate();
			conn.commit();
			if (updated == 0) {
				System.out.println("last_dt 갱신 대상 단말기 없음 terminalID=" + terminalID);
			}
		} catch (Exception e) {
			System.err.println("[DB] last_dt 갱신 실패 terminalID=" + terminalID + " : " + e.getMessage());
			CyberMoneyLedger.rollbackQuietly(conn);
		}
	}

	static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
