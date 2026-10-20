/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.json;

import co.kr.json.PServer;
import com.jetwork.bin.JetworkServerApplication;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.simple.JSONObject;

public class JSonForm3 {
    public void setPaymentJson() {
        JSONObject data = new JSONObject();
        data.put((Object)"terminalID", (Object)JetworkServerApplication.terID);
        data.put((Object)"price", (Object)JetworkServerApplication.printPrice);
        Date m_strTime = new Date();
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);
        String sTime = dFormat.format(m_strTime);
        data.put((Object)"payOK", (Object)sTime);
        data.put((Object)"craditName", (Object)"");
        data.put((Object)"payType", (Object)"TM");
        data.put((Object)"payTypeTM", (Object)"TM");
        data.put((Object)"paperSize", (Object)"A4");
        data.put((Object)"color", (Object)JetworkServerApplication.printColor);
        data.put((Object)"count", (Object)JetworkServerApplication.printCount);
        System.out.println("11111111111111111111");
        PServer ps = new PServer();
        ps.putPost(data);
    }
}
