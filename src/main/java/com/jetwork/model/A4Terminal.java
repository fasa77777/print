package com.jetwork.model;

/**
 * 가격 정책 요청 (/stech/printnuri/pricepolicy 요청 본문).
 */
public class A4Terminal {
	/** 단말기 ID */
	String terminalID;
	/** 에이전트 버전 (jw_terminal.agent_version 에 저장) */
	String agentVersion;

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
