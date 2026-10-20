package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DB 접속 정보를 한 곳에서 읽어 오는 클래스.
 *
 * 읽는 순서 (뒤에 있는 값이 앞의 값을 덮어씀)
 *   1. classpath 의 db.properties  (src/main/resources/db.properties, Git 에는 올리지 않음)
 *   2. 실행한 폴더의 db.properties, 또는 -Ddb.config=경로 로 지정한 파일 (운영 서버에서 권장: jar 에 비밀번호가 들어가지 않음)
 *   3. 환경변수 DB_URL, DB_USERNAME, DB_PASSWORD
 *
 * 커넥션 풀 크기도 db.properties 의 db.pool.maxActive / db.pool.minIdle 로 바꿀 수 있다.
 *
 * db.properties 는 src/main/resources/db.properties.example 을 복사해서 만든다.
 */
public final class DbConfig {

	private static final Logger logger = LoggerFactory.getLogger(DbConfig.class);

	private static final String RESOURCE = "/db.properties";
	private static final String DEFAULT_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

	private static final String url;
	/** JDBC 드라이버 클래스 (db.driver). 보통은 쓰지 않는다 (기본 MSSQL). 테스트 프로그램이 가짜 DB 드라이버를 쓸 때 지정한다 */
	private static final String driver;
	private static final String username;
	private static final String password;
	private static final int poolMaxActive;
	private static final int poolMinIdle;

	/** 최대 커넥션 수 기본값. 실서버 jar 와 같은 200개 */
	private static final int DEFAULT_POOL_MAX_ACTIVE = 200;
	/** 요청이 없어도 열어 두는 커넥션 수 기본값. 요청이 몰리면 최대 수까지 늘어나고, 한가해지면 이 수까지 줄어든다 */
	private static final int DEFAULT_POOL_MIN_IDLE = 10;

	static {
		Properties props = new Properties();
		// 1) jar 안의 db.properties (src/main/resources/db.properties 를 넣고 빌드한 경우)
		try (InputStream is = DbConfig.class.getResourceAsStream(RESOURCE)) {
			if (is != null) {
				props.load(is);
				logger.info("[DbConfig] jar 안의 db.properties 를 읽었습니다");
			}
		} catch (IOException e) {
			logger.error("DbConfig 처리 중 오류", e);
		}
		// 2) jar 밖의 db.properties (실행한 폴더, 또는 -Ddb.config=경로). 있으면 1) 의 값을 덮어쓴다.
		//    운영 서버에서는 jar 에 비밀번호를 넣지 않고 jar 옆에 이 파일을 두는 것을 권장한다.
		File external = new File(System.getProperty("db.config", "db.properties"));
		if (external.isFile()) {
			try (InputStream is = new FileInputStream(external)) {
				props.load(is);
				logger.info("[DbConfig] {} 를 읽었습니다", external.getAbsolutePath());
			} catch (IOException e) {
				logger.error("[DbConfig] " + external.getAbsolutePath() + " 를 읽지 못했습니다", e);
			}
		}

		url = pick("DB_URL", props.getProperty("db.url"));
		driver = props.getProperty("db.driver", DEFAULT_DRIVER).trim();
		username = pick("DB_USERNAME", props.getProperty("db.username"));
		password = pick("DB_PASSWORD", props.getProperty("db.password"));
		poolMaxActive = intValue(props.getProperty("db.pool.maxActive"), DEFAULT_POOL_MAX_ACTIVE);
		poolMinIdle = intValue(props.getProperty("db.pool.minIdle"), DEFAULT_POOL_MIN_IDLE);

		if (url == null || url.isEmpty()) {
			logger.warn("[DbConfig] DB 접속 정보가 없습니다. "
					+ "src/main/resources/db.properties.example 을 db.properties 로 복사해서 값을 채우거나, "
					+ "환경변수 DB_URL / DB_USERNAME / DB_PASSWORD 를 설정하세요.");
		}
	}

	private DbConfig() {
	}

	private static String pick(String envName, String fileValue) {
		String env = System.getenv(envName);
		if (env != null && !env.isEmpty()) {
			return env;
		}
		return fileValue == null ? null : fileValue.trim();
	}

	private static int intValue(String value, int defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(value.trim());
			return v > 0 ? v : defaultValue;
		} catch (NumberFormatException e) {
			logger.warn("[DbConfig] 숫자가 아닌 풀 설정값 무시: " + value);
			return defaultValue;
		}
	}

	public static int getPoolMaxActive() {
		return poolMaxActive;
	}

	public static int getPoolMinIdle() {
		return poolMinIdle;
	}

	public static String getDriver() {
		return driver;
	}

	public static String getUrl() {
		return url;
	}

	public static String getUsername() {
		return username;
	}

	public static String getPassword() {
		return password;
	}
}
