package com.jetwork.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP 부하(스트레스) 테스트. 실제 서버에 동시 요청을 보내 처리량·응답 시간·오류율을 잰다.
 *
 * 실행 방법 (이클립스)
 *   이 파일 우클릭 → Run As → Java Application. 기본은 테스트 서버 222.122.118.8, 단말기 123456, 카드 8E156710.
 *   Run Configurations → Arguments 로 바꿀 수 있다.
 *     baseUrl=http://127.0.0.1:8090/jetwork threads=50 requests=2000 write=false
 *
 * 하는 일 (기본: 읽기 위주, DB 에 결제 기록을 남기지 않음)
 *   1. 단말기 확인 /postTerminal          (jw_terminal 의 update 시각만 바뀜)
 *   2. 가격 조회 /printnuri/pricepolicy   (jw_terminal 의 update 시각만 바뀜)
 *   3. 잔액 조회 /postCyberMoney          (카드가 없으면 0원으로 등록됨)
 *   4. 1~3 을 섞어서
 *   write=true 일 때만
 *   5. 카드 결제 기록 /postCraditPayment  (승인번호를 매번 새로 만들어 jw_payment 에 requests 건 기록 → 끝나고 지우는 SQL 을 출력)
 *
 * 결과: 요청 수, 성공·실패, 초당 처리 수, 응답 시간(평균, 50%, 95%, 99%, 최대)
 */
public class StressTest {

	static String baseUrl = "http://222.122.118.8:8090/jetwork";
	static String terminalID = "123456";
	static String serial = "erwerewrwerwer";
	static String userCard = "8E156710";
	static int threads = 50;
	static int requests = 2000;
	static boolean write = false;

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			int eq = arg.indexOf('=');
			if (eq < 0) continue;
			String k = arg.substring(0, eq), v = arg.substring(eq + 1);
			if (k.equals("baseUrl")) baseUrl = v;
			else if (k.equals("terminalID")) terminalID = v;
			else if (k.equals("serial")) serial = v;
			else if (k.equals("userCard")) userCard = v;
			else if (k.equals("threads")) threads = Integer.parseInt(v);
			else if (k.equals("requests")) requests = Integer.parseInt(v);
			else if (k.equals("write")) write = Boolean.parseBoolean(v);
		}
		System.out.println("부하 테스트  서버 " + baseUrl + "  동시 " + threads + "  요청 " + requests + "건씩  단말기 " + terminalID + "  쓰기 " + write);
		Result warm = run("준비 (연결 확인)", 1, 1, terminal());
		if (warm.ok == 0) {
			System.out.println("서버에 연결할 수 없습니다: " + warm.firstError);
			return;
		}
		List<Result> all = new ArrayList<Result>();
		all.add(run("단말기 확인 /postTerminal", threads, requests, terminal()));
		all.add(run("가격 조회 /printnuri/pricepolicy", threads, requests, price()));
		all.add(run("잔액 조회 /postCyberMoney", threads, requests, balance()));
		all.add(run("섞어서 (단말기·가격·잔액)", threads, requests, mixed(terminal(), price(), balance())));
		String prefix = null;
		if (write) {
			prefix = "ST" + (System.currentTimeMillis() % 1000000);
			all.add(run("카드 결제 기록 /postCraditPayment", threads, requests, cardPayment(prefix)));
		}
		System.out.println();
		System.out.println(Result.header());
		for (Result r : all) System.out.println(r.line());
		if (prefix != null) {
			System.out.println();
			System.out.println("테스트 결제 기록 확인·삭제:");
			System.out.println("  SELECT COUNT(*) FROM jw_payment WHERE terminal_id = '" + terminalID + "' AND payment_ok LIKE '" + prefix + "%';  -- " + requests + " 건이어야 함");
			System.out.println("  DELETE FROM jw_payment WHERE terminal_id = '" + terminalID + "' AND payment_ok LIKE '" + prefix + "%';");
		}
	}

	// ---------------- 요청 ----------------

	/** 요청 하나 (경로 + JSON 본문). n 은 0 부터 시작하는 요청 번호 */
	public interface Request {
		String path(int n);

		String body(int n);
	}

	static Request fixed(final String path, final String body) {
		return new Request() {
			public String path(int n) { return path; }
			public String body(int n) { return body; }
		};
	}

	static Request terminal() {
		return fixed("/postTerminal", "{\"terminalID\":\"" + terminalID + "\",\"serial\":\"" + serial + "\"}");
	}

	static Request price() {
		return fixed("/printnuri/pricepolicy", "{\"terminalID\":\"" + terminalID + "\",\"agentVersion\":\"PRT_1.11.01\"}");
	}

	static Request balance() {
		return fixed("/postCyberMoney", "{\"userID\":\"" + terminalID + "\",\"userCard\":\"" + userCard + "\"}");
	}

	static Request cardPayment(final String approvalPrefix) {
		return new Request() {
			public String path(int n) { return "/postCraditPayment"; }
			public String body(int n) {
				return "{\"terminalID\":\"" + terminalID + "\",\"totalAmount\":\"100\",\"paperSize\":\"A4\",\"color\":\"false\",\"jobType\":\"print\","
						+ "\"totalPages\":\"1\",\"R09\":\"" + approvalPrefix + n + "\",\"R11\":\"N\",\"R12\":\"029\",\"R33\":\"\"}";
			}
		};
	}

	static Request mixed(final Request... rs) {
		return new Request() {
			public String path(int n) { return rs[n % rs.length].path(n); }
			public String body(int n) { return rs[n % rs.length].body(n); }
		};
	}

	// ---------------- 실행 ----------------

	/** 결과 판정: HTTP 200 이고 본문에 "result" 가 있으면 성공 */
	public interface Check {
		boolean ok(int status, String body);
	}

	static final Check HTTP_OK = new Check() {
		public boolean ok(int status, String body) { return status == 200 && body.contains("\"result\""); }
	};

	public static Result run(String name, int threads, int total, Request req) throws InterruptedException {
		return run(name, threads, total, req, HTTP_OK);
	}

	/** threads 개가 동시에 total 건을 보낸다 */
	public static Result run(String name, int threads, final int total, final Request req, final Check check) throws InterruptedException {
		final AtomicInteger next = new AtomicInteger();
		final Result r = new Result(name, threads);
		final long[] lat = new long[total];
		ExecutorService ex = Executors.newFixedThreadPool(threads);
		final CountDownLatch start = new CountDownLatch(1);
		long t0 = System.nanoTime();
		for (int t = 0; t < threads; t++) {
			ex.execute(new Runnable() {
				public void run() {
					try {
						start.await();
					} catch (InterruptedException e) {
						return;
					}
					int n;
					while ((n = next.getAndIncrement()) < total) {
						long s = System.nanoTime();
						String err = null;
						try {
							HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + req.path(n)).openConnection();
							con.setConnectTimeout(10000);
							con.setReadTimeout(60000);
							con.setRequestMethod("POST");
							con.setDoOutput(true);
							con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
							OutputStream os = con.getOutputStream();
							os.write(req.body(n).getBytes(StandardCharsets.UTF_8));
							os.close();
							int st = con.getResponseCode();
							InputStream is = st < 400 ? con.getInputStream() : con.getErrorStream();
							String body = is == null ? "" : PaymentTestClient.read(is);
							if (!check.ok(st, body)) err = "HTTP " + st + " " + (body.length() > 150 ? body.substring(0, 150) : body);
						} catch (Exception e) {
							err = e.toString();
						}
						lat[n] = System.nanoTime() - s;
						if (err == null) {
							r.okCount.incrementAndGet();
						} else {
							r.errCount.incrementAndGet();
							r.addError(err);
						}
					}
				}
			});
		}
		start.countDown();
		ex.shutdown();
		ex.awaitTermination(30, TimeUnit.MINUTES);
		r.finish(System.nanoTime() - t0, lat);
		System.out.println("  " + r.line());
		return r;
	}

	/** 부하 결과 */
	public static class Result {
		final String name;
		final int threads;
		final AtomicInteger okCount = new AtomicInteger();
		final AtomicInteger errCount = new AtomicInteger();
		final Map<String, AtomicLong> errors = Collections.synchronizedMap(new LinkedHashMap<String, AtomicLong>());
		public int ok, err;
		public double seconds, rps, avgMs, p50, p95, p99, maxMs;
		public String firstError;

		Result(String name, int threads) {
			this.name = name;
			this.threads = threads;
		}

		void addError(String e) {
			String key = e.replaceAll("\\d{6,}", "#");
			synchronized (errors) {
				if (firstError == null) firstError = e;
				AtomicLong c = errors.get(key);
				if (c == null) errors.put(key, c = new AtomicLong());
				c.incrementAndGet();
			}
		}

		void finish(long nanos, long[] lat) {
			ok = okCount.get();
			err = errCount.get();
			seconds = nanos / 1e9;
			rps = (ok + err) / seconds;
			long[] l = Arrays.copyOf(lat, lat.length);
			Arrays.sort(l);
			double sum = 0;
			for (long x : l) sum += x;
			avgMs = l.length == 0 ? 0 : sum / l.length / 1e6;
			p50 = pct(l, 0.50);
			p95 = pct(l, 0.95);
			p99 = pct(l, 0.99);
			maxMs = l.length == 0 ? 0 : l[l.length - 1] / 1e6;
		}

		static double pct(long[] sorted, double p) {
			if (sorted.length == 0) return 0;
			return sorted[Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1)] / 1e6;
		}

		static String header() {
			return String.format("%-32s %5s %7s %6s %8s %7s %7s %7s %7s %8s", "시나리오", "동시", "성공", "실패", "초당", "평균ms", "50%", "95%", "99%", "최대ms");
		}

		String line() {
			String s = String.format("%-32s %5d %7d %6d %8.0f %7.1f %7.1f %7.1f %7.1f %8.1f", name, threads, ok, err, rps, avgMs, p50, p95, p99, maxMs);
			synchronized (errors) {
				for (Map.Entry<String, AtomicLong> e : errors.entrySet()) {
					s += "\n      실패 " + e.getValue() + "건: " + e.getKey();
				}
			}
			return s;
		}
	}
}
