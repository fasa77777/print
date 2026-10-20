package co.kr.json;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;





/**
 * 카드 결제 승인 결과(단말기 작업 코드 04, R04=0000)를 중계 서버의 /printnuri/payment 로 보낸다.
 * 단말기 전문을 ';' 로 나눈 "키=값" 목록에서 키로 값을 꺼낸다 (S03=단말기 ID, S10=금액, R09=승인번호, R11=체크카드 여부, R12=발급사 코드, R33=교통카드 구분).
 */
public class JSonForm {
	
	public void setPaymentJson(String [] sD) {
		// 위치(sD[2] 등)로 꺼내면 빈 필드가 하나만 끼어도 값이 밀려 엉뚱한 값이 기록되므로, 키로 찾는다.
		Map<String, String> f = toMap(sD);

		JSONObject data = new JSONObject();
		data.put("terminalID", f.getOrDefault("S03", ""));
		data.put("price", f.getOrDefault("S10", ""));
		data.put("payOK", f.getOrDefault("R09", ""));      // 승인번호
		data.put("craditName", f.getOrDefault("R12", "")); // 발급사 코드
		data.put("payType", f.getOrDefault("R11", ""));    // 체크카드 여부
		data.put("payTypeTM", f.getOrDefault("R33", ""));  // 교통카드 구분
		data.put("paperSize", "A4");

		new PServer().putPost(data);
	}

	/** "키=값" 목록을 맵으로 바꾼다. 키는 '=' 앞 3글자(S03, R04 등), 값은 앞뒤 공백을 뗀다. */
	public static Map<String, String> toMap(String[] sD) {
		Map<String, String> map = new HashMap<String, String>();
		for (String field : sD) {
			int eq = field.indexOf('=');
			if (eq >= 3) {
				map.put(field.substring(eq - 3, eq), field.substring(eq + 1).trim());
			}
		}
		return map;
	}

}
