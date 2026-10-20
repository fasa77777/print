package co.kr.json;

import org.json.simple.JSONObject;



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
