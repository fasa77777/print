/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.json;

import co.kr.json.PrintPolicyPost;
import org.json.simple.JSONObject;

public class JSonForm1 {
    public void setPolicyJson(String terminalID) {
        JSONObject data = new JSONObject();
        data.put((Object)"terminalID", (Object)terminalID);
        data.put((Object)"agentVersion", (Object)"Xerox_2.11.01");
        System.out.println("11111111111111111111");
        PrintPolicyPost ps = new PrintPolicyPost();
        ps.putPost(data);
    }
}
