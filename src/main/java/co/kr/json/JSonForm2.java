/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.json;

import co.kr.json.PrintCyberPOST;
import com.jetwork.bin.JetworkServerApplication;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.simple.JSONObject;

public class JSonForm2 {
    public void setPaymentJson(String card) {
        JSONObject data = new JSONObject();
        data.put((Object)"terminalID", (Object)JetworkServerApplication.terID);
        data.put((Object)"totalAmount", (Object)JetworkServerApplication.printPrice);
        SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss");
        Date time = new Date();
        String time1 = format1.format(time);
        System.out.println(time1);
        data.put((Object)"cyberNo", (Object)time1);
        data.put((Object)"userCard", (Object)card);
        data.put((Object)"jobType", (Object)"print");
        data.put((Object)"paperSize", (Object)"A4");
        data.put((Object)"color", (Object)JetworkServerApplication.printColor);
        data.put((Object)"totalPages", (Object)JetworkServerApplication.printCount);
        System.out.println("11111111111111111111");
        PrintCyberPOST ps = new PrintCyberPOST();
        ps.putPost(data);
    }
}
