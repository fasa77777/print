package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.jetwork.bin.ConnectionPool;

/** 사이버머니 잔액 조회 (/stech/postCyberMoney). 처음 보는 카드는 0원으로 등록한다. */
public class UserCyberMoneySQL {

	private static final Logger logger = LoggerFactory.getLogger(UserCyberMoneySQL.class);

	/**
	 * @param terminalID 가맹점을 찾을 단말기 ID (컨트롤러는 요청의 userID 를 넘긴다)
	 * @param card       카드번호
	 * @return 잔액 문자열. 새로 등록했으면 "0", 카드번호가 없거나 오류면 빈 문자열
	 */
	public String getUser(String terminalID, String card) {
		// 카드번호가 비어 있으면 조회·등록하지 않는다. (빈 카드번호가 cybermoney 에 등록되지 않게)
		if (card == null || card.trim().isEmpty()) {
			logger.warn("카드번호 없는 잔액 조회 terminalID={}", terminalID);
			return "";
		}
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		Connection conn = dbcp.getConnection();
		if (conn == null) {
			return "";
		}
		try {
			conn.setAutoCommit(false);

			String money = null;
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT cyberMoney FROM [dbo].[cybermoney] WHERE [cardNumber] = ?")) {
				ps.setString(1, card);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						money = rs.getString("cyberMoney");
					}
				}
			}

			if (money == null) {
				CyberMoneyLedger.registerCard(conn, card, terminalID);
				money = "0";
			}
			conn.commit();
			return money;
		} catch (Exception e) {
			logger.error("UserCyberMoneySQL 처리 중 오류", e);
			CyberMoneyLedger.rollbackQuietly(conn);
			return "";
		} finally {
			dbcp.freeConnection(conn);
		}
	}
}
