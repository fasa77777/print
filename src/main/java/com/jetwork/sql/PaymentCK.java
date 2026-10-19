package com.jetwork.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.jetwork.bin.ConnectionPool;

/** 같은 날 같은 승인번호로 카드 결제가 두 번 기록되지 않게 확인한다. */
public class PaymentCK {

	/**
	 * 오늘 같은 승인번호의 결제 내역이 없으면 true.
	 * 동시에 들어온 중복 요청이 먼저 기록될 시간을 주려고 1초 기다린다 (기존 동작 유지).
	 */
	public boolean setTerminal(String paymentOK) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return false;
		}
		boolean notExists = true;
		try {
			notExists = !CardPaymentLedger.existsToday(conn, paymentOK);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dbcp.freeConnection(conn);
		}
		// 기다리는 동안 커넥션을 붙잡고 있지 않도록, 반납한 뒤에 기다린다.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return notExists;
	}

	/** 오늘 같은 승인번호의 결제 내역이 있으면 가장 마지막 한 건을 지운다 (기존 동작 유지). */
	public void setTerminal1(String paymentOK) {
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return;
		}
		try {
			conn.setAutoCommit(false);
			String lastId = null;
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT id FROM [jw_payment] WHERE (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) "
							+ " AND payment_ok = ?")) {
				ps.setString(1, paymentOK);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						lastId = rs.getString("id");
					}
				}
			}
			if (lastId != null) {
				System.out.println("중복삭제 ==== " + lastId);
				try (PreparedStatement ps = conn.prepareStatement("DELETE FROM [dbo].[jw_payment] WHERE id = ?")) {
					ps.setString(1, lastId);
					ps.executeUpdate();
				}
			}
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			CyberMoneyLedger.rollbackQuietly(conn);
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
