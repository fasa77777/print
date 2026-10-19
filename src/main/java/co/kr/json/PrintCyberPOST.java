package co.kr.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jetwork.bin.JetworkServerApplication;

/** 사이버머니 결제를 중계 서버의 /kyocera/postCyberMoneyPayment 로 요청하고 승인 여부를 기록한다. */
public class PrintCyberPOST {

	// 기존 주소에 있던 '//' 를 그대로 유지한다.
	private static final String URL = JsonHttpClient.BASE_URL + "//kyocera/postCyberMoneyPayment";

	public void putPost(JSONObject jo) {
		try {
			String json = JsonHttpClient.post(URL, jo);
			JSONObject response = (JSONObject) new JSONParser().parse(json);
			String result = (String) response.get("result");
			System.out.println("result" + result);

			if ("OK".equals(result)) {
				JetworkServerApplication.chk = true;
				JetworkServerApplication.chk1 = true;
				System.out.println("승인");
			} else if ("FAIL".equals(result)) {
				JetworkServerApplication.chk = false;
				JetworkServerApplication.chk1 = true;
				System.out.println("승인거절");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
