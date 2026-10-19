package com.jetwork.model;

/**
 * 가격 정책 요청 (/printnuri/printnuri/pricepolicy 요청 본문).
 */
public class A4Terminal {
	/** 단말기 ID */
	String terminalID;
	/** 에이전트 버전 (jw_terminal.agent_version 에 저장) */
	String agentVersion;
	/** 단말기 버전 (jw_terminal.terminalVersion 에 저장) */
	String version;
	
	
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAgentVersion() {
		return agentVersion;
	}

	public void setAgentVersion(String agentVersion) {
		this.agentVersion = agentVersion;
	}

	public String getTerminalID() {
		return terminalID;
	}

	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}
	

}
