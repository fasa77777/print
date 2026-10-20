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

	/**
	 * 진행 중인 출력 작업 (Xerox 결제 요청에서 받음). 없으면 null.
	 * 예전에는 결제가 끝나도 금액이 남아 있어서, 새 작업 없이 사이버머니 카드를 태그하면 이전 금액으로 결제 요청이 갔다.
	 * 이제 Xerox 결제 API 가 결과를 받거나 시간이 지나면 지운다. (finishPayment)
	 */
	private final java.util.concurrent.atomic.AtomicReference<Job> job = new java.util.concurrent.atomic.AtomicReference<Job>();

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

	/** 출력 작업 한 건 (금액·컬러·매수). 값이 바뀌지 않아서, 한 번 꺼내 두면 다른 스레드가 지워도 그대로 쓸 수 있다. */
	public static final class Job {
		private final int price;
		private final String color;
		private final String count;

		Job(int price, String color, String count) {
			this.price = price;
			this.color = color == null ? "" : color;
			this.count = count == null ? "" : count;
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
	}

	/** 진행 중인 출력 작업. 없으면 null */
	public Job currentJob() {
		return job.get();
	}

	/** 진행 중인 작업 금액. 작업이 없으면 0 */
	public int getPrice() {
		Job j = job.get();
		return j == null ? 0 : j.getPrice();
	}

	public String getColor() {
		Job j = job.get();
		return j == null ? "" : j.getColor();
	}

	public String getCount() {
		Job j = job.get();
		return j == null ? "" : j.getCount();
	}

	/**
	 * 새 결제를 시작한다. 이전 결제의 결과는 지운다.
	 *
	 * @return 시작한 작업. 끝나면 finishPayment 에 넘긴다
	 */
	public Job startPayment(int price, String color, String count) {
		Job started = new Job(price, color, count);
		synchronized (resultLock) {
			job.set(started);
			resultArrived = false;
			approved = false;
		}
		return started;
	}

	/**
	 * 결제가 끝났다(승인·거절·시간 초과). 진행 중인 작업을 지운다.
	 * 그 사이 같은 단말기에 새 결제가 시작됐으면 새 작업은 지우지 않는다.
	 */
	public void finishPayment(Job finished) {
		job.compareAndSet(finished, null);
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
