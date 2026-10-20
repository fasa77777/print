package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * MSSQL 커넥션 풀 (HikariCP).
 *
 * 각 SQL 클래스는 ConnectionPool.getInst().getConnection() 으로 커넥션을 빌리고,
 * 다 쓰면 freeConnection(...) 또는 close() 로 반납한다. (close() 하면 실제로 끊지 않고 풀로 돌아간다)
 *
 * 예전에는 commons-dbcp 1.4 (2010년, 지원 종료)를 썼다. 바꾼 이유
 *  - 커넥션을 빌리고 돌려줄 때 풀 전체에 잠금 하나를 걸어서, 동시에 요청이 몰리면 서로 기다렸다
 *  - 커넥션을 빌릴 때마다 SELECT 1 을 한 번 더 보냈다 (DB 왕복이 매번 두 배)
 *  - 쉬는 커넥션 점검을 1분에 3개씩만 해서, 네트워크가 끊긴 뒤 죽은 커넥션이 오래 남았다
 *  - 점검 쿼리에 제한 시간이 없었다
 * HikariCP 는 Spring Boot 기본 커넥션 풀이라 이미 프로젝트(spring-boot-starter-jdbc)에 들어 있다.
 */
public class DBCPConnectionMgr {

	private static final Logger logger = LoggerFactory.getLogger(DBCPConnectionMgr.class);

	private static final String POOL_NAME = "jetwork";

	// ---- 풀 설정 ----
	// 최대 커넥션 수와 최소 유휴 커넥션 수는 DbConfig (db.properties 의 db.pool.maxActive / db.pool.minIdle, 기본 200 / 10)
	/** 커넥션이 모두 사용 중일 때 기다리는 최대 시간. 넘으면 null (요청은 오류로 응답) */
	private static final long CONNECTION_TIMEOUT_MILLIS = 10000;
	/** 커넥션이 살아 있는지 확인하는 최대 시간 */
	private static final long VALIDATION_TIMEOUT_MILLIS = 3000;
	/** 이 시간 이상 쉰 커넥션은 닫는다 (최소 유휴 개수는 남김). 방화벽이 유휴 연결을 끊기 전에 정리 */
	private static final long IDLE_TIMEOUT_MILLIS = 3 * 60 * 1000;
	/** 커넥션 최대 수명. 오래된 커넥션은 쉬는 동안 새 커넥션으로 바꾼다 */
	private static final long MAX_LIFETIME_MILLIS = 20 * 60 * 1000;
	/** 커넥션을 이 시간 넘게 반납하지 않으면 경고 로그 (반납 누락 찾기용, 동작에는 영향 없음) */
	private static final long LEAK_DETECTION_MILLIS = 60 * 1000;

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

	/** 커넥션 풀. DB 설정이 없어 만들지 못했으면 null */
	private final HikariDataSource dataSource;

	public static synchronized DBCPConnectionMgr getInstance() {
		if (instance == null) {
			instance = new DBCPConnectionMgr();
		}
		return instance;
	}

	private DBCPConnectionMgr() {
		HikariDataSource ds = null;
		try {
			ds = createPool(withDefaultOptions(DbConfig.getUrl()), DbConfig.getUsername(), DbConfig.getPassword());
		} catch (Exception ex) {
			logger.error("[DB] 커넥션 풀 생성 실패", ex);
		}
		dataSource = ds;
		if (dataSource != null) {
			// 서버를 끌 때 DB 커넥션을 정리한다
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					dataSource.close();
				}
			}, "db-pool-close"));
		}
	}

	/**
	 * 풀에서 커넥션을 빌린다.
	 *
	 * @return 커넥션. DB 에 접속할 수 없거나 풀이 가득 차 CONNECTION_TIMEOUT_MILLIS 안에 빌리지 못하면 null
	 */
	public Connection getConnection() {
		if (dataSource == null) {
			logger.warn("[DB] 커넥션 풀이 준비되지 않았습니다. DB 설정(db.properties)을 확인하세요.");
			return null;
		}
		try {
			return dataSource.getConnection();
		} catch (SQLException ex) {
			logger.warn("[DB] 커넥션을 가져오지 못했습니다: {} ({})", ex.getMessage(), poolStatus());
			return null;
		}
	}

	/** 풀 상태 (사용 중 / 쉬는 중 / 기다리는 요청 수). 로그용 */
	public String poolStatus() {
		if (dataSource == null) {
			return "풀 없음";
		}
		HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
		if (pool == null) {
			return "풀 시작 전";
		}
		return "사용 " + pool.getActiveConnections() + " / 쉬는 중 " + pool.getIdleConnections()
				+ " / 대기 " + pool.getThreadsAwaitingConnection() + " / 최대 " + dataSource.getMaximumPoolSize();
	}

	private HikariDataSource createPool(String jdbcURL, String user, String password) {
		if (jdbcURL == null || jdbcURL.isEmpty()) {
			throw new IllegalStateException("DB URL 이 없습니다 (db.properties 의 db.url 또는 환경변수 DB_URL)");
		}
		int max = DbConfig.getPoolMaxActive();
		int minIdle = Math.min(DbConfig.getPoolMinIdle(), max);

		HikariConfig config = new HikariConfig();
		config.setPoolName(POOL_NAME);
		config.setDriverClassName(DbConfig.getDriver()); // 기본 com.microsoft.sqlserver.jdbc.SQLServerDriver
		config.setJdbcUrl(jdbcURL);
		config.setUsername(user);
		config.setPassword(password);
		config.setMaximumPoolSize(max);
		config.setMinimumIdle(minIdle);
		config.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
		config.setValidationTimeout(VALIDATION_TIMEOUT_MILLIS);
		config.setIdleTimeout(IDLE_TIMEOUT_MILLIS);
		config.setMaxLifetime(MAX_LIFETIME_MILLIS);
		config.setLeakDetectionThreshold(LEAK_DETECTION_MILLIS);
		// 커넥션을 돌려받을 때 auto commit 을 기본값(true)으로 되돌리고, 커밋하지 않은 작업은 rollback 한다 (HikariCP 기본 동작)
		config.setAutoCommit(true);
		// 서버 시작 때 DB 가 꺼져 있어도 서버는 뜨고, DB 가 살아나면 그때부터 접속한다 (예전과 같은 동작)
		config.setInitializationFailTimeout(-1);

		HikariDataSource ds = new HikariDataSource(config);
		logger.info("[DB] 커넥션 풀 생성 (HikariCP, 최대 {}개, 최소 유휴 {}개)", max, minIdle);
		return ds;
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
