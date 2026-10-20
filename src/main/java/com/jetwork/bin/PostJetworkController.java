package com.jetwork.bin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import com.jetwork.model.RecordStatus;
import com.jetwork.model.Terminal;
import com.jetwork.model.XeroxPay;
import com.jetwork.sql.CreaditPaymentSQL;
import com.jetwork.sql.CyberMoneyKyoceraPaymentSQL;
import com.jetwork.sql.CyberMoneyPaymentSQL;
import com.jetwork.sql.KiccCreaditPaymentSQL;
import com.jetwork.sql.PaperPriceSQL;
import com.jetwork.sql.PrintNuriCreaditPaymentSQL;
import com.jetwork.sql.TerminalInfoSQL;
import com.jetwork.sql.UserCyberMoneySQL;

import co.kr.up.util.Mask;

/**
 * 단말기·복사기 에이전트가 호출하는 HTTP API. 모든 주소 앞에 /stech 가 붙는다.
 * 각 API 는 요청을 받아 com.jetwork.sql 의 SQL 클래스로 DB 에 기록하고 결과를 JSON 으로 돌려준다.
 */
@RestController

@RequestMapping("/stech")
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
     * 없으면 FALSE, DB 오류면 ERROR 로 응답한다.
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
    		pr.setResult("ERROR");
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
    * jw_terminal 에 없는 단말기 ID 면 기록하지 않고 FAIL(no terminal) 로 응답한다. 그 밖에는 OK.
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
   	return result.toPaymentResult(payment.getTerminalID());
   } 
   
   
   /**
    * KICC 단말기 카드 결제 기록. jw_terminal 에 없는 단말기 ID 면 기록하지 않고 FAIL(no terminal), 그 밖에는 OK.
    */
   @RequestMapping(method = RequestMethod.POST, path = "/kicc/postCraditPayment" ,consumes = "application/json")
   public PaymentResult postRequest5(@RequestBody PaymentCreaditKicc payment){


   	logger.info("KICC 카드 결제 기록 terminalID={} amount={} R09={}", payment.getTerminalID(), payment.getTotalAmount(), payment.getR09());
   	RecordStatus status = new KiccCreaditPaymentSQL().setPayment(payment);
   	logger.info("KICC 카드 결제 기록 결과 terminalID={} {}", payment.getTerminalID(), status);
       return status.toPaymentResult(payment.getTerminalID());
   }
   
   /**
    * 단말기별 A4 흑백·컬러 가격 조회. 실제 주소는 /stech/printnuri/pricepolicy 이다.
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
   	ps.setResult("OK");
    return ps;
   }
   
   
   /**
    * 프린트누리 결제 기록. 실제 주소는 /stech/printnuri/payment 이다.
    * 소켓 서버가 단말기 결제 결과를 중계 서버로 보낼 때 이 형식을 쓴다.
    * jw_terminal 에 없는 단말기 ID 면 기록하지 않고 result=FAIL.
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

   /**
    * Xerox 출력 결제.
    * 1. 요청의 terminalID → 요청 IP → 마지막 접속 단말기 순서로 결제할 단말기를 정해서
    *    그 단말기(10000번 포트)에 결제 요청 전문을 보내고
    * 2. 그 단말기의 결제 결과가 소켓 서버(10020번 포트)로 도착할 때까지 최대 120초 기다린 뒤
    * 3. 승인이면 OK, 거절·취소·시간 초과면 NO 로 응답한다.
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

   	int amount = Integer.parseInt(xeroxPay.getTotalAmount().trim());
   	logger.info("Xerox 결제 요청 {} ({}로 찾음) 금액={} 작업={}", terminal, how, amount, xeroxPay.getJobType());

   	// 2. 이 단말기의 이전 결제 결과를 지우고 새 결제를 시작한다
   	terminal.startPayment(amount, xeroxPay.getColor(), xeroxPay.getTotalPages());

   	// 3. 결제 단말기(10000번 포트)에 결제 요청 전문을 보낸다
   	try (Socket sck = new Socket(terminal.getIp(), 10000)) {
   		sck.setSoTimeout(60000);
   		OutputStream sos = sck.getOutputStream();
   		sos.write(new ReqCharge(amount).assemble());
   		sos.flush();

   		byte[] ack = new byte[3];
   		int read = sck.getInputStream().read(ack, 0, ack.length);
   		logger.debug("단말기 응답 {} bytes", read);
   	}

   	// 4. 단말기가 10020번 소켓 서버로 결과를 보내면 SocketServerDriver 가 이 단말기의 결과로 기록한다
   	Boolean approved;
   	try {
   		approved = terminal.awaitResult(XEROX_RESULT_TIMEOUT_SECONDS * 1000L);
   	} catch (InterruptedException e) {
   		Thread.currentThread().interrupt();
   		approved = null;
   	}

   	if (approved == null) {
   		logger.info("Xerox 결제 결과 대기 시간 초과 ({}초) {}", XEROX_RESULT_TIMEOUT_SECONDS, terminal);
   		ps.setResult("NO");
   	} else {
   		ps.setResult(approved ? "OK" : "NO");
   	}
   	return ps;
   }

}
