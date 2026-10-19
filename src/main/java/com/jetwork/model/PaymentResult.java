package com.jetwork.model;

/**
 * 결제 응답 (사이버머니·카드 결제 API 공통).
 */
public class PaymentResult {
	
	/** 단말기 ID */
	String terminalID;
	/** OK = 결제 성공, FAIL = 실패 */
	String result;
	/** 단말기 화면에 보여 줄 안내 문구 */
	String text;
	/** 실패 사유 코드 (ok, no card, no money, duplicate, error) */
	String reason;	
	/** 사이버머니 잔액 */
	int balance;
	
	
	

	public int getBalance() {
		return balance;
	}
	public void setBalance(int balance) {
		this.balance = balance;
	}
	public String getTerminalID() {
		return terminalID;
	}
	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	

}
