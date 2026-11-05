package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Terminal;

/** 단말기 등록 확인과 버전 정보 갱신 (/jetwork/postTerminal). */
public class TerminalInfoSQL {

	private static final Logger logger = LoggerFactory.getLogger(TerminalInfoSQL.class);

	public enum Status {
		/** jw_terminal 에 단말기 ID 와 시리얼이 함께 등록되어 있음 */
		REGISTERED,
		/** 등록되지 않은 단말기 (또는 시리얼 불일치) */
		NOT_REGISTERED,
		/** 등록은 되어 있지만 사용하지 않는 단말기 (USE_YN 이 Y 가 아님) */
		NOT_IN_USE,
		/** 등록은 되어 있지만 사용 기간(expireDt)이 지난 단말기 */
		EXPIRED,
		/** DB 오류로 확인하지 못함 */
		ERROR
	}

	/**
	 * 단말기의 버전 정보와 갱신 시각을 기록한다.
	 * agent_version, terminalVersion 은 값이 왔을 때만 바꾼다 (null 이나 빈 값이면 기존 값 유지). [update] 는 항상 지금으로.
	 * 예전에는 값이 안 와도 덮어써서, 매장 에이전트의 가격 조회 때마다 terminalVersion 이 지워졌다.
	 */
	static void updateVersions(Connection conn, String terminalID, String agentVersion, String terminalVersion) throws java.sql.SQLException {
		boolean agent = agentVersion != null && !agentVersion.trim().isEmpty();
		boolean term = terminalVersion != null && !terminalVersion.trim().isEmpty();
		String sql = "UPDATE [dbo].[jw_terminal] SET " + (agent ? "[agent_version] = ?, " : "") + (term ? "[terminalVersion] = ?, " : "")
				+ "[update] = getdate() WHERE [terminal_id] = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			int i = 1;
			if (agent) ps.setString(i++, agentVersion.trim());
			if (term) ps.setString(i++, terminalVersion.trim());
			ps.setString(i, terminalID);
			ps.executeUpdate();
		}
	}

	/**
	 * 라이센스 날짜 등록: 단말기에서 신규 요청(/jetwork/postTerminal)이 와서
	 * 등록 확인(단말기 ID + 시리얼)에 성공하면 그 시각을 jw_terminal.license_dt 에 기록한다.
	 * serial 은 단말기 시리얼이다.
	 * 기록하면 INFO 로그를 남긴다. 기록에 실패해도(컬럼이 없는 DB 등) 단말기 확인 결과에는 영향을 주지 않고 경고(WARN)만 남긴다.
	 */
	static void markLicenseChecked(Connection conn, String terminalID, String serial) {
		try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE [dbo].[jw_terminal] SET [license_dt] = getdate() WHERE [terminal_id] = ? AND [serial] = ?")) {
			ps.setString(1, terminalID);
			ps.setString(2, serial);
			int rows = ps.executeUpdate();
			if (rows > 0) {
				logger.info("라이센스 날짜 등록 (license_dt = 현재 시각) terminalID={} serial={}", terminalID, serial);
			} else {
				logger.warn("라이센스 날짜 등록 대상 없음 (license_dt 안 바뀜) terminalID={} serial={}", terminalID, serial);
			}
		} catch (java.sql.SQLException e) {
			logger.warn("라이센스 날짜 등록 실패 (license_dt) terminalID={} serial={} : {}", terminalID, serial, e.getMessage());
		}
	}

	/** expireDt 컬럼 이름 */
	static final String EXPIRE_COLUMN = "expireDt";

	/** 오늘 날짜 yyyyMMdd (한국 시간) */
	static String today() {
		java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyyMMdd");
		f.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
		return f.format(new java.util.Date());
	}

	/**
	 * 조회한 행의 expireDt 값. 컬럼이 없거나 읽지 못하면 null (기간 제한 없음으로 본다).
	 * 컬럼 형식이 datetime 이면 "2026-12-31 00:00:00.0", varchar 면 "20261231" 처럼 온다.
	 */
	private static String expireDt(ResultSet rs, String terminalID) {
		try {
			int i = columnIndex(rs, EXPIRE_COLUMN);
			if (i > 0) {
				return rs.getString(i);
			}
			logger.warn("jw_terminal 에 {} 컬럼이 없어 사용 기간을 확인하지 않음 terminalID={}", EXPIRE_COLUMN, terminalID);
		} catch (java.sql.SQLException e) {
			logger.warn("{} 를 읽지 못해 사용 기간을 확인하지 않음 terminalID={} : {}", EXPIRE_COLUMN, terminalID, e.getMessage());
		}
		return null;
	}

	/** 사용 여부 컬럼 이름. 값이 Y 인 단말기만 사용할 수 있다 */
	static final String USE_COLUMN = "USE_YN";

	/**
	 * 단말기를 사용할 수 있는지: USE_YN 이 Y 면 true, 그 밖의 값(N, 빈 값, NULL)은 false.
	 * USE_YN 컬럼이 없는 DB 면 확인하지 않고 true (경고 로그).
	 */
	static boolean isInUse(ResultSet rs, String terminalID) throws java.sql.SQLException {
		int i = columnIndex(rs, USE_COLUMN);
		if (i < 0) {
			logger.warn("jw_terminal 에 {} 컬럼이 없어 사용 여부를 확인하지 않음 terminalID={}", USE_COLUMN, terminalID);
			return true;
		}
		return isUseY(rs.getString(i));
	}

	/** 로그용 USE_YN 값 (컬럼이 없거나 읽지 못하면 "-") */
	private static String useYnText(ResultSet rs) {
		try {
			int i = columnIndex(rs, USE_COLUMN);
			return i < 0 ? "-" : String.valueOf(rs.getString(i));
		} catch (java.sql.SQLException e) {
			return "-";
		}
	}

	/** USE_YN 값이 Y 인지 (앞뒤 공백은 무시) */
	static boolean isUseY(String useYn) {
		return useYn != null && "Y".equals(useYn.trim());
	}

	/** 조회 결과에서 컬럼 위치(1부터). 없으면 -1. 대소문자는 가리지 않는다 */
	private static int columnIndex(ResultSet rs, String column) throws java.sql.SQLException {
		java.sql.ResultSetMetaData meta = rs.getMetaData();
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			if (column.equalsIgnoreCase(meta.getColumnLabel(i)) || column.equalsIgnoreCase(meta.getColumnName(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 오늘(today, yyyyMMdd)이 expireDt 보다 이후 날짜인지. 날짜(일) 단위로 비교한다.
	 * expireDt 가 비었거나 날짜로 읽을 수 없으면 false (기간 제한 없음).
	 */
	static boolean isExpired(String expireDt, String today) {
		if (expireDt == null) {
			return false;
		}
		String digits = expireDt.replaceAll("[^0-9]", "");
		if (digits.length() < 8) {
			if (!expireDt.trim().isEmpty()) {
				logger.warn("{} 값을 날짜로 읽을 수 없어 사용 기간을 확인하지 않음: [{}]", EXPIRE_COLUMN, expireDt);
			}
			return false;
		}
		return today.compareTo(digits.substring(0, 8)) > 0;
	}

	/**
	 * 단말기 ID 와 시리얼이 등록되어 있고 사용 기간(expireDt)이 지나지 않았으면 license_dt 를 지금으로 바꾸고, 버전 정보(agent_version, terminalVersion)를 갱신한다. 버전은 값이 왔을 때만 바꾼다.
	 * USE_YN 이 Y 가 아니면 NOT_IN_USE, 사용 기간이 지났으면 EXPIRED (둘 다 아무것도 바꾸지 않음).
	 */
	public Status setTerminal(Terminal terminal) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return Status.ERROR;
		}
		boolean registered = false;
		try {
			conn.setAutoCommit(false);

			// 등록 확인: 단말기 ID 와 시리얼(단말기 시리얼)이 jw_terminal 에 함께 등록되어 있는지
			String expireDt = null;
			boolean inUse = true;
			String useYn = null;
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = ? AND [serial] = ?")) {
				ps.setString(1, terminal.getTerminalID());
				ps.setString(2, terminal.getSerial());
				try (ResultSet rs = ps.executeQuery()) {
					registered = rs.next();
					if (registered) {
						inUse = isInUse(rs, terminal.getTerminalID());
						useYn = useYnText(rs);
						expireDt = expireDt(rs, terminal.getTerminalID());
					}
				}
			}

			// 사용 여부 확인: USE_YN 이 Y 인 단말기만 사용 가능 (N, 빈 값, NULL 은 실패)
			if (registered && !inUse) {
				logger.warn("사용하지 않는 단말기 (USE_YN={}) terminalID={} serial={}", useYn, terminal.getTerminalID(),
						terminal.getSerial());
				conn.commit();
				return Status.NOT_IN_USE;
			}

			// 사용 기간 확인: 오늘이 expireDt 보다 이후 날짜면 실패 (expireDt 당일까지는 사용 가능)
			if (registered && isExpired(expireDt, today())) {
				logger.warn("단말기 사용 기간 만료 terminalID={} serial={} expireDt={} 오늘={}", terminal.getTerminalID(),
						terminal.getSerial(), expireDt, today());
				conn.commit();
				return Status.EXPIRED;
			}

			if (registered) {
				logger.debug("등록된 단말기 {}", terminal.getTerminalID());
				// 단말기에서 신규 요청 시 라이센스 날짜 등록 (serial = 단말기 시리얼)
				markLicenseChecked(conn, terminal.getTerminalID(), terminal.getSerial());
				updateVersions(conn, terminal.getTerminalID(), terminal.getAgentVersion(), terminal.getTerminalVersion());
			}
			conn.commit();
		} catch (Exception e) {
			DbLog.error(logger, "TerminalInfoSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return Status.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
		return registered ? Status.REGISTERED : Status.NOT_REGISTERED;
	}
}
