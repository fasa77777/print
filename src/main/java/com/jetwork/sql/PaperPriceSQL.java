package com.jetwork.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.A4Terminal;
import com.jetwork.model.PriceResult;

/** 단말기별 용지 가격 조회와 버전 정보 갱신 (/printnuri/printnuri/pricepolicy). */
public class PaperPriceSQL {

	/** 단말기의 용지 가격 목록을 돌려주고, 단말기 버전 정보를 갱신한다. 오류면 빈 목록. */
	public Vector<PriceResult> getPrice(A4Terminal terminal) {
		Vector<PriceResult> prices = new Vector<PriceResult>();
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return prices;
		}
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM jw_paper WHERE terminalID = ?")) {
				ps.setString(1, terminal.getTerminalID());
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						PriceResult pr = new PriceResult();
						pr.setA4Color(rs.getString("paper_colorYN"));
						pr.setA4PaperPrice(rs.getString("paper_price"));
						pr.setA4PaperSize(rs.getString("paper_size"));
						prices.addElement(pr);
					}
				}
			}

			try (PreparedStatement ps = conn.prepareStatement(
					"UPDATE [dbo].[jw_terminal] SET [agent_version] = ?, terminalVersion = ?, [update] = getdate() "
							+ " WHERE [terminal_id] = ?")) {
				ps.setString(1, terminal.getAgentVersion());
				ps.setString(2, terminal.getVersion());
				ps.setString(3, terminal.getTerminalID());
				ps.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
		return prices;
	}
}
