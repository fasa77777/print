package com.jetwork.bin;

/**
 * 결제 단말기 한 대의 상태.
 *
 * 예전에는 단말기 IP, 단말기 ID, 결제 금액, 승인 결과를 서버 전체에서 static 변수 하나로 같이 써서
 * 단말기가 여러 대면 서로 덮어썼다. 이제 단말기(IP)마다 이 객체를 하나씩 둔다. (TerminalRegistry 참고)
 */
public final class TerminalState {

	private final String ip;

	/** 02 전문(가격 정책 요청)에서 알게 된 단말기 ID */
	private volatile String terminalID = "";
	/** 이 단말기의 가격 정책을 중계 서버에 이미 요청했는지 */
	private volatile boolean policyRequested;

	/** 진행 중인 출력 작업 (Xerox 결제 요청에서 받음) */
	private volatile int price;
	private volatile String color = "";
	private volatile String count = "";

	/** 결제 결과 (단말기 → 소켓 서버 → 여기 → Xerox 결제 API) */
	private final Object resultLock = new Object();
	private boolean resultArrived;
	private boolean approved;

	TerminalState(String ip) {
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}

	public String getTerminalID() {
		return terminalID;
	}

	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID == null ? "" : terminalID;
	}

	public boolean isPolicyRequested() {
		return policyRequested;
	}

	public void markPolicyRequested() {
		this.policyRequested = true;
	}

	public int getPrice() {
		return price;
	}

	public String getColor() {
		return color;
	}

	public String getCount() {
		return count;
	}

	/** 새 결제를 시작한다. 이전 결제의 결과는 지운다. */
	public void startPayment(int price, String color, String count) {
		this.price = price;
		this.color = color == null ? "" : color;
		this.count = count == null ? "" : count;
		synchronized (resultLock) {
			resultArrived = false;
			approved = false;
		}
	}

	/** 결제 결과가 도착했다. 결과를 기다리는 Xerox 결제 API 를 깨운다. */
	public void setResult(boolean approved) {
		synchronized (resultLock) {
			this.approved = approved;
			this.resultArrived = true;
			resultLock.notifyAll();
		}
	}

	/**
	 * 결제 결과를 기다린다.
	 *
	 * @return 승인이면 true, 거절·취소면 false, 시간 안에 결과가 안 오면 null
	 */
	public Boolean awaitResult(long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		synchronized (resultLock) {
			while (!resultArrived) {
				long left = deadline - System.currentTimeMillis();
				if (left <= 0) {
					return null;
				}
				resultLock.wait(left);
			}
			resultArrived = false;
			return approved;
		}
	}

	@Override
	public String toString() {
		return "단말기[" + terminalID + " " + ip + "]";
	}
}
