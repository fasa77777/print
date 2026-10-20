package co.kr.json;

import java.io.BufferedReader;
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
	 * HTTP 200 이 아니면 응답 메시지를 출력하고 빈 문자열을 돌려준다.
	 */
	public static String post(String url, JSONObject body) throws Exception {
		logger.debug("POST {}", url);

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
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
			logger.warn("중계 서버 응답 {} {} : {}", status, con.getResponseMessage(), url);
			return "";
		}

		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		}
		logger.debug("중계 서버 응답 {}", sb);
		return sb.toString();
	}
}
