package com.jetwork.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DB 접속 정보를 한 곳에서 읽어 오는 클래스.
 *
 * 읽는 순서 (뒤에 있는 값이 앞의 값을 덮어씀)
 *   1. classpath 의 db.properties  (src/main/resources/db.properties, Git 에는 올리지 않음)
 *   2. 환경변수 DB_URL, DB_USERNAME, DB_PASSWORD  (운영 서버에서 쓰기 좋음)
 *
 * 커넥션 풀 크기도 db.properties 의 db.pool.maxActive / db.pool.maxIdle 로 바꿀 수 있다.
 *
 * db.properties 는 src/main/resources/db.properties.example 을 복사해서 만든다.
 */
public final class DbConfig {

	private static final Logger logger = LoggerFactory.getLogger(DbConfig.class);

	private static final String RESOURCE = "/db.properties";

	private static final String url;
	private static final String username;
	private static final String password;
	private static final int poolMaxActive;
	private static final int poolMaxIdle;

	/** 풀 크기 기본값. Tomcat 요청 스레드(기본 200개)가 동시에 DB 를 쓰는 경우까지 감당하도록 잡았다. */
	private static final int DEFAULT_POOL_MAX_ACTIVE = 100;
	private static final int DEFAULT_POOL_MAX_IDLE = 20;

	static {
		Properties props = new Properties();
		try (InputStream is = DbConfig.class.getResourceAsStream(RESOURCE)) {
			if (is != null) {
				props.load(is);
			}
		} catch (IOException e) {
			logger.error("DbConfig 처리 중 오류", e);
		}

		url = pick("DB_URL", props.getProperty("db.url"));
		username = pick("DB_USERNAME", props.getProperty("db.username"));
		password = pick("DB_PASSWORD", props.getProperty("db.password"));
		poolMaxActive = intValue(props.getProperty("db.pool.maxActive"), DEFAULT_POOL_MAX_ACTIVE);
		poolMaxIdle = intValue(props.getProperty("db.pool.maxIdle"), DEFAULT_POOL_MAX_IDLE);

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

	public static int getPoolMaxIdle() {
		return poolMaxIdle;
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
