package co.kr.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.TerminalState;

/** 사이버머니 결제를 중계 서버의 /kyocera/postCyberMoneyPayment 로 요청하고, 그 단말기의 결제 결과로 기록한다. */
public class PrintCyberPOST {

	private static final Logger logger = LoggerFactory.getLogger(PrintCyberPOST.class);

	// 기존 주소에 있던 '//' 를 그대로 유지한다.
	private static final String URL = JsonHttpClient.BASE_URL + "//kyocera/postCyberMoneyPayment";

	public void putPost(JSONObject jo, TerminalState terminal) {
		try {
			String json = JsonHttpClient.post(URL, jo);
			JSONObject response = (JSONObject) new JSONParser().parse(json);
			String result = (String) response.get("result");

			if ("OK".equals(result)) {
				terminal.setResult(true);
				logger.info("사이버머니 승인 {}", terminal);
			} else if ("FAIL".equals(result)) {
				terminal.setResult(false);
				logger.info("사이버머니 승인 거절 {} reason={}", terminal, response.get("reason"));
			} else {
				logger.warn("사이버머니 응답 결과를 알 수 없음 {} : {}", terminal, response);
			}
		} catch (Exception e) {
			logger.warn("사이버머니 결제 요청 실패 {} : {}", terminal, e.toString());
		}
	}
}
