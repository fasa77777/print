/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

public class PaymentResult {
	String terminalID;
	String result;
	String text;
	String reason;
	int balance;

	public int getBalance() {
		return this.balance;
	}

	public void setBalance(int balance) {
		this.balance = balance;
	}

	public String getTerminalID() {
		return this.terminalID;
	}

	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}

	public String getResult() {
		return this.result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
