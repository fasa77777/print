package com.jetwork.bin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
import com.jetwork.sql.PaymentCK;
import com.jetwork.sql.PrintNuriCreaditPaymentSQL;
import com.jetwork.sql.PrinterSQL;
import com.jetwork.sql.TerminalInfoSQL;
import com.jetwork.sql.UserCyberMoneySQL;

@RestController

@RequestMapping("/printnuri")
public class PostJetworkController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*
	Terminal
	String terminalID;
	String key;
	String license;
	
	contentType: 'application/json'
	 * */
	
    @RequestMapping(method = RequestMethod.POST, path = "/postTerminal" ,consumes = "application/json")
    public PayRresult postRequest(@RequestBody Terminal terminal){
    	logger.info(terminal.getTerminalID());
    	logger.info(terminal.getLicense());
    	logger.info(terminal.getSerial());
    	logger.info(terminal.getKey());   
    	logger.info(terminal.getBlack());
    	logger.info(terminal.getBlue());
    	logger.info(terminal.getRed());
    	logger.info(terminal.getYello());   
    	logger.info(terminal.getTerminalSerial());  
    	logger.info(terminal.getAgentVersion()); 
    	logger.info(terminal.getTerminalVersion());
    	
    	//terminal  DB
    	//현석이 테스트용 주석처리
    	TerminalInfoSQL tfs = new TerminalInfoSQL();
    	boolean check = tfs.setTerminal(terminal);
    	
    	PayRresult pr = new PayRresult();

    	pr.setTerminalID(terminal.getTerminalID());
    	if(check) {
    		pr.setResult("OK");
    		
    		System.out.println("OK");
    	}else {
    		pr.setResult("FALSE");
    	}
    	
    	//PayRresult pr1 = new PayRresult();
    	pr.setResult("OK");
        return pr;
    }
    
    @RequestMapping(method = RequestMethod.POST, path = "/postCyberMoney" ,consumes = "application/json")
    public CyberMoney postRequest1(@RequestBody CyberMoney cyber){
    	logger.info(cyber.getUserID());
    	logger.info(cyber.getUserCard());
    	//현석이 테스트용 주석처리
    	//cyber머니 DB조회
    	UserCyberMoneySQL uc = new UserCyberMoneySQL();
    	String money = uc.getUser(cyber.getUserID(), cyber.getUserCard());
    	logger.info("money ===== "+money);
    	//
    	
    	cyber.setUserID(cyber.getUserID());
    	cyber.setUserCard(cyber.getUserCard());
    	cyber.setMoney(money);
    	cyber.setResult("OK");

        return cyber;
    }
    
    @RequestMapping(method = RequestMethod.POST, path = "/postCyberMoneyPayment" ,consumes = "application/json")
    public PaymentResult postRequest2(@RequestBody PaymentKicc payment){
    	logger.info("CyberMoney");
    	logger.info(payment.getUserCard());
    	
    	//현석이 테스트용 주석처리
    	//cyber머니 과금 처리
    	CyberMoneyPaymentSQL cmps = new CyberMoneyPaymentSQL();
    	String money = cmps.setPayment(payment);
    	//
    	PaymentResult pr = new PaymentResult();
    	
    	System.out.println("money === "+money);
     	System.out.println("\"0\".equals(money) === "+"0".equals(money));
     	
     	int i = Integer.parseInt(money);
     	
    	if("0".equals(money)) {    	  
        	pr.setTerminalID(payment.getTerminalID());
        	pr.setResult("FAIL");
        	pr.setText("등록되지않은 카드입니다.");
        	pr.setReason("no card");
        	pr.setBalance(0);

    	}else if(i < 0){
        	pr.setTerminalID(payment.getTerminalID());
        	pr.setResult("FAIL");
        	pr.setText("잔액부족 충전후 사용하세요");
        	pr.setReason("no money");
        	pr.setBalance(i);
    	}
    	
    	else {
        	pr.setTerminalID(payment.getTerminalID());
        	pr.setResult("OK");
        	pr.setText("정상결제 되었습니다.");
        	pr.setReason("ok");
        	pr.setBalance(i);
    	}
    	//PaymentResult pr = new PaymentResult();
    	pr.setTerminalID(payment.getTerminalID());
    	pr.setResult("OK");
    	pr.setText("정상결제 되었습니다.");
    	pr.setReason("ok");
    	
    	
    	
        return pr;
    }
    

   @RequestMapping(method = RequestMethod.POST, path = "/postCraditPayment" ,consumes = "application/json")
    public PaymentResult postRequest3(@RequestBody PaymentKicc payment){
	    //logger.info("requst body : {}", payment.); 

    	logger.info(payment.getSecureID());
    	logger.info(payment.getTerminalID());
    	logger.info(payment.getTotalAmount());
    	//logger.info(payment.getTerminalID());
    	//logger.info(payment.getUserID());
    	
    	//신용카드,체크카드,티머니,캐시비 과금 처리
    	//현석이 테스트용 주석처리
//    	
    	CreaditPaymentSQL cps = new CreaditPaymentSQL();
    	cps.setPayment(payment);
    	//
    	PaymentResult pr = new PaymentResult();
    	pr.setTerminalID(payment.getTerminalID());
    	pr.setResult("OK");
    	System.out.println("리턴 OK");
        return pr;
    }
   

   @RequestMapping(method = RequestMethod.POST, path = "/kyocera/postCyberMoneyPayment" ,consumes = "application/json")
   public PaymentResult postRequest4(@RequestBody PaymentKyocera payment){
   	logger.info("CyberMoney");
   	logger.info(payment.getUserCard());
   	
  //현석이 테스트용 주석처리
   	//cyber머니 과금 처리
//   	CyberMoneyKyoceraPaymentSQL cmps = new CyberMoneyKyoceraPaymentSQL();
//   	String money = cmps.setPayment(payment);
//   	
//   	String bi = cmps.reMoney(payment.getUserCard());
//   	//
//   	PaymentResult pr = new PaymentResult();
//   	
//   	System.out.println("money === "+money);
//    	System.out.println("\"0\".equals(money) === "+"0".equals(money));
//    	
//    	int i = Integer.parseInt(money);
//    	
//   	if("0".equals(money)) {    	  
//       	pr.setTerminalID(payment.getTerminalID());
//       	pr.setResult("FAIL");
//       	pr.setText("등록되지않은 카드입니다.");
//       	pr.setReason("no card");
//       //	pr.setBalance("0");
//
//   	}else if(i < 0){
//       	pr.setTerminalID(payment.getTerminalID());
//       	pr.setResult("FAIL");
//       	pr.setText("잔액부족 충전후 사용하세요");
//       	pr.setReason("no money");
//      // 	pr.setBalance(payment.getTotalAmount());
//   	}
//   	
//   	else {
//       	pr.setTerminalID(payment.getTerminalID());
//       	pr.setResult("OK");
//       	pr.setText("정상결제 되었습니다.");
//       	pr.setReason("ok");
//       	
//    //   	pr.setBalance(bi);
//   	}
       PaymentResult pr = new PaymentResult();
       return pr;
   } 
   
   
   @RequestMapping(method = RequestMethod.POST, path = "/kicc/postCraditPayment" ,consumes = "application/json")
   public PaymentResult postRequest5(@RequestBody PaymentCreaditKicc payment){
	    //logger.info("requst body : {}", payment.); 


   	logger.info(payment.getTerminalID());
   	logger.info(payment.getTotalAmount());
   	//logger.info(payment.getTerminalID());
   	//logger.info(payment.getUserID());
    //현석이 테스트용 주석처리
   	//신용카드,체크카드,티머니,캐시비 과금 처리
   	KiccCreaditPaymentSQL cps = new KiccCreaditPaymentSQL();
   	cps.setPayment(payment);
   	//
   	PaymentResult pr = new PaymentResult();
   	pr.setTerminalID(payment.getTerminalID());
   	pr.setResult("OK");

       return pr;
   }
   
   @RequestMapping(method = RequestMethod.POST, path = "/printnuri/pricepolicy" ,consumes = "application/json")
   public PriceSendResult postRequest6(@RequestBody A4Terminal terminal){
	    //logger.info("requst body : {}", payment.); 


   	logger.info(terminal.getTerminalID());
   	logger.info(terminal.getAgentVersion());
   	logger.info(terminal.getVersion());
   	//logger.info(payment.getTotalAmount());
   	//logger.info(payment.getTerminalID());
   	//logger.info(payment.getUserID());
   	
   	//프린터 가격정보
  	PaperPriceSQL cps = new PaperPriceSQL();
   	Vector v = cps.getPrice(terminal);
   	//
   	
   	PriceSendResult ps = new PriceSendResult();
   	for (int i = 0; i < v.size(); i++) {
   		PriceResult pr = (PriceResult)v.elementAt(i);
   		if("Y".equals(pr.getA4Color())) {
   			System.out.println(pr.getA4PaperPrice());
   			ps.setA4color(pr.getA4PaperPrice());
   		}else if("N".equals(pr.getA4Color())) {
   			System.out.println(pr.getA4PaperPrice());
   			ps.setA4mono(pr.getA4PaperPrice());
   		}   		
	}   	
   	ps.setResult("OK");
    return ps;
   }
   
   
   @RequestMapping(method = RequestMethod.POST, path = "/printnuri/payment" ,consumes = "application/json")
   public PriceSendResult postRequest7(@RequestBody PrintNuriPay printNuriPay){
	    //logger.info("requst body : {}", payment.); 


   	logger.info(printNuriPay.getTerminalID());
   	//logger.info(payment.getTotalAmount());
   	//logger.info(payment.getTerminalID());
   	//logger.info(payment.getUserID());
   	
   	//프린터 가격정보
   	PrintNuriCreaditPaymentSQL cps = new PrintNuriCreaditPaymentSQL();
   	cps.setPayment(printNuriPay);
   	//
   	
   	PriceSendResult ps = new PriceSendResult();
   	ps.setResult("OK");
    return ps;
   }
   
   @RequestMapping(method = RequestMethod.POST, path = "/xerox/xeroxPayment" ,consumes = "application/json")
   public PriceXeroxSendResult postRequest8(@RequestBody XeroxPay xeroxPay) throws UnknownHostException, IOException{
	    //logger.info("requst body : {}", payment.); 


   	logger.info(xeroxPay.getJobType());
   	//logger.info(payment.getTotalAmount());
   	//logger.info(payment.getTerminalID());
   	//logger.info(payment.getUserID()); = 
   	JetworkServerApplication.printColor = xeroxPay.getColor();
   	JetworkServerApplication.printCount = xeroxPay.getTotalPages();
   	JetworkServerApplication.printPrice = Integer.parseInt(xeroxPay.getTotalAmount());
   	
   	//프린터 가격정보
   //	PrintNuriCreaditPaymentSQL cps = new PrintNuriCreaditPaymentSQL();
   //	cps.setPayment(printNuriPay);
   	//
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
	String resultData = new String(resultBytes,0,read);
	
	System.out.println("resultData : "+resultData);

//	sis.close();
//	sos.close();
//	sck.close();
	//if(socket.getKeepAlive()) {
		while(true) { 
			System.out.println("resultData : "+resultData);
			System.out.println("UPrintServer.chk1===="+JetworkServerApplication.chk1);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(JetworkServerApplication.chk1) {
				JetworkServerApplication.chk1 = false;
				System.out.println("break");
				break; 
			} 
		}
		PriceXeroxSendResult ps =new PriceXeroxSendResult();

		if(JetworkServerApplication.chk) {
			ps.setCmd("pay");
			ps.setResult("OK");
		}else {
			ps.setCmd("pay");
			ps.setResult("NO");
		}
		
		return ps;
   }
   
   @RequestMapping(method = RequestMethod.POST, path = "/postPrinter" ,consumes = "application/json")
   public PrinterResult postRequest9(@RequestBody Printer printer){
   	logger.info(printer.getDevid());
   	logger.info(printer.getSitename());
   	logger.info(printer.getSerialnumber());
   	logger.info(printer.getTotal()+"");   

   	
   	//terminal  DB
   	PrinterSQL tfs = new PrinterSQL();
   	boolean check = tfs.setPrinter(printer);
   	
   	
   	PrinterResult pr = new PrinterResult();
   	
   	pr.setSerialnumber(printer.getSerialnumber());
   	if(check) {
   		pr.setResult("OK");
   		
   		System.out.println("OK");
   	}else {
   		pr.setResult("FALSE");
   	}
   	
   	

       return pr;
   }
   
}
