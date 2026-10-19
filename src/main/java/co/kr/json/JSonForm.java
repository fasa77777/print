package co.kr.json;

import org.json.simple.JSONObject; 
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;





public class JSonForm {
	
	public void setPaymentJson(String [] sD) {
		
		System.out.println("sD.length===="+sD.length);
		JSONObject data = new JSONObject();
		String tID= sD[2].replaceAll("S03=", "");
		data.put("terminalID", tID); //S03
		String price= sD[4].replaceAll("S10=", "");
		data.put("price", price); //S10
		if(sD.length < 13) {
			data.put("payOK", "");//R09
			
			data.put("craditName", "");//R13
			
			data.put("payType","");//R11
		}else {
			String pOK= sD[13].replaceAll("R09=", "");
			data.put("payOK", pOK);//R09
			
			String craditName= sD[16].replaceAll("R12=", "");
			data.put("craditName", craditName);//R13
			
			String payType= sD[15].replaceAll("R11=", "");
			data.put("payType",payType);//R11
		}

		if(sD.length < 37) {
			data.put("payTypeTM","");//R33
		}else {
			String payTypeTM= sD[37].replaceAll("R33=", "");
			data.put("payTypeTM", payTypeTM.trim());//R33 (예전에는 "R33=" 접두어가 붙은 채로 보내 티머니 판정이 안 됐다)
		}
		
		data.put("paperSize","A4");
//		data.put("color", UPrintServer.printColor);
//		data.put("count", UPrintServer.printCount);
		System.out.println("11111111111111111111");
		PServer p = new PServer();
		p.putPost(data);
		
	}

}
