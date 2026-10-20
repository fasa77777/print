package co.kr.json;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 카드·티머니 결제 결과를 중계 서버의 /printnuri/payment 로 보낸다. */
public class PServer {

	private static final Logger logger = LoggerFactory.getLogger(PServer.class);
	private static final String URL = JsonHttpClient.BASE_URL + "/printnuri/payment";

	public void putPost(JSONObject jo) {
		try {
			JsonHttpClient.post(URL, jo);
		} catch (Exception e) {
			logger.warn("결제 기록 전송 실패 {} : {}", jo.get("terminalID"), e.toString());
		}
	}
}
