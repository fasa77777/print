package co.kr.json;

import org.json.simple.JSONObject;

/** 카드·티머니 결제 결과를 중계 서버의 /printnuri/payment 로 보낸다. */
public class PServer {

	private static final String URL = JsonHttpClient.BASE_URL + "/printnuri/payment";

	public void putPost(JSONObject jo) {
		try {
			JsonHttpClient.post(URL, jo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
