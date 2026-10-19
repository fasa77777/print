package com.jetwork.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * MSSQL 커넥션 풀 (commons-dbcp 1.4).
 *
 * 각 SQL 클래스는 ConnectionPool.getInst().getConnection() 으로 커넥션을 빌리고,
 * 다 쓰면 freeConnection(...) 또는 close() 로 반납한다. (close() 하면 실제로 끊지 않고 풀로 돌아간다)
 */
public class DBCPConnectionMgr {

	private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String POOL_NAME = "jetwork";
	private static final String POOL_URL = "jdbc:apache:commons:dbcp:" + POOL_NAME;

	// ---- 풀 설정 ----
	/** 동시에 쓸 수 있는 최대 커넥션 수 (예전 값 20000 은 DB 서버가 감당할 수 없는 수였다) */
	private static final int MAX_ACTIVE = 50;
	/** 쉬고 있는 커넥션을 최대 몇 개까지 남겨 둘지 */
	private static final int MAX_IDLE = 10;
	/** 커넥션이 모두 사용 중일 때 기다리는 최대 시간. 넘으면 오류 (예전에는 무한 대기) */
	private static final long MAX_WAIT_MILLIS = 10000;
	/** 빌려주기 전에 살아 있는지 확인하는 쿼리 (DB 재시작·네트워크 끊김 후 죽은 커넥션 재사용 방지) */
	private static final String VALIDATION_QUERY = "SELECT 1";
	/** 쉬는 커넥션 점검 주기와, 이 시간 이상 쉰 커넥션은 닫음 (클라우드 방화벽이 유휴 연결을 끊기 전에 정리) */
	private static final long EVICTION_RUN_MILLIS = 60 * 1000;
	private static final long MIN_EVICTABLE_IDLE_MILLIS = 3 * 60 * 1000;
	/** URL 에 지정이 없을 때 쓰는 접속·응답 타임아웃 (네트워크가 멈춰도 요청 스레드가 묶이지 않게) */
	private static final String DEFAULT_URL_OPTIONS = "loginTimeout=10;socketTimeout=60000";

	private static DBCPConnectionMgr instance;

	/** 풀 생성에 성공했는지 */
	private boolean ready = false;

	public static synchronized DBCPConnectionMgr getInstance() {
		if (instance == null) {
			instance = new DBCPConnectionMgr();
		}
		return instance;
	}

	private DBCPConnectionMgr() {
		try {
			setupDriver(JDBC_DRIVER, withDefaultOptions(DbConfig.getUrl()), DbConfig.getUsername(),
					DbConfig.getPassword());
			ready = true;
		} catch (Exception ex) {
			System.err.println("[DB] 커넥션 풀 생성 실패: " + ex);
			ex.printStackTrace();
		}
	}

	/**
	 * 풀에서 커넥션을 빌린다.
	 *
	 * @return 커넥션. DB 에 접속할 수 없거나 풀이 가득 차 MAX_WAIT_MILLIS 안에 빌리지 못하면 null
	 */
	public Connection getConnection() {
		if (!ready) {
			System.err.println("[DB] 커넥션 풀이 준비되지 않았습니다. DB 설정(db.properties)을 확인하세요.");
			return null;
		}
		try {
			return DriverManager.getConnection(POOL_URL);
		} catch (SQLException ex) {
			System.err.println("[DB] 커넥션을 가져오지 못했습니다: " + ex.getMessage());
			return null;
		}
	}

	private void setupDriver(String jdbcDriver, String jdbcURL, String user, String password) throws Exception {
		if (jdbcURL == null || jdbcURL.isEmpty()) {
			throw new IllegalStateException("DB URL 이 없습니다 (db.properties 의 db.url 또는 환경변수 DB_URL)");
		}

		// JDBC 드라이버 로딩
		Class.forName(jdbcDriver);

		GenericObjectPool connectionPool = new GenericObjectPool(null);
		connectionPool.setMaxActive(MAX_ACTIVE);
		connectionPool.setMaxIdle(MAX_IDLE);
		connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
		connectionPool.setMaxWait(MAX_WAIT_MILLIS);
		connectionPool.setTestOnBorrow(true);
		connectionPool.setTestWhileIdle(true);
		connectionPool.setTimeBetweenEvictionRunsMillis(EVICTION_RUN_MILLIS);
		connectionPool.setMinEvictableIdleTimeMillis(MIN_EVICTABLE_IDLE_MILLIS);

		// 실제 DB 커넥션을 만드는 팩토리
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcURL, user, password);

		// 풀에 넣을 커넥션을 만들고 검증하는 팩토리 (생성자에서 풀에 자기 자신을 등록한다)
		new PoolableConnectionFactory(connectionFactory, connectionPool, null, VALIDATION_QUERY,
				false, // read only
				true); // 반납될 때 auto commit 을 기본값(true)으로 되돌림

		PoolingDriver driver = new PoolingDriver();
		driver.registerPool(POOL_NAME, connectionPool);
		System.out.println("[DB] 커넥션 풀 등록 완료 (최대 " + MAX_ACTIVE + "개)");
	}

	/** URL 에 loginTimeout / socketTimeout 이 없으면 기본값을 붙인다. */
	static String withDefaultOptions(String url) {
		if (url == null || url.isEmpty()) {
			return url;
		}
		String lower = url.toLowerCase();
		StringBuilder sb = new StringBuilder(url);
		for (String option : DEFAULT_URL_OPTIONS.split(";")) {
			String key = option.substring(0, option.indexOf('=')).toLowerCase();
			if (!lower.contains(key + "=")) {
				if (sb.charAt(sb.length() - 1) != ';') {
					sb.append(';');
				}
				sb.append(option);
			}
		}
		return sb.toString();
	}

    public void freeConnection(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void freeConnection(Connection con, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Connection con, PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }   
    public void freeConnection(Connection con, Statement stmt) {
        try {
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Connection con) {
        try {
            if (con != null) con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void freeConnection(ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }   
   
}
