package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.json.simple.JSONObject;

import com.jetwork.bin.JetworkServerApplication;



/**
 * 티머니 결제 승인(단말기 작업 코드 04, R32 가 FF 가 아님)을 중계 서버의 /printnuri/payment 로 보낸다.
 * 승인번호 대신 현재 시각(yyyyMMddHHmmss)을 쓴다.
 */
public class JSonForm3 {
	
	public void setPaymentJson() {
		
		JSONObject data = new JSONObject();
	//	String tID= sD[2].replaceAll("S03=", "");
		data.put("terminalID", JetworkServerApplication.terID); //S03
		//String price= sD[4].replaceAll("S10=", "");
		data.put("price", JetworkServerApplication.printPrice); //S10
		java.util.Date m_strTime = new java.util.Date(); 
		SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMddHHmmss",Locale.KOREA) ; 
		String sTime = dFormat.format(m_strTime); 
		//String pOK= sD[13].replaceAll("R09=", "");
		data.put("payOK", sTime);//R09
		
		//String craditName= sD[16].replaceAll("R12=", "");
		data.put("craditName", "");//R13
		
		//String payType= sD[15].replaceAll("R11=", "");
		data.put("payType","TM");//R11

		data.put("payTypeTM","TM");//R33
		
		
		data.put("paperSize", "A4");
		data.put("color", JetworkServerApplication.printColor);
		data.put("count", JetworkServerApplication.printCount);
		System.out.println("11111111111111111111");
		PServer ps = new PServer();
		ps.putPost(data);
		
	}

}
