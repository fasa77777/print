package com.jetwork.bin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.jetwork.model.PaymentKyocera;
import com.jetwork.model.PaymentResult;
import com.jetwork.sql.CyberMoneyKyoceraPaymentSQL;
 
public class PostAPIController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // http통신할때 post는 body에다 data를 집어넣어서 받아오겠다.
    // @RequestBody에 SearchVO에 있는 값을 매칭해서 보내줘라.
    
	   @RequestMapping(method = RequestMethod.POST, path = "/CyberMoneyPayment" ,consumes = "application/json")
	   public PaymentResult postRequest4(@RequestBody PaymentKyocera payment){
	   	logger.info("CyberMoney");
	   	logger.info(payment.getUserCard());
	   	
	   	
	   	//cyber머니 과금 처리
	   	CyberMoneyKyoceraPaymentSQL cmps = new CyberMoneyKyoceraPaymentSQL();
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

	   	}else if(i < 0){
	       	pr.setTerminalID(payment.getTerminalID());
	       	pr.setResult("FAIL");
	       	pr.setText("잔액부족 충전후 사용하세요");
	       	pr.setReason("no money");
	   	}
	   	
	   	else {
	       	pr.setTerminalID(payment.getTerminalID());
	       	pr.setResult("OK");
	       	pr.setText("정상결제 되었습니다.");
	       	pr.setReason("ok");
	   	}
	 
	       return pr;
	   } 
 
}
