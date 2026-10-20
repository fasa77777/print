package co.kr.json;

import org.json.simple.JSONObject;

import com.jetwork.bin.TerminalState;

/**
 * 가격 정책 요청(단말기 작업 코드 02)을 중계 서버의 /printnuri/pricepolicy 로 보낸다.
 */
public class JSonForm1 {

	public void setPolicyJson(TerminalState terminal) {
		JSONObject data = new JSONObject();
		data.put("terminalID", terminal.getTerminalID()); // S03
		data.put("agentVersion", "Xerox_2.11.01");

		new PrintPolicyPost().putPost(data, terminal);
	}
}
