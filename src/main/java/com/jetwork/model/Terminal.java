package com.jetwork.model;

public class Terminal {
	String terminalID;
	String key;
	String license;
	String serial;
	String result;
	String black;
	String blue;
	String red;
	String yello;	
	String terminalSerial;
	String agentVersion = "1.1.1";
	String terminalVersion = "1052";
	
	
	
	public String getTerminalVersion() {
		return terminalVersion;
	}
	public void setTerminalVersion(String terminalVersion) {
		this.terminalVersion = terminalVersion;
	}
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
