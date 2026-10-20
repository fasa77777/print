/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.json;

import co.kr.json.PServer;
import org.json.simple.JSONObject;

public class JSonForm {
    public <PostServer> void setPaymentJson(String[] sD) {
        System.out.println("sD.length====" + sD.length);
        JSONObject data = new JSONObject();
        String tID = sD[2].replaceAll("S03=", "");
        data.put((Object)"terminalID", (Object)tID);
        String price = sD[4].replaceAll("S10=", "");
        data.put((Object)"price", (Object)price);
        if (sD.length < 13) {
            data.put((Object)"payOK", (Object)"");
            data.put((Object)"craditName", (Object)"");
            data.put((Object)"payType", (Object)"");
        } else {
            String pOK = sD[13].replaceAll("R09=", "");
            data.put((Object)"payOK", (Object)pOK);
            String craditName = sD[16].replaceAll("R12=", "");
            data.put((Object)"craditName", (Object)craditName);
            String payType = sD[15].replaceAll("R11=", "");
            data.put((Object)"payType", (Object)payType);
        }
        if (sD.length < 37) {
            data.put((Object)"payTypeTM", (Object)"");
        } else {
            String payTypeTM = sD[37].replaceAll("R33=", "");
            data.put((Object)"payTypeTM", (Object)sD[37]);
        }
        data.put((Object)"paperSize", (Object)"A4");
        System.out.println("11111111111111111111");
        PServer p = new PServer();
        p.putPost(data);
    }
}
