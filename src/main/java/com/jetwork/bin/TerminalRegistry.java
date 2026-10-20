package com.jetwork.bin;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 결제 단말기별 상태(TerminalState) 모음. 단말기는 소켓에 접속한 IP 로 구분한다.
 */
public final class TerminalRegistry {

	private static final ConcurrentHashMap<String, TerminalState> BY_IP = new ConcurrentHashMap<String, TerminalState>();

	/** 마지막으로 소켓에 접속한 단말기 IP (Xerox 결제에서 단말기를 특정할 수 없을 때만 쓴다) */
	private static volatile String lastIp = "";

	private TerminalRegistry() {
	}

	/** 소켓에 접속한 단말기의 상태. 처음 보는 IP 면 새로 만든다. */
	public static TerminalState connected(String ip) {
		lastIp = ip;
		TerminalState state = BY_IP.get(ip);
		if (state == null) {
			TerminalState created = new TerminalState(ip);
			state = BY_IP.putIfAbsent(ip, created);
			if (state == null) {
				state = created;
			}
		}
		return state;
	}

	/** IP 로 단말기를 찾는다. 소켓에 접속한 적이 없으면 null. */
	public static TerminalState findByIp(String ip) {
		return ip == null ? null : BY_IP.get(ip);
	}

	/** 단말기 ID 로 찾는다. 02 전문으로 ID 를 알려 준 적이 없으면 null. */
	public static TerminalState findByTerminalID(String terminalID) {
		if (terminalID == null || terminalID.trim().isEmpty()) {
			return null;
		}
		String id = terminalID.trim();
		for (TerminalState state : BY_IP.values()) {
			if (id.equals(state.getTerminalID())) {
				return state;
			}
		}
		return null;
	}

	/** 마지막으로 접속한 단말기. 아무도 접속하지 않았으면 null. */
	public static TerminalState last() {
		return findByIp(lastIp);
	}

	/** 등록된 단말기 수 */
	public static int size() {
		return BY_IP.size();
	}
}
