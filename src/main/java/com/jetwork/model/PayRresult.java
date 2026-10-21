package com.jetwork.model;

/**
 * 단말기 등록 확인 응답 (/jetwork/postTerminal).
 */
public class PayRresult {

	/** 단말기 ID */
	String terminalID;
	/** OK = 등록된 단말기, FALSE = 미등록 또는 DB 오류 */
	String result;

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
}
