package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.A4Terminal;
import com.jetwork.model.PriceResult;

/** 단말기별 용지 가격 조회와 에이전트 버전 갱신 (/stech/printnuri/pricepolicy). */
public class PaperPriceSQL {

	private static final Logger logger = LoggerFactory.getLogger(PaperPriceSQL.class);

	/** 단말기의 용지 가격 목록을 돌려주고, 단말기 에이전트 버전을 갱신한다. 오류면 빈 목록. */
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

			// 버전은 값이 왔을 때만 바꾼다 (매장 에이전트는 version 을 보내지 않으므로 terminalVersion 을 지우지 않게)
			TerminalInfoSQL.updateVersions(conn, terminal.getTerminalID(), terminal.getAgentVersion(), terminal.getVersion());
			conn.commit();
		} catch (Exception e) {
			DbLog.error(logger, "PaperPriceSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
		return prices;
	}
}
