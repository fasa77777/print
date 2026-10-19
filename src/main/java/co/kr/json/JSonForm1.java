package co.kr.json;

import org.json.simple.JSONObject;



/**
 * 가격 정책 요청(단말기 작업 코드 02)을 중계 서버의 /printnuri/pricepolicy 로 보낸다.
 */
public class JSonForm1 {
	public void setPolicyJson(String terminalID) {
		

		JSONObject data = new JSONObject();
		data.put("terminalID", terminalID); //S03
		data.put("agentVersion","Xerox_2.11.01");
		System.out.println("11111111111111111111");
		PrintPolicyPost ps = new PrintPolicyPost();
		ps.putPost(data);
		
	}

}
