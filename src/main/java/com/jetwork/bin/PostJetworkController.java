package com.jetwork.bin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jetwork.model.A4Terminal;
import com.jetwork.model.CyberMoney;
import com.jetwork.model.CyberMoneyResult;
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
import com.jetwork.model.RecordStatus;
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

import co.kr.up.util.Mask;

/**
 * 단말기·복사기 에이전트가 호출하는 HTTP API. 모든 주소 앞에 /jetwork 가 붙는다 (실서버 jar 와 같음).
 * 요청·응답 형식은 실서버 jar 와 같다.
 * 각 API 는 요청을 받아 com.jetwork.sql 의 SQL 클래스로 DB 에 기록하고 결과를 JSON 으로 돌려준다.
 */
@RestController

@RequestMapping("/jetwork")
public class PostJetworkController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*
	Terminal
	String terminalID;
	String key;
	String license;
	
	contentType: 'application/json'
	 * */
	
    /**
     * 단말기 등록 확인.
     * jw_terminal 에 단말기 ID 와 시리얼이 함께 등록되어 있으면 버전 정보를 갱신하고 OK,
     * 없거나 DB 오류면 FALSE 로 응답한다 (실서버 jar 와 같음. DB 오류는 로그로 구분).
     */
    @RequestMapping(method = RequestMethod.POST, path = "/postTerminal" ,consumes = "application/json")
    public PayRresult postRequest(@RequestBody Terminal terminal){
    	logger.debug("단말기 확인 요청 terminalID={} serial={} terminalSerial={} agentVersion={}",
    			terminal.getTerminalID(), terminal.getSerial(), terminal.getTerminalSerial(), terminal.getAgentVersion());
    	
    	// 단말기 ID 와 시리얼이 jw_terminal 에 등록된 단말기만 OK 로 응답한다.
    	long started = System.currentTimeMillis();
    	TerminalInfoSQL.Status status = new TerminalInfoSQL().setTerminal(terminal);
    	long elapsed = System.currentTimeMillis() - started;
    	logger.info("단말기 확인 terminalID={} result={} ({}ms)", terminal.getTerminalID(), status, elapsed);

    	PayRresult pr = new PayRresult();
    	pr.setTerminalID(terminal.getTerminalID());
    	switch (status) {
    	case REGISTERED:
    		pr.setResult("OK");
    		break;
    	case NOT_REGISTERED:
    		logger.warn("미등록 단말기 접속 terminalID={} serial={} agentVersion={}",
    				terminal.getTerminalID(), terminal.getSerial(), terminal.getAgentVersion());
    		pr.setResult("FALSE");
    		break;
    	default:
    		logger.warn("단말기 확인 중 DB 오류 terminalID={}", terminal.getTerminalID());
    		pr.setResult("FALSE");
    		break;
    	}
        return pr;
    }
    
    /**
     * 사이버머니 잔액 조회.
     * 처음 보는 카드는 잔액 0원으로 새로 등록하고 0 을 돌려준다.
     */
    @RequestMapping(method = RequestMethod.POST, path = "/postCyberMoney" ,consumes = "application/json")
    public CyberMoney postRequest1(@RequestBody CyberMoney cyber){

    	//현석이 테스트용 주석처리
    	//cyber머니 DB조회
    	UserCyberMoneySQL uc = new UserCyberMoneySQL();
    	String money = uc.getUser(cyber.getUserID(), cyber.getUserCard());
    	logger.info("사이버머니 잔액 조회 terminalID={} card={} money={}", cyber.getUserID(), Mask.card(cyber.getUserCard()), money);
    	//
    	
    	cyber.setUserID(cyber.getUserID());
    	cyber.setUserCard(cyber.getUserCard());
    	cyber.setMoney(money);
    	cyber.setResult("OK");

        return cyber;
    }
    
    /**
     * 사이버머니 결제.
     * 잔액이 결제 금액 이상이면 차감하고 결제 내역을 남긴다.
     * 잔액 부족·미등록 카드·오류는 FAIL 로 응답한다. (CyberMoneyResult 참고)
     */
    @RequestMapping(method = RequestMethod.POST, path = "/postCyberMoneyPayment" ,consumes = "application/json")
    public PaymentResult postRequest2(@RequestBody PaymentKicc payment){

    	// 사이버머니 차감. 잔액 부족·미등록 카드·오류는 FAIL 로 응답한다.
    	CyberMoneyPaymentSQL cmps = new CyberMoneyPaymentSQL();
    	CyberMoneyResult result = cmps.setPayment(payment);
    	logger.info("사이버머니 결제 terminalID={} card={} amount={} result={} balance={}",
    			payment.getTerminalID(), Mask.card(payment.getUserCard()), payment.getTotalAmount(), result.getStatus(), result.getBalance());

        return result.toPaymentResult(payment.getTerminalID());
    }
    

   /**
    * 카드·교통카드 결제 기록.
    * R11 이 Y/N 이면 체크/신용카드, 아니면 R33 으로 티머니(TM)·캐시비(EB)·기타 교통카드를 구분해 jw_payment 에 기록한다.
    * 응답은 실서버 jar 와 같이 항상 OK.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/postCraditPayment" ,consumes = "application/json")
    public PaymentResult postRequest3(@RequestBody PaymentKicc payment){

    	logger.info("카드 결제 기록 terminalID={} amount={} R09={} R11={} R33={}",
    			payment.getTerminalID(), payment.getTotalAmount(), payment.getR09(), payment.getR11(), payment.getR33());
    	
    	RecordStatus status = new CreaditPaymentSQL().setPayment(payment);
    	logger.info("카드 결제 기록 결과 terminalID={} {}", payment.getTerminalID(), status);
        return status.toPaymentResult(payment.getTerminalID());
    }
   

   /**
    * 교세라 사이버머니 결제.
    * 같은 결제번호(cyberNo)로 이미 처리된 결제는 다시 차감하지 않는다. (CyberMoneyKyoceraPaymentSQL 참고)
    */
   @RequestMapping(method = RequestMethod.POST, path = "/kyocera/postCyberMoneyPayment" ,consumes = "application/json")
   public PaymentResult postRequest4(@RequestBody PaymentKyocera payment){

   	// 잔액 부족·미등록 카드·중복 결제번호·오류는 FAIL 로 응답한다.
   	CyberMoneyResult result = new CyberMoneyKyoceraPaymentSQL().setPayment(payment);
   	logger.info("교세라 사이버머니 결제 terminalID={} card={} amount={} result={} balance={}",
   			payment.getTerminalID(), Mask.card(payment.getUserCard()), payment.getTotalAmount(), result.getStatus(), result.getBalance());
   	PaymentResult pr = result.toPaymentResult(payment.getTerminalID());
   	pr.setBalance(0); // 실서버 jar 와 같이 교세라 응답에는 잔액을 넣지 않는다 (0)
   	return pr;
   } 
   
   
   /**
    * KICC 단말기 카드 결제 기록. 응답은 실서버 jar 와 같이 항상 OK.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/kicc/postCraditPayment" ,consumes = "application/json")
   public PaymentResult postRequest5(@RequestBody PaymentCreaditKicc payment){


   	logger.info("KICC 카드 결제 기록 terminalID={} amount={} R09={}", payment.getTerminalID(), payment.getTotalAmount(), payment.getR09());
   	RecordStatus status = new KiccCreaditPaymentSQL().setPayment(payment);
   	logger.info("KICC 카드 결제 기록 결과 terminalID={} {}", payment.getTerminalID(), status);
       return status.toPaymentResult(payment.getTerminalID());
   }
   
   /**
    * 단말기별 A4 흑백·컬러 가격 조회. 실제 주소는 /jetwork/printnuri/pricepolicy 이다.
   * 가격이 없으면 컬러·흑백 각각 60원(DEFAULT_PRICE)으로 응답한다.
    * 요청이 오면 단말기의 버전 정보도 갱신한다.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/printnuri/pricepolicy" ,consumes = "application/json")
   public PriceSendResult postRequest6(@RequestBody A4Terminal terminal){


   	logger.info("가격 정책 조회 terminalID={} agentVersion={}", terminal.getTerminalID(), terminal.getAgentVersion());
   	
   	//프린터 가격정보
  	PaperPriceSQL cps = new PaperPriceSQL();
   	Vector v = cps.getPrice(terminal);
   	//
   	
   	PriceSendResult ps = new PriceSendResult();
   	for (int i = 0; i < v.size(); i++) {
   		PriceResult pr = (PriceResult)v.elementAt(i);
   		if("Y".equals(pr.getA4Color())) {
   			ps.setA4color(pr.getA4PaperPrice());
   		}else if("N".equals(pr.getA4Color())) {
   			ps.setA4mono(pr.getA4PaperPrice());
   		}   		
	}
   	// 가격이 없으면(jw_paper 에 없음, 숫자가 아님, DB 오류) DEFAULT_PRICE 원으로 응답한다.
   	// 예전에는 null 로 응답해서, 매장 에이전트가 가격을 숫자로 바꾸지 못하고 기본값 5원으로 출력했다.
   	String color = priceOrDefault(ps.getA4color());
   	String mono = priceOrDefault(ps.getA4mono());
   	if (!isPrice(ps.getA4color()) || !isPrice(ps.getA4mono())) {
   		logger.warn("가격 정보 없음 → 기본 {}원 사용 terminalID={} (jw_paper 컬러={} 흑백={})", DEFAULT_PRICE,
   				terminal.getTerminalID(), ps.getA4color(), ps.getA4mono());
   	}
   	ps.setA4color(color);
   	ps.setA4mono(mono);
   	ps.setResult("OK");
    return ps;
   }

   /** 가격 정보가 없을 때 쓰는 A4 1장 가격(원). 컬러·흑백 각각 적용 */
   static final String DEFAULT_PRICE = "60";

   /** 숫자인 가격이면 공백을 뗀 값, 비었거나 숫자가 아니면 DEFAULT_PRICE */
   static String priceOrDefault(String price) {
   	return isPrice(price) ? price.trim() : DEFAULT_PRICE;
   }

   private static boolean isPrice(String price) {
   	return price != null && price.trim().matches("\\d+");
   }
   
   
   /**
    * 프린트누리 결제 기록. 실제 주소는 /jetwork/printnuri/payment 이다.
    * 소켓 서버가 단말기 결제 결과를 중계 서버로 보낼 때 이 형식을 쓴다.
    * 응답은 실서버 jar 와 같이 항상 OK.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/printnuri/payment" ,consumes = "application/json")
   public PriceSendResult postRequest7(@RequestBody PrintNuriPay printNuriPay){


   	logger.info("프린트누리 결제 기록 terminalID={} price={} payOK={}", printNuriPay.getTerminalID(), printNuriPay.getPrice(), printNuriPay.getPayOK());
   	
   	RecordStatus status = new PrintNuriCreaditPaymentSQL().setPayment(printNuriPay);
   	logger.info("프린트누리 결제 기록 결과 terminalID={} {}", printNuriPay.getTerminalID(), status);
    return status.toPriceSendResult();
   }
   
   /** Xerox 결제에서 단말기 결제 결과를 기다리는 최대 시간(초). 넘으면 결제 실패(NO)로 응답한다. */
   private static final int XEROX_RESULT_TIMEOUT_SECONDS = 120;
   /** 결제 단말기가 결제 요청을 받는 포트 */
   private static final int TERMINAL_PAY_PORT = 10000;
   /** 결제 단말기 접속 제한 시간 */
   private static final int TERMINAL_CONNECT_TIMEOUT_MILLIS = 5000;

   /**
    * Xerox 출력 결제.
    * 1. 요청의 terminalID → 요청 IP → 마지막 접속 단말기 순서로 결제할 단말기를 정해서
    *    그 단말기(10000번 포트)에 결제 요청 전문을 보내고
    * 2. 그 단말기의 결제 결과가 소켓 서버(10020번 포트)로 도착할 때까지 최대 120초 기다린 뒤
    * 3. 승인이면 OK, 거절·취소·시간 초과면 NO 로 응답한다.
    *    금액 오류나 단말기에 접속하지 못한 경우도 (500 오류 대신) NO 로 응답한다.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/xerox/xeroxPayment" ,consumes = "application/json")
   public PriceXeroxSendResult postRequest8(@RequestBody XeroxPay xeroxPay, HttpServletRequest request) throws UnknownHostException, IOException{
   	PriceXeroxSendResult ps = new PriceXeroxSendResult();
   	ps.setCmd("pay");

   	// 1. 어느 단말기로 결제를 요청할지 정한다
   	//    요청에 terminalID 가 있으면 그 단말기, 없으면 요청을 보낸 IP 의 단말기,
   	//    둘 다 모르면 (예전처럼) 마지막으로 접속한 단말기
   	TerminalState terminal = TerminalRegistry.findByTerminalID(xeroxPay.getTerminalID());
   	String how = "terminalID";
   	if (terminal == null) {
   		terminal = TerminalRegistry.findByIp(request.getRemoteAddr());
   		how = "요청 IP";
   	}
   	if (terminal == null) {
   		terminal = TerminalRegistry.last();
   		how = "마지막 접속 단말기";
   		if (terminal != null) {
   			logger.warn("Xerox 결제 단말기를 특정할 수 없어 마지막으로 접속한 단말기로 보냄 {} (요청 IP {}, terminalID={})",
   					terminal, request.getRemoteAddr(), xeroxPay.getTerminalID());
   		}
   	}
   	if (terminal == null) {
   		logger.warn("Xerox 결제 요청을 보낼 단말기가 없음 (접속한 단말기 없음)");
   		ps.setResult("NO");
   		return ps;
   	}

   	// 금액이 비었거나 숫자가 아니면 (예전에는 500 오류) 결제하지 않고 NO
   	int amount;
   	try {
   		amount = Integer.parseInt(xeroxPay.getTotalAmount().replace("\"", "").trim());
   	} catch (RuntimeException e) {
   		logger.warn("Xerox 결제 금액 오류 [{}] {}", xeroxPay.getTotalAmount(), terminal);
   		ps.setResult("NO");
   		return ps;
   	}
   	if (amount <= 0) {
   		logger.warn("Xerox 결제 금액이 0원 이하 [{}] {}", amount, terminal);
   		ps.setResult("NO");
   		return ps;
   	}
   	logger.info("Xerox 결제 요청 {} ({}로 찾음) 금액={} 작업={}", terminal, how, amount, xeroxPay.getJobType());

   	// 2. 이 단말기의 이전 결제 결과를 지우고 새 결제를 시작한다
   	TerminalState.Job job = terminal.startPayment(amount, xeroxPay.getColor(), xeroxPay.getTotalPages());

   	// 3. 결제 단말기(10000번 포트)에 결제 요청 전문을 보낸다
   	//    단말기가 꺼져 있거나 응답이 없으면 (예전에는 500 오류, 접속 대기도 OS 기본값 20초 이상) 5초 만에 NO
   	try (Socket sck = new Socket()) {
   		sck.connect(new InetSocketAddress(terminal.getIp(), TERMINAL_PAY_PORT), TERMINAL_CONNECT_TIMEOUT_MILLIS);
   		sck.setSoTimeout(60000);
   		OutputStream sos = sck.getOutputStream();
   		sos.write(new ReqCharge(amount).assemble());
   		sos.flush();

   		byte[] ack = new byte[3];
   		int read = sck.getInputStream().read(ack, 0, ack.length);
   		logger.debug("단말기 응답 {} bytes", read);
   	} catch (IOException e) {
   		logger.warn("결제 단말기에 결제 요청을 보내지 못함 {} : {}", terminal, e.toString());
   		terminal.finishPayment(job);
   		ps.setResult("NO");
   		return ps;
   	}

   	// 4. 단말기가 10020번 소켓 서버로 결과를 보내면 SocketServerDriver 가 이 단말기의 결과로 기록한다
   	Boolean approved;
   	try {
   		approved = terminal.awaitResult(XEROX_RESULT_TIMEOUT_SECONDS * 1000L);
   	} catch (InterruptedException e) {
   		Thread.currentThread().interrupt();
   		approved = null;
   	} finally {
   		// 결제가 끝났으므로 진행 중인 작업을 지운다. (이후 카드를 태그해도 이전 금액으로 결제되지 않게)
   		terminal.finishPayment(job);
   	}

   	if (approved == null) {
   		logger.info("Xerox 결제 결과 대기 시간 초과 ({}초) {}", XEROX_RESULT_TIMEOUT_SECONDS, terminal);
   		ps.setResult("NO");
   	} else {
   		ps.setResult(approved ? "OK" : "NO");
   	}
   	return ps;
   }

   /**
    * 프린터 상태 등록·갱신 (실서버 jar 와 같음).
    * 시리얼 번호가 [SONTECH].[dbo].[printer] 에 있으면 갱신하고 OK, 없으면 새로 등록하고 FALSE (실서버 jar 와 같은 응답).
    */
   @RequestMapping(method = RequestMethod.POST, path = "/postPrinter" ,consumes = "application/json")
   public PrinterResult postRequest9(@RequestBody Printer printer){
   	logger.info("프린터 상태 devid={} site={} serial={} total={}", printer.getDevid(), printer.getSitename(),
   			printer.getSerialnumber(), printer.getTotal());
   	boolean check = new PrinterSQL().setPrinter(printer);
   	PrinterResult pr = new PrinterResult();
   	pr.setSerialnumber(printer.getSerialnumber());
   	pr.setResult(check ? "OK" : "FALSE");
   	return pr;
   }

}
