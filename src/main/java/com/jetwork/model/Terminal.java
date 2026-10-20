package com.jetwork.model;

/**
 * 단말기 등록 확인 요청 (/stech/postTerminal).
 */
public class Terminal {
	/** 단말기 ID (jw_terminal.terminal_id) */
	String terminalID;
	/** 단말기 키 */
	String key;
	/** 라이선스 */
	String license;
	/** 단말기 시리얼 (jw_terminal.serial, ID 와 함께 일치해야 등록된 단말기) */
	String serial;
	/** 처리 결과 */
	String result;
	/** 검정 토너 정보 */
	String black;
	/** 파랑 토너 정보 */
	String blue;
	/** 빨강 토너 정보 */
	String red;
	/** 노랑 토너 정보 */
	String yello;	
	/** 단말기 하드웨어 시리얼 */
	String terminalSerial;
	/** 에이전트 버전 */
	String agentVersion = "1.1.1";
	
	
	
	public String getAgentVersion() {
		return agentVersion;
	}
	public void setAgentVersion(String agentVersion) {
		this.agentVersion = agentVersion;
	}
	public String getTerminalSerial() {
		return terminalSerial;
	}
	public void setTerminalSerial(String terminalSerial) {
		this.terminalSerial = terminalSerial;
	}
	public String getBlack() {
		return black;
	}
	public void setBlack(String black) {
		this.black = black;
	}
	public String getBlue() {
		return blue;
	}
	public void setBlue(String blue) {
		this.blue = blue;
	}
	public String getRed() {
		return red;
	}
	public void setRed(String red) {
		this.red = red;
	}
	public String getYello() {
		return yello;
	}
	public void setYello(String yello) {
		this.yello = yello;
	}

	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getTerminalID() {
		return terminalID;
	}
	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		this.license = license;
	}
	public String getSerial() {
		return serial;
	}
	public void setSerial(String serial) {
		this.serial = serial;
	}
	

}
