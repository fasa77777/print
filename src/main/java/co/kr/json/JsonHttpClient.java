package co.kr.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 결제 중계 서버(pay1.printnuri.com)로 JSON 을 POST 하는 공통 HTTP 클라이언트.
 * 예전에는 PServer, PrintCyberPOST, PrintPolicyPost 에 같은 코드가 복사되어 있었다.
 */
public final class JsonHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(JsonHttpClient.class);

	/**
	 * 중계 서버 주소. 경로는 각 호출하는 쪽에서 붙인다.
	 * 테스트할 때는 실행 옵션 -Drelay.baseUrl=http://127.0.0.1:9000/jetwork 처럼 바꿀 수 있다.
	 */
	public static final String BASE_URL = System.getProperty("relay.baseUrl", "http://pay1.printnuri.com:8090/jetwork");

	/** 중계 서버 접속 제한 시간 */
	private static final int CONNECT_TIMEOUT_MILLIS = 5000;
	/** 중계 서버 응답 제한 시간 */
	private static final int READ_TIMEOUT_MILLIS = 15000;

	private JsonHttpClient() {
	}

	/**
	 * JSON 을 POST 하고 응답 본문을 돌려준다.
	 * HTTP 200 이 아니면 경고 로그를 남기고 빈 문자열을 돌려준다.
	 *
	 * 응답이 200 이 아니어도 본문을 끝까지 읽고 닫는다. 예전에는 오류 응답 본문을 읽지 않아서
	 * 그 소켓이 반납되지 않고 GC 될 때까지 열려 있었다 (오류 응답 300번에 소켓 254개가 남음).
	 * 접속 실패·시간 초과 같은 예외가 나면 연결을 끊어 소켓을 바로 닫는다.
	 */
	public static String post(String url, JSONObject body) throws Exception {
		logger.debug("POST {}", url);

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		try {
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestMethod("POST");
			// 중계 서버가 응답하지 않으면 소켓 작업 스레드가 묶여 다른 단말기 전문까지 밀리므로 시간을 제한한다.
			con.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
			con.setReadTimeout(READ_TIMEOUT_MILLIS);

			// UTF-8 로 보낸다. (예전에는 OS 기본 인코딩이라 Windows 에서는 한글이 MS949 로 나가 깨질 수 있었다)
			try (OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "UTF-8")) {
				wr.write(body.toString());
				wr.flush();
			}

			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				String error = readAll(con.getErrorStream());
				error = error.trim();
				logger.warn("중계 서버 응답 {} {} : {} {}", status, con.getResponseMessage(), url,
						error.length() > 200 ? error.substring(0, 200) + "..." : error);
				return "";
			}

			String response = readAll(con.getInputStream());
			logger.debug("중계 서버 응답 {}", response);
			return response;
		} catch (Exception e) {
			con.disconnect();
			throw e;
		}
	}

	/** 스트림을 끝까지 읽고 닫는다 (null 이면 빈 문자열). 끝까지 읽어야 연결을 다시 쓸 수 있다. */
	private static String readAll(InputStream in) throws IOException {
		if (in == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		}
		return sb.toString();
	}
}
