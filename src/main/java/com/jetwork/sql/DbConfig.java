package com.jetwork.sql;

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
 * db.properties 는 src/main/resources/db.properties.example 을 복사해서 만든다.
 */
public final class DbConfig {

	private static final String RESOURCE = "/db.properties";

	private static final String url;
	private static final String username;
	private static final String password;

	static {
		Properties props = new Properties();
		try (InputStream is = DbConfig.class.getResourceAsStream(RESOURCE)) {
			if (is != null) {
				props.load(is);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		url = pick("DB_URL", props.getProperty("db.url"));
		username = pick("DB_USERNAME", props.getProperty("db.username"));
		password = pick("DB_PASSWORD", props.getProperty("db.password"));

		if (url == null || url.isEmpty()) {
			System.err.println("[DbConfig] DB 접속 정보가 없습니다. "
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
