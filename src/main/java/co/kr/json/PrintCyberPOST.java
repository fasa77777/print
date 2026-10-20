package co.kr.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.TerminalState;

/** 사이버머니 결제를 중계 서버의 /kyocera/postCyberMoneyPayment 로 요청하고, 그 단말기의 결제 결과로 기록한다. */
public class PrintCyberPOST {

	private static final Logger logger = LoggerFactory.getLogger(PrintCyberPOST.class);

	// 중계 서버에서 /jetwork/kyocera/postCyberMoneyPayment 가 있는 것을 확인했다. (예전 주소의 '//' 는 정리)
	private static final String URL = JsonHttpClient.BASE_URL + "/kyocera/postCyberMoneyPayment";

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
				// 결제가 됐는지 알 수 없으면 승인으로 보지 않는다.
				// (예전에는 결과를 기록하지 않아 Xerox 결제 API 가 120초를 다 기다린 뒤 NO 로 응답했다)
				terminal.setResult(false);
				logger.warn("사이버머니 응답 결과를 알 수 없음 {} : {}", terminal, response);
			}
		} catch (Exception e) {
			// 중계 서버 접속 실패·시간 초과·응답 형식 오류도 바로 실패로 알린다.
			terminal.setResult(false);
			logger.warn("사이버머니 결제 요청 실패 {} : {}", terminal, e.toString());
		}
	}
}
