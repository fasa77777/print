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

	/**
	 * 이 시간 넘게 소켓 접속이 없고 진행 중인 결제도 없는 단말기 상태는 지운다.
	 * 예전에는 한 번 접속한 IP 가 영원히 남아서, 단말기 IP 가 바뀔 때마다(DHCP 등) 목록이 계속 늘었다.
	 */
	static final long STALE_MILLIS = 24L * 60 * 60 * 1000;
	/** 정리는 이 횟수만큼 접속할 때마다 한 번 한다 */
	private static final int CLEANUP_EVERY = 1000;
	private static final java.util.concurrent.atomic.AtomicInteger connects = new java.util.concurrent.atomic.AtomicInteger();

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
		state.touch();
		if (connects.incrementAndGet() % CLEANUP_EVERY == 0) {
			removeStale(System.currentTimeMillis() - STALE_MILLIS);
		}
		return state;
	}

	/** 마지막 접속이 olderThan 보다 오래됐고 진행 중인 결제가 없는 단말기 상태를 지운다. 지운 개수를 돌려준다. */
	static int removeStale(long olderThan) {
		int removed = 0;
		for (java.util.Iterator<TerminalState> it = BY_IP.values().iterator(); it.hasNext();) {
			TerminalState state = it.next();
			if (state.getLastSeen() < olderThan && state.currentJob() == null) {
				it.remove();
				removed++;
			}
		}
		return removed;
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

	/** 지금 알고 있는 단말기 상태 전부 (디버깅 페이지용 복사본) */
	static java.util.List<TerminalState> all() {
		return new java.util.ArrayList<TerminalState>(BY_IP.values());
	}

	/** 등록된 단말기 수 */
	public static int size() {
		return BY_IP.size();
	}
}
