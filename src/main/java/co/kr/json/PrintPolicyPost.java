package co.kr.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.TerminalState;

/** 단말기 가격 정책을 중계 서버의 /printnuri/pricepolicy 에 요청한다. */
public class PrintPolicyPost {

	private static final Logger logger = LoggerFactory.getLogger(PrintPolicyPost.class);
	private static final String URL = JsonHttpClient.BASE_URL + "/printnuri/pricepolicy";

	public void putPost(JSONObject jo, TerminalState terminal) {
		try {
			String json = JsonHttpClient.post(URL, jo);
			JSONObject response = (JSONObject) new JSONParser().parse(json);
			logger.info("가격 정책 응답 {} : {}", terminal, response);

			// 응답을 정상적으로 받았으면 이 단말기는 다시 요청하지 않는다. (실패하면 다음 02 전문 때 다시 요청)
			terminal.markPolicyRequested();
		} catch (Exception e) {
			logger.warn("가격 정책 요청 실패 {} : {}", terminal, e.toString());
		}
	}
}
