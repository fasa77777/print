package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Terminal;

/** 단말기 등록 확인과 버전 정보 갱신 (/stech/postTerminal). */
public class TerminalInfoSQL {

	private static final Logger logger = LoggerFactory.getLogger(TerminalInfoSQL.class);

	public enum Status {
		/** jw_terminal 에 단말기 ID 와 시리얼이 함께 등록되어 있음 */
		REGISTERED,
		/** 등록되지 않은 단말기 (또는 시리얼 불일치) */
		NOT_REGISTERED,
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

	/** 단말기 ID 와 시리얼이 등록되어 있으면 버전 정보(agent_version, terminalVersion)를 갱신한다. 버전은 값이 왔을 때만 바꾼다. */
	public Status setTerminal(Terminal terminal) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return Status.ERROR;
		}
		boolean registered = false;
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT 1 FROM [dbo].[jw_terminal] WHERE [terminal_id] = ? AND [serial] = ?")) {
				ps.setString(1, terminal.getTerminalID());
				ps.setString(2, terminal.getSerial());
				try (ResultSet rs = ps.executeQuery()) {
					registered = rs.next();
				}
			}

			if (registered) {
				logger.debug("등록된 단말기 {}", terminal.getTerminalID());
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
