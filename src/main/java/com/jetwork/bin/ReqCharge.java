/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.bin.ReqMessage;

public class ReqCharge
extends ReqMessage {
    private int price;

    public ReqCharge(int price) {
        this.price = price;
    }

    @Override
    public byte getJobCode() {
        return 4;
    }

    @Override
    public byte[] assembleBody() {
        String bodyMessage = "S01=A1;S02=40;S09=00;S10=" + this.price + ";";
        return bodyMessage.getBytes();
    }
}
