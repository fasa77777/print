package co.kr.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jetwork.bin.JetworkServerApplication;

/** 단말기 가격 정책을 중계 서버의 /printnuri/pricepolicy 에 요청한다. */
public class PrintPolicyPost {

	private static final String URL = JsonHttpClient.BASE_URL + "/printnuri/pricepolicy";

	public void putPost(JSONObject jo) {
		try {
			String json = JsonHttpClient.post(URL, jo);
			JSONObject response = (JSONObject) new JSONParser().parse(json);
			System.out.println("[" + response + "]");

			// 응답을 정상적으로 받았으면 다음부터는 가격 정책을 다시 요청하지 않는다.
			JetworkServerApplication.chk2 = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
