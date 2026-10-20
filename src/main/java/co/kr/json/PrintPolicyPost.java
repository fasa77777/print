/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.json;

import com.jetwork.bin.JetworkServerApplication;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PrintPolicyPost {
    public void putPost(JSONObject jo) {
        String url = "http://pay1.printnuri.com:8090/jetwork/printnuri/pricepolicy";
        String json = "";
        try {
            System.out.println("2222222222222222222222222222222");
            URL object = new URL(url);
            HttpURLConnection con = (HttpURLConnection)object.openConnection();
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
            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(String.valueOf(line) + "\n");
                }
                br.close();
                System.out.println("[" + sb.toString() + "]");
                json = sb.toString();
            } else {
                System.out.println(con.getResponseMessage());
            }
            System.out.println("[" + json + "]");
            JSONParser parser = new JSONParser();
            JSONObject object1 = (JSONObject)parser.parse(json);
            System.out.println("[" + object1 + "]");
            JetworkServerApplication.chk2 = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
