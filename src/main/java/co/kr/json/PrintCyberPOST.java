package co.kr.json;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jetwork.bin.JetworkServerApplication;

public class PrintCyberPOST {
	public void putPost(JSONObject jo) {
		String url="http://pay1.printnuri.com:8090/jetwork//kyocera/postCyberMoneyPayment";
		String json = "";
		try {
			System.out.println("2222222222222222222222222222222");
			URL object=new URL(url);
			HttpURLConnection con = (HttpURLConnection) object.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestMethod("POST");
			System.out.println("33333333333333333333333333333333333");
			OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
			wr.write(jo.toString());
			wr.flush();
			System.out.println("444444444444444444444444444444444");
			//display what returns the POST request

			StringBuilder sb = new StringBuilder();  
			int HttpResult = con.getResponseCode(); 
			if (HttpResult == HttpURLConnection.HTTP_OK) {
			       BufferedReader br = new BufferedReader(
			               new InputStreamReader(con.getInputStream(), "utf-8"));
			       String line = null;  
			       while ((line = br.readLine()) != null) {  
			           sb.append(line + "\n");  
			       }
			       br.close();
			       System.out.println("[" + sb.toString()+"]");  
			       json =  sb.toString();
			       
			} else {
			       System.out.println(con.getResponseMessage());  
			}  
			
			 System.out.println("[" + json+"]");  
			JSONParser parser = new JSONParser();
			JSONObject object1 = (JSONObject) parser.parse(json);
			
			System.out.println("[" + object1+"]");  
			//JSONObject parse_response = (JSONObject) object1.get("");

		//	String color = (String) parse_response.get("a4color");
		//	String mono = (String) parse_response.get("a4mono");
			String result = (String) object1.get("result");

			System.out.println("result"+result);
			
			if("OK".equals(result)) {
				JetworkServerApplication.chk = true;
				JetworkServerApplication.chk1 = true;
			
				System.out.println("승인");
			}else if("FAIL".equals(result)) {
				JetworkServerApplication.chk = false;
				JetworkServerApplication.chk1 = true;
				System.out.println("승인거절");
			}
			
			
			


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
