package com.jetwork.bin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * 디버깅 페이지용 최근 요청 기록 (디버깅 페이지를 켰을 때만 동작).
 *
 * 요청마다 주소·본문·응답·걸린 시간과 그 요청에서 실행한 SQL 을 모아 최근 MAX_RECORDS 건만 메모리에 둔다.
 * 본문은 TraceFilter.MAX_BODY, SQL 은 요청당 MAX_SQL 줄까지만 담아서 메모리가 계속 늘지 않는다.
 * 카드번호는 TraceFilter·SqlTrace 가 가린 뒤에 넘겨준다.
 */
public final class RequestRecorder {

	/** 남겨 두는 최근 요청 수 */
	static final int MAX_RECORDS = 200;
	/** 요청 하나에 남기는 SQL 줄 수 */
	static final int MAX_SQL = 100;

	private static volatile boolean enabled;
	private static final Deque<Record> RECENT = new ArrayDeque<Record>();
	private static final ThreadLocal<Record> CURRENT = new ThreadLocal<Record>();

	private RequestRecorder() {
	}

	/** 디버깅 페이지가 켜져 있는지 */
	public static boolean enabled() {
		return enabled;
	}

	static void enable() {
		enabled = true;
	}

	/** 요청 처리 시작. 이 스레드에서 실행하는 SQL 이 이 요청에 붙는다 */
	static Record start(long id, String method, String uri, String ip) {
		Record r = new Record(id, method, uri, ip);
		CURRENT.set(r);
		return r;
	}

	/** 지금 처리 중인 요청에 SQL 한 줄을 붙인다 (요청 처리 중이 아니면 무시) */
	public static void sql(String line) {
		Record r = CURRENT.get();
		if (r != null) {
			r.addSql(line);
		}
	}

	/** 요청 처리 끝. 최근 기록에 넣는다 */
	static void finish(Record r, int status, long elapsedMillis, String requestBody, String responseBody) {
		CURRENT.remove();
		r.status = status;
		r.elapsedMillis = elapsedMillis;
		r.requestBody = requestBody;
		r.responseBody = responseBody;
		synchronized (RECENT) {
			RECENT.addFirst(r);
			while (RECENT.size() > MAX_RECORDS) {
				RECENT.removeLast();
			}
		}
	}

	/** 번호가 afterId 보다 큰 기록 (최신 순) */
	static List<Record> since(long afterId) {
		List<Record> out = new ArrayList<Record>();
		synchronized (RECENT) {
			for (Record r : RECENT) {
				if (r.id > afterId) {
					out.add(r);
				}
			}
		}
		return out;
	}

	static void clear() {
		synchronized (RECENT) {
			RECENT.clear();
		}
	}

	/** 요청 한 건 (JSON 으로 디버깅 페이지에 보낸다) */
	public static final class Record {
		private final long id;
		private final long time = System.currentTimeMillis();
		private final String method;
		private final String uri;
		private final String ip;
		private int status;
		private long elapsedMillis;
		private String requestBody;
		private String responseBody;
		private final List<String> sql = new ArrayList<String>();
		private int sqlDropped;

		Record(long id, String method, String uri, String ip) {
			this.id = id;
			this.method = method;
			this.uri = uri;
			this.ip = ip;
		}

		void addSql(String line) {
			if (sql.size() < MAX_SQL) {
				sql.add(line);
			} else {
				sqlDropped++;
			}
		}

		public long getId() {
			return id;
		}

		public long getTime() {
			return time;
		}

		public String getMethod() {
			return method;
		}

		public String getUri() {
			return uri;
		}

		public String getIp() {
			return ip;
		}

		public int getStatus() {
			return status;
		}

		public long getElapsedMillis() {
			return elapsedMillis;
		}

		public String getRequestBody() {
			return requestBody;
		}

		public String getResponseBody() {
			return responseBody;
		}

		public List<String> getSql() {
			return Collections.unmodifiableList(sql);
		}

		public int getSqlDropped() {
			return sqlDropped;
		}
	}
}
