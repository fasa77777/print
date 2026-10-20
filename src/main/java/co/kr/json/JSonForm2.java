package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;

import com.jetwork.bin.JetworkServerApplication;

public class JSonForm2 {
	public void setPaymentJson(String card) {
//		
//		String cmd;
//		String color;
//		String copies;
//		String duplex;
//		String jobType;
//		String mac;
//		String nUp;
//		String paperSize;
//		String scanCount;
//		String serial;
//		String terminalID;
//		String timeOut;
//		String totalAmount;
//		String totalPages;
		
		
		
		JSONObject data = new JSONObject();
		data.put("terminalID", JetworkServerApplication.terID); //S03
		data.put("totalAmount", JetworkServerApplication.printPrice); //S10
		   
	   SimpleDateFormat format1 = new SimpleDateFormat ( "yyyyMMddHHmmss");
   		
	   Date time = new Date();
	   		
	   String time1 = format1.format(time);

	   		
	   System.out.println(time1);

		data.put("cyberNo", time1);//R09
		
		data.put("userCard", card);//R13
		
		data.put("jobType","print");//R11

		
		data.put("paperSize", "A4");
		data.put("color", JetworkServerApplication.printColor);
		data.put("totalPages", JetworkServerApplication.printCount);
		System.out.println("11111111111111111111");
		PrintCyberPOST ps = new PrintCyberPOST();
		ps.putPost(data);
		
		
		
	}

}
