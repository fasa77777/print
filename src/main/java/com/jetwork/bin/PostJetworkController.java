/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.bin.JetworkServerApplication;
import com.jetwork.bin.ReqCharge;
import com.jetwork.model.A4Terminal;
import com.jetwork.model.CyberMoney;
import com.jetwork.model.PayRresult;
import com.jetwork.model.PaymentCreaditKicc;
import com.jetwork.model.PaymentKicc;
import com.jetwork.model.PaymentKyocera;
import com.jetwork.model.PaymentResult;
import com.jetwork.model.PriceResult;
import com.jetwork.model.PriceSendResult;
import com.jetwork.model.PriceXeroxSendResult;
import com.jetwork.model.PrintNuriPay;
import com.jetwork.model.Printer;
import com.jetwork.model.PrinterResult;
import com.jetwork.model.Terminal;
import com.jetwork.model.XeroxPay;
import com.jetwork.sql.CreaditPaymentSQL;
import com.jetwork.sql.CyberMoneyKyoceraPaymentSQL;
import com.jetwork.sql.CyberMoneyPaymentSQL;
import com.jetwork.sql.KiccCreaditPaymentSQL;
import com.jetwork.sql.PaperPriceSQL;
import com.jetwork.sql.PrintNuriCreaditPaymentSQL;
import com.jetwork.sql.PrinterSQL;
import com.jetwork.sql.TerminalInfoSQL;
import com.jetwork.sql.UserCyberMoneySQL;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/jetwork"})
public class PostJetworkController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(method={RequestMethod.POST}, path={"/postTerminal"}, consumes={"application/json"})
    public PayRresult postRequest(@RequestBody Terminal terminal) {
        this.logger.info(terminal.getTerminalID());
        this.logger.info(terminal.getLicense());
        this.logger.info(terminal.getSerial());
        this.logger.info(terminal.getKey());
        this.logger.info(terminal.getBlack());
        this.logger.info(terminal.getBlue());
        this.logger.info(terminal.getRed());
        this.logger.info(terminal.getYello());
        this.logger.info(terminal.getTerminalSerial());
        this.logger.info(terminal.getAgentVersion());
        this.logger.info(terminal.getTerminalVersion());
        TerminalInfoSQL tfs = new TerminalInfoSQL();
        boolean check = tfs.setTerminal(terminal);
        PayRresult pr = new PayRresult();
        pr.setTerminalID(terminal.getTerminalID());
        if (check) {
            pr.setResult("OK");
            System.out.println("OK");
        } else {
            pr.setResult("FALSE");
        }
        return pr;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/postCyberMoney"}, consumes={"application/json"})
    public CyberMoney postRequest1(@RequestBody CyberMoney cyber) {
        this.logger.info(cyber.getUserID());
        this.logger.info(cyber.getUserCard());
        UserCyberMoneySQL uc = new UserCyberMoneySQL();
        String money = uc.getUser(cyber.getUserID(), cyber.getUserCard());
        this.logger.info("money ===== " + money);
        cyber.setUserID(cyber.getUserID());
        cyber.setUserCard(cyber.getUserCard());
        cyber.setMoney(money);
        cyber.setResult("OK");
        return cyber;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/postCyberMoneyPayment"}, consumes={"application/json"})
    public PaymentResult postRequest2(@RequestBody PaymentKicc payment) {
        this.logger.info("CyberMoney");
        this.logger.info(payment.getUserCard());
        CyberMoneyPaymentSQL cmps = new CyberMoneyPaymentSQL();
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
            pr.setBalance(0);
        } else if (i < 0) {
            pr.setTerminalID(payment.getTerminalID());
            pr.setResult("FAIL");
            pr.setText("잔액부족 충전후 사용하세요");
            pr.setReason("no money");
            pr.setBalance(i);
        } else {
            pr.setTerminalID(payment.getTerminalID());
            pr.setResult("OK");
            pr.setText("정상결제 되었습니다.");
            pr.setReason("ok");
            pr.setBalance(i);
        }
        return pr;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/postCraditPayment"}, consumes={"application/json"})
    public PaymentResult postRequest3(@RequestBody PaymentKicc payment) {
        this.logger.info(payment.getSecureID());
        this.logger.info(payment.getTerminalID());
        this.logger.info(payment.getTotalAmount());
        CreaditPaymentSQL cps = new CreaditPaymentSQL();
        cps.setPayment(payment);
        PaymentResult pr = new PaymentResult();
        pr.setTerminalID(payment.getTerminalID());
        pr.setResult("OK");
        System.out.println("리턴 OK");
        return pr;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/kyocera/postCyberMoneyPayment"}, consumes={"application/json"})
    public PaymentResult postRequest4(@RequestBody PaymentKyocera payment) {
        this.logger.info("CyberMoney");
        this.logger.info(payment.getUserCard());
        CyberMoneyKyoceraPaymentSQL cmps = new CyberMoneyKyoceraPaymentSQL();
        String money = cmps.setPayment(payment);
        String bi = cmps.reMoney(payment.getUserCard());
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

    @RequestMapping(method={RequestMethod.POST}, path={"/kicc/postCraditPayment"}, consumes={"application/json"})
    public PaymentResult postRequest5(@RequestBody PaymentCreaditKicc payment) {
        this.logger.info(payment.getTerminalID());
        this.logger.info(payment.getTotalAmount());
        KiccCreaditPaymentSQL cps = new KiccCreaditPaymentSQL();
        cps.setPayment(payment);
        PaymentResult pr = new PaymentResult();
        pr.setTerminalID(payment.getTerminalID());
        pr.setResult("OK");
        return pr;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/printnuri/pricepolicy"}, consumes={"application/json"})
    public PriceSendResult postRequest6(@RequestBody A4Terminal terminal) {
        this.logger.info(terminal.getTerminalID());
        this.logger.info(terminal.getAgentVersion());
        this.logger.info(terminal.getVersion());
        PaperPriceSQL cps = new PaperPriceSQL();
        Vector<PriceResult> v = cps.getPrice(terminal);
        PriceSendResult ps = new PriceSendResult();
        int i = 0;
        while (i < v.size()) {
            PriceResult pr = v.elementAt(i);
            if ("Y".equals(pr.getA4Color())) {
                System.out.println(pr.getA4PaperPrice());
                ps.setA4color(pr.getA4PaperPrice());
            } else if ("N".equals(pr.getA4Color())) {
                System.out.println(pr.getA4PaperPrice());
                ps.setA4mono(pr.getA4PaperPrice());
            }
            ++i;
        }
        ps.setResult("OK");
        return ps;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/printnuri/payment"}, consumes={"application/json"})
    public PriceSendResult postRequest7(@RequestBody PrintNuriPay printNuriPay) {
        this.logger.info(printNuriPay.getTerminalID());
        PrintNuriCreaditPaymentSQL cps = new PrintNuriCreaditPaymentSQL();
        cps.setPayment(printNuriPay);
        PriceSendResult ps = new PriceSendResult();
        ps.setResult("OK");
        return ps;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/xerox/xeroxPayment"}, consumes={"application/json"})
    public PriceXeroxSendResult postRequest8(@RequestBody XeroxPay xeroxPay) throws UnknownHostException, IOException {
        this.logger.info(xeroxPay.getJobType());
        JetworkServerApplication.printColor = xeroxPay.getColor();
        JetworkServerApplication.printCount = xeroxPay.getTotalPages();
        JetworkServerApplication.printPrice = Integer.parseInt(xeroxPay.getTotalAmount());
        Socket sck = null;
        OutputStream sos = null;
        InputStream sis = null;
        sck = new Socket(JetworkServerApplication.TerminalIP, 10000);
        sck.setSoTimeout(60000);
        System.out.println("=========================");
        ReqCharge reqCharge = new ReqCharge(Integer.parseInt(xeroxPay.getTotalAmount()));
        System.out.println("=========================");
        byte[] reqByte = reqCharge.assemble();
        sos = sck.getOutputStream();
        System.out.println("=========================");
        sos.write(reqByte);
        sos.flush();
        System.out.println("=========================");
        sis = sck.getInputStream();
        byte[] ack = new byte[3];
        int read = sis.read(ack, 0, ack.length);
        byte[] resultBytes = new byte[1024];
        String resultData = new String(resultBytes, 0, read);
        System.out.println("resultData : " + resultData);
        do {
            System.out.println("resultData : " + resultData);
            System.out.println("UPrintServer.chk1====" + JetworkServerApplication.chk1);
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!JetworkServerApplication.chk1);
        JetworkServerApplication.chk1 = false;
        System.out.println("break");
        PriceXeroxSendResult ps = new PriceXeroxSendResult();
        if (JetworkServerApplication.chk) {
            ps.setCmd("pay");
            ps.setResult("OK");
        } else {
            ps.setCmd("pay");
            ps.setResult("NO");
        }
        return ps;
    }

    @RequestMapping(method={RequestMethod.POST}, path={"/postPrinter"}, consumes={"application/json"})
    public PrinterResult postRequest9(@RequestBody Printer printer) {
        this.logger.info(printer.getDevid());
        this.logger.info(printer.getSitename());
        this.logger.info(printer.getSerialnumber());
        this.logger.info(String.valueOf(printer.getTotal()));
        PrinterSQL tfs = new PrinterSQL();
        boolean check = tfs.setPrinter(printer);
        PrinterResult pr = new PrinterResult();
        pr.setSerialnumber(printer.getSerialnumber());
        if (check) {
            pr.setResult("OK");
            System.out.println("OK");
        } else {
            pr.setResult("FALSE");
        }
        return pr;
    }
}
