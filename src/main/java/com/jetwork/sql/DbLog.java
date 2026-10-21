package com.jetwork.sql;

import java.sql.SQLException;

import org.slf4j.Logger;

/**
 * DB 오류 로그.
 *
 * DB 접속 끊김·시간 초과 같은 SQLException 은 한 줄로만 남긴다. (어디서·무슨 오류·SQLState·오류 코드)
 * 예전에는 오류마다 스택 전체(수십 줄)를 남겨서, DB 가 3초만 끊겨도 요청 수만큼 로그가 7천 줄 넘게 쌓였다.
 * SQLException 이 아닌 오류(코드 버그 등)는 원인을 찾을 수 있게 스택까지 남긴다.
 */
public final class DbLog {

	private DbLog() {
	}

	public static void error(Logger logger, String where, Exception e) {
		if (e instanceof SQLException) {
			SQLException s = (SQLException) e;
			logger.error("{} DB 오류: {} (SQLState={}, 코드={})", where, s.getMessage(), s.getSQLState(), s.getErrorCode());
		} else {
			logger.error(where + " 처리 중 오류", e);
		}
	}
}
