package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 사이버머니 관련 SQL 모음. 모든 쿼리는 PreparedStatement 로 값을 넘긴다.
 * 호출하는 쪽이 커넥션과 트랜잭션(commit / rollback)을 관리한다.
 */
final class CyberMoneyLedger {

	private static final Logger logger = LoggerFactory.getLogger(CyberMoneyLedger.class);

	private CyberMoneyLedger() {
	}

	/** 카드의 현재 잔액. 등록되지 않은 카드면 null. */
	static Integer findBalance(Connection conn, String card) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"SELECT cyberMoney FROM [dbo].[cybermoney] WHERE [cardNumber] = ?")) {
			ps.setString(1, card);
			try (ResultSet rs = ps.executeQuery()) {
				Integer balance = null;
				while (rs.next()) {
					String money = rs.getString("cyberMoney");
					balance = parseMoney(money);
					if (balance == null) {
						throw new SQLException("cybermoney 잔액이 숫자가 아닙니다: [" + money + "]");
					}
				}
				return balance;
			}
		}
	}

	/** 카드를 잔액 0원으로 새로 등록한다. 가맹점은 단말기의 가맹점으로 넣는다. */
	static void registerCard(Connection conn, String card, String terminalID) throws SQLException {
		String franID = findTerminalFranId(conn, terminalID);
		try (PreparedStatement ps = conn.prepareStatement(
				"INSERT INTO [dbo].[cybermoney] ([cardNumber],[cyberMoney],[fran_id],[reg_date]) VALUES (?, 0, ?, getdate())")) {
			ps.setString(1, card);
			ps.setString(2, franID);
			ps.executeUpdate();
		}
	}

	/**
	 * 잔액이 충분할 때만 금액을 차감한다.
	 * 확인과 차감을 UPDATE 한 번으로 처리해서, 같은 카드로 동시에 결제해도 잔액이 음수가 되거나 한 번만 차감되지 않는다.
	 * 잔액과 금액이 같으면 0원이 되도록 차감한다.
	 *
	 * @return 차감했으면 true, 잔액이 부족하면 false
	 */
	static boolean deduct(Connection conn, String card, int amount) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE [dbo].[cybermoney] SET [cyberMoney] = CAST([cyberMoney] AS INT) - ? "
						+ " WHERE [cardNumber] = ? AND CAST([cyberMoney] AS INT) >= ?")) {
			ps.setInt(1, amount);
			ps.setString(2, card);
			ps.setInt(3, amount);
			return ps.executeUpdate() > 0;
		}
	}

	/** 같은 카드·결제번호로 이미 결제 내역이 있는지. */
	static boolean isDuplicate(Connection conn, String card, String cyberNo) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"SELECT 1 FROM [dbo].[jw_payment] WHERE [userCard] = ? AND [cyberNo] = ?")) {
			ps.setString(1, card);
			ps.setString(2, cyberNo);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/** 단말기가 속한 가맹점 이름. 없으면 빈 문자열. */
	static String findFranName(Connection conn, String terminalID) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"SELECT b.fran_name FROM [dbo].[jw_terminal] a, [dbo].[jw_franchise] b "
						+ " WHERE a.fran_id = b.fran_id AND a.terminal_id = ?")) {
			ps.setString(1, terminalID);
			try (ResultSet rs = ps.executeQuery()) {
				String franName = "";
				while (rs.next()) {
					franName = rs.getString(1);
				}
				return franName;
			}
		}
	}

	/**
	 * 단말기의 가맹점 ID. 없으면 빈 문자열.
	 * 예전에는 jw_terminal 의 3번째 컬럼으로 읽어서 컬럼 순서가 바뀌면 엉뚱한 값이 들어갔다.
	 * 이제 fran_id 라는 이름의 컬럼을 찾아 읽고, 그런 컬럼이 없을 때만 3번째 컬럼을 쓴다.
	 */
	static String findTerminalFranId(Connection conn, String terminalID) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = ?")) {
			ps.setString(1, terminalID);
			try (ResultSet rs = ps.executeQuery()) {
				int column = franIdColumn(rs.getMetaData());
				String franID = "";
				while (rs.next()) {
					franID = rs.getString(column);
				}
				return franID == null ? "" : franID;
			}
		}
	}

	private static int franIdColumn(java.sql.ResultSetMetaData meta) throws SQLException {
		if (meta != null) {
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				if ("fran_id".equalsIgnoreCase(meta.getColumnName(i))) {
					return i;
				}
			}
		}
		return 3;
	}

	/** 사이버머니 결제 내역을 jw_payment 에 남긴다. */
	static void insertPayment(Connection conn, String terminalID, String amount, String paperSize, String color,
			String paymentOk, String jobType, String franName, String card, String cyberNo, String totalPages)
			throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
				"INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
						+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],"
						+ "[payment_cardName],[userCard],[cyberNo],[payment_totalCount]) "
						+ " VALUES (?, ?, N'사이버머니', ?, ?, ?, ?, getdate(), ?, ?, ?, ?)")) {
			ps.setString(1, terminalID);
			ps.setString(2, amount);
			ps.setString(3, paperSize);
			ps.setString(4, color);
			ps.setString(5, paymentOk);
			ps.setString(6, jobType);
			ps.setNString(7, franName);
			ps.setString(8, card);
			ps.setString(9, cyberNo);
			ps.setString(10, totalPages);
			ps.executeUpdate();
		}
	}

	/** 금액 문자열을 숫자로. 따옴표·공백은 무시한다. 숫자가 아니면 null. */
	static Integer parseMoney(String value) {
		if (value == null) {
			return null;
		}
		String v = value.replace("\"", "").trim();
		if (v.isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(v);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static void rollbackQuietly(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			conn.rollback();
		} catch (SQLException e) {
			logger.error("CyberMoneyLedger 처리 중 오류", e);
		}
	}
}
