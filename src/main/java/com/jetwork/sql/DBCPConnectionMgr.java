package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(DBCPConnectionMgr.class);

	private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String POOL_NAME = "jetwork";
	private static final String POOL_URL = "jdbc:apache:commons:dbcp:" + POOL_NAME;

	// ---- 풀 설정 ----
	// 최대 커넥션 수와 유휴 커넥션 수는 DbConfig (db.properties 의 db.pool.maxActive / db.pool.maxIdle, 기본 100 / 20)
	/** 커넥션이 모두 사용 중일 때 기다리는 최대 시간. 넘으면 오류 (예전에는 무한 대기) */
	private static final long MAX_WAIT_MILLIS = 10000;
	/** 빌려주기 전에 살아 있는지 확인하는 쿼리 (DB 재시작·네트워크 끊김 후 죽은 커넥션 재사용 방지) */
	private static final String VALIDATION_QUERY = "SELECT 1";
	/** 쉬는 커넥션 점검 주기와, 이 시간 이상 쉰 커넥션은 닫음 (클라우드 방화벽이 유휴 연결을 끊기 전에 정리) */
	private static final long EVICTION_RUN_MILLIS = 60 * 1000;
	private static final long MIN_EVICTABLE_IDLE_MILLIS = 3 * 60 * 1000;
	/**
	 * URL 에 지정이 없을 때 붙이는 접속 옵션.
	 * - loginTimeout=10: DB 접속을 10초 안에 못 하면 실패
	 * - socketTimeout=15000: 쿼리 응답이 15초 안에 안 오면 실패 (요청 스레드가 무한히 묶이지 않게)
	 * - lockTimeout=5000: 다른 연결이 잡고 있는 잠금을 5초까지만 기다리고 실패 (기본값은 무한 대기)
	 * - sendStringParametersAsUnicode=false: 문자열 파라미터를 VARCHAR 로 보낸다.
	 *   NVARCHAR 로 보내면 VARCHAR 컬럼(terminal_id 등)의 인덱스를 못 타고 테이블 전체를 읽어서,
	 *   다른 연결이 잠근 행을 만나면 잠금이 풀릴 때까지 기다리게 된다.
	 *   한글 값은 setNString 으로 보내므로 영향이 없다.
	 */
	private static final String DEFAULT_URL_OPTIONS = "loginTimeout=10;socketTimeout=15000;lockTimeout=5000;sendStringParametersAsUnicode=false";

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
			logger.error("[DB] 커넥션 풀 생성 실패", ex);
		}
	}

	/**
	 * 풀에서 커넥션을 빌린다.
	 *
	 * @return 커넥션. DB 에 접속할 수 없거나 풀이 가득 차 MAX_WAIT_MILLIS 안에 빌리지 못하면 null
	 */
	public Connection getConnection() {
		if (!ready) {
			logger.warn("[DB] 커넥션 풀이 준비되지 않았습니다. DB 설정(db.properties)을 확인하세요.");
			return null;
		}
		try {
			return DriverManager.getConnection(POOL_URL);
		} catch (SQLException ex) {
			logger.warn("[DB] 커넥션을 가져오지 못했습니다: " + ex.getMessage());
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
		connectionPool.setMaxActive(DbConfig.getPoolMaxActive());
		connectionPool.setMaxIdle(DbConfig.getPoolMaxIdle());
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
		logger.info("[DB] 커넥션 풀 등록 완료 (최대 " + DbConfig.getPoolMaxActive() + "개, 유휴 " + DbConfig.getPoolMaxIdle() + "개)");
	}

	/** URL 에 DEFAULT_URL_OPTIONS 의 옵션이 없으면 기본값을 붙인다. (URL 에 이미 있으면 그 값을 쓴다) */
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
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
    public void freeConnection(Connection con, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
   
    public void freeConnection(Connection con, PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }   
    public void freeConnection(Connection con, Statement stmt) {
        try {
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
   
    public void freeConnection(Connection con) {
        try {
            if (con != null) con.close();
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
   
    public void freeConnection(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
   
    public void freeConnection(PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }
    public void freeConnection(ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            logger.error("DBCPConnectionMgr 처리 중 오류", e);
        }
    }   
   
}
