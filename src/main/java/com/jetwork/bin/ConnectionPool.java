package com.jetwork.bin;

import com.jetwork.sql.DBCPConnectionMgr;

/** DB 커넥션 풀(DBCPConnectionMgr) 접근 창구. */
public class ConnectionPool {

	/**
	 * 커넥션 풀을 돌려준다. 아직 만들어지지 않았으면 이때 만든다.
	 * (서버 시작 직후 main 에서 풀을 만들기 전에 HTTP 요청이 들어와도 null 이 되지 않도록)
	 */
	public static DBCPConnectionMgr getInst() {
		return DBCPConnectionMgr.getInstance();
	}

	/** 서버 시작 시 풀을 미리 만든다. */
	public void connection() {
		DBCPConnectionMgr.getInstance();
	}
}
