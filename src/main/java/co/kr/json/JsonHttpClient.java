package co.kr.json;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;

/**
 * 결제 중계 서버(pay1.printnuri.com)로 JSON 을 POST 하는 공통 HTTP 클라이언트.
 * 예전에는 PServer, PrintCyberPOST, PrintPolicyPost 에 같은 코드가 복사되어 있었다.
 */
public final class JsonHttpClient {

	/** 중계 서버 주소. 경로는 각 호출하는 쪽에서 붙인다. */
	public static final String BASE_URL = "http://pay1.printnuri.com:8090/jetwork";

	private JsonHttpClient() {
	}

	/**
	 * JSON 을 POST 하고 응답 본문을 돌려준다.
	 * HTTP 200 이 아니면 응답 메시지를 출력하고 빈 문자열을 돌려준다.
	 */
	public static String post(String url, JSONObject body) throws Exception {
		System.out.println("POST " + url);

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestMethod("POST");

		OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
		wr.write(body.toString());
		wr.flush();

		int status = con.getResponseCode();
		if (status != HttpURLConnection.HTTP_OK) {
			System.out.println(con.getResponseMessage());
			return "";
		}

		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		}
		System.out.println("[" + sb + "]");
		return sb.toString();
	}
}
