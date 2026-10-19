package com.jetwork.model;

/**
 * Xerox 결제 응답 (/printnuri/xerox/xeroxPayment).
 */
public class PriceXeroxSendResult {
	/** OK = 결제 승인, NO = 거절·취소·시간 초과 */
	String result;
	/** 명령 (항상 pay) */
	String cmd;
	

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	

}
