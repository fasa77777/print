package com.jetwork.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Terminal;

/** 단말기 등록 확인과 버전 정보 갱신 (/stech/postTerminal). */
public class TerminalInfoSQL {

	public enum Status {
		/** jw_terminal 에 단말기 ID 와 시리얼이 함께 등록되어 있음 */
		REGISTERED,
		/** 등록되지 않은 단말기 (또는 시리얼 불일치) */
		NOT_REGISTERED,
		/** DB 오류로 확인하지 못함 */
		ERROR
	}

	/** 단말기 ID 와 시리얼이 등록되어 있으면 버전 정보를 갱신한다. */
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
				System.out.println("사용중인 복사기 " + terminal.getTerminalID());
				try (PreparedStatement ps = conn.prepareStatement(
						"UPDATE [dbo].[jw_terminal] SET [agent_version] = ?, [update] = getdate() "
								+ " WHERE [terminal_id] = ?")) {
					ps.setString(1, terminal.getAgentVersion());
					ps.setString(2, terminal.getTerminalID());
					ps.executeUpdate();
				}
			}
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
			return Status.ERROR;
		} finally {
			dbcp.freeConnection(conn);
		}
		return registered ? Status.REGISTERED : Status.NOT_REGISTERED;
	}
}
