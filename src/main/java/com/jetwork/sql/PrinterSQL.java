package com.jetwork.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Printer;

/**
 * 프린터 상태 등록/갱신 (예전 /printnuri/postPrinter. /stech 버전에는 이 API 가 없어 지금은 쓰지 않음).
 *
 * 주의: 존재 확인은 [SONTECH].[dbo].[printer] 에서 하고, 등록·갱신은 현재 DB(GWS)의 [dbo].[printer] 에 한다.
 * 기존 코드 그대로이며, 두 테이블이 같은 것인지 확인이 필요하다. (docs/ARCHITECTURE.md 참고)
 */
public class PrinterSQL {

	private static final Logger logger = LoggerFactory.getLogger(PrinterSQL.class);

	private static final String UPDATE_SQL = "UPDATE [dbo].[printer] SET "
			+ " [devid] = ?, [sitename] = ?, [desc1] = ?, [errmsg] = ?, [modelname] = ?, "
			+ " [total] = ?, [mono] = ?, [color] = ?, [black] = ?, [cyan] = ?, [magenta] = ?, [yellow] = ?, "
			+ " [devicestatus] = ?, [printerstatus] = ?, [coverstatus] = ?, [macaddr] = ?, [regdate] = getdate() "
			+ " WHERE [serialnumber] = ?";

	private static final String INSERT_SQL = "INSERT INTO [dbo].[printer] ([devid], [sitename], [desc1], [errmsg], [modelname], "
			+ " [total], [mono], [color], [black], [cyan], [magenta], [yellow], "
			+ " [devicestatus], [printerstatus], [coverstatus], [macaddr], [serialnumber], [regdate]) "
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, getdate())";

	/** @return 이미 등록된 프린터였으면 true (갱신), 새로 등록했으면 false */
	public boolean setPrinter(Printer printer) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return false;
		}
		boolean registered = false;
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT 1 FROM [SONTECH].[dbo].[printer] WHERE [serialnumber] = ?")) {
				ps.setString(1, printer.getSerialnumber());
				try (ResultSet rs = ps.executeQuery()) {
					registered = rs.next();
				}
			}

			// UPDATE 와 INSERT 모두 앞 16개 값의 순서가 같고, 17번째가 시리얼 번호다.
			try (PreparedStatement ps = conn.prepareStatement(registered ? UPDATE_SQL : INSERT_SQL)) {
				ps.setString(1, printer.getDevid());
				ps.setNString(2, printer.getSitename());
				ps.setNString(3, printer.getDesc());
				ps.setNString(4, printer.getErrmsg());
				ps.setNString(5, printer.getModelname());
				ps.setInt(6, printer.getTotal());
				ps.setInt(7, printer.getMono());
				ps.setInt(8, printer.getColor());
				ps.setInt(9, printer.getBlack());
				ps.setInt(10, printer.getCyan());
				ps.setInt(11, printer.getMagenta());
				ps.setInt(12, printer.getYellow());
				ps.setNString(13, printer.getDevicestatus());
				ps.setNString(14, printer.getPrinterstatus());
				ps.setNString(15, printer.getCoverstatus());
				ps.setString(16, printer.getMacaddr());
				ps.setString(17, printer.getSerialnumber());
				ps.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			DbLog.error(logger, "PrinterSQL", e);
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
		logger.debug("프린터 {} registered={}", printer.getSerialnumber(), registered);
		return registered;
	}
}
