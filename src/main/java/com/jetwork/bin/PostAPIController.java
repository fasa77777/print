/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.model.PaymentKyocera;
import com.jetwork.model.PaymentResult;
import com.jetwork.sql.CyberMoneyKyoceraPaymentSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class PostAPIController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(method={RequestMethod.POST}, path={"/CyberMoneyPayment"}, consumes={"application/json"})
    public PaymentResult postRequest4(@RequestBody PaymentKyocera payment) {
        this.logger.info("CyberMoney");
        this.logger.info(payment.getUserCard());
        CyberMoneyKyoceraPaymentSQL cmps = new CyberMoneyKyoceraPaymentSQL();
        String money = cmps.setPayment(payment);
        PaymentResult pr = new PaymentResult();
        System.out.println("money === " + money);
        System.out.println("\"0\".equals(money) === " + "0".equals(money));
        int i = Integer.parseInt(money);
        if ("0".equals(money)) {
            pr.setTerminalID(payment.getTerminalID());
            pr.setResult("FAIL");
            pr.setText("등록되지않은 카드입니다.");
            pr.setReason("no card");
        } else if (i < 0) {
            pr.setTerminalID(payment.getTerminalID());
            pr.setResult("FAIL");
            pr.setText("잔액부족 충전후 사용하세요");
            pr.setReason("no money");
        } else {
            pr.setTerminalID(payment.getTerminalID());
            pr.setResult("OK");
            pr.setText("정상결제 되었습니다.");
            pr.setReason("ok");
        }
        return pr;
    }
}
