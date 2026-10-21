package com.jetwork.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.sql.DBCPConnectionMgr;
import com.jetwork.sql.DbConfig;

/**
 * DB 연결·부하 테스트 (실제 DB, 읽기 전용).
 *
 * 실행 방법 (이클립스)
 *   src/main/resources/db.properties (또는 프로젝트 폴더의 db.properties) 에 접속 정보를 넣고
 *   이 파일 우클릭 → Run As → Java Application.
 *   Arguments 로 바꿀 수 있는 값: terminalID=123456 serial=erwerewrwerwer userCard=8E156710 threads=50 requests=2000
 *
 * 하는 일 (SELECT 만 한다. DB 에 아무것도 쓰지 않는다)
 *   1. 접속: 첫 접속 시간, SQL Server 버전, DB 이름
 *   2. 테이블: 서버가 쓰는 6개 표가 있는지, 테스트 단말기·가격·카드가 있는지
 *   3. 쿼리별 응답 시간 (1개씩): 서버가 결제마다 실제로 실행하는 SELECT 들
 *   4. 동시 부하: threads 개가 위 쿼리를 섞어서 requests 건 실행 (서버와 같은 커넥션 풀 설정)
 *   5. 풀 상태: 최대 동시 사용, 끝난 뒤 반납 안 된 커넥션이 없는지
 */
public class DbStressTest {

	static String terminalID = "123456";
	static String serial = "erwerewrwerwer";
	static String userCard = "8E156710";
	static int threads = 50;
	static int requests = 2000;

	static int fail = 0;

	/** 서버가 실제로 쓰는 SELECT 들 (값은 테스트 단말기·카드) */
	static final String[][] QUERIES = {
			{ "단말기 확인 (ID+시리얼)", "SELECT 1 FROM [dbo].[jw_terminal] WHERE [terminal_id] = ? AND [serial] = ?", "T", "S" },
			{ "단말기 등록 여부", "SELECT 1 FROM [dbo].[jw_terminal] WHERE [terminal_id] = ?", "T" },
			{ "용지 가격", "SELECT * FROM jw_paper WHERE terminalID = ?", "T" },
			{ "사이버머니 잔액", "SELECT cyberMoney FROM [dbo].[cybermoney] WHERE [cardNumber] = ?", "C" },
			{ "가맹점명", "SELECT b.fran_name FROM [dbo].[jw_terminal] a, [dbo].[jw_franchise] b WHERE a.fran_id = b.fran_id AND a.terminal_id = ?", "T" },
			{ "사이버머니 중복 결제번호", "SELECT 1 FROM [dbo].[jw_payment] WHERE [userCard] = ? AND [cyberNo] = ?", "C", "N" },
			{ "카드 오늘 같은 승인번호 (결제마다)", "SELECT 1 FROM [jw_payment] WHERE (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120))  AND payment_ok = ?", "A" },
	};

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			int eq = arg.indexOf('=');
			if (eq < 0) continue;
			String k = arg.substring(0, eq), v = arg.substring(eq + 1);
			if (k.equals("terminalID")) terminalID = v;
			else if (k.equals("serial")) serial = v;
			else if (k.equals("userCard")) userCard = v;
			else if (k.equals("threads")) threads = Integer.parseInt(v);
			else if (k.equals("requests")) requests = Integer.parseInt(v);
		}

		section("1. 접속");
		String url = DbConfig.getUrl();
		System.out.println("  DB " + maskUrl(url) + "  계정 " + DbConfig.getUsername() + "  풀 최대 " + DbConfig.getPoolMaxActive());
		if (url == null || url.isEmpty()) {
			bad("DB 접속 정보가 없습니다 (db.properties 의 db.url)");
			finish();
			return;
		}
		long t0 = System.currentTimeMillis();
		DBCPConnectionMgr pool = ConnectionPool.getInst();
		Connection c = pool.getConnection();
		long first = System.currentTimeMillis() - t0;
		if (c == null) {
			bad("DB 에 접속하지 못했습니다 (" + first + "ms). 주소·포트·계정·방화벽을 확인하세요.");
			finish();
			return;
		}
		ok("첫 접속 " + first + "ms");
		try {
			System.out.println("  SQL Server: " + one(c, "SELECT @@VERSION").split("\n")[0].trim());
			System.out.println("  DB 이름: " + one(c, "SELECT DB_NAME()") + "  서버 시각: " + one(c, "SELECT CONVERT(VARCHAR(19), GETDATE(), 120)"));
		} catch (Exception e) {
			System.out.println("  (버전 조회 실패: " + e.getMessage() + ")");
		}

		section("2. 테이블·테스트 데이터");
		for (String t : new String[] { "jw_terminal", "jw_payment", "cybermoney", "jw_franchise", "jw_paper", "printer" }) {
			try {
				one(c, "SELECT TOP 1 1 FROM [dbo].[" + t + "]");
				ok("표 " + t + " 있음");
			} catch (Exception e) {
				bad("표 " + t + " 읽기 실패: " + e.getMessage());
			}
		}
		check(c, "테스트 단말기 " + terminalID + " / " + serial + " 가 jw_terminal 에 있음", "SELECT 1 FROM [dbo].[jw_terminal] WHERE [terminal_id] = ? AND [serial] = ?", terminalID, serial);
		check(c, "테스트 단말기의 용지 가격이 jw_paper 에 있음 (없으면 가격 조회는 60원)", "SELECT TOP 1 1 FROM jw_paper WHERE terminalID = ?", terminalID);
		check(c, "테스트 카드 " + userCard + " 가 cybermoney 에 있음", "SELECT 1 FROM [dbo].[cybermoney] WHERE [cardNumber] = ?", userCard);
		pool.freeConnection(c);

		section("3. 쿼리별 응답 시간 (한 번에 1개씩, 20번 평균)");
		for (String[] q : QUERIES) {
			long[] lat = new long[20];
			String err = null;
			for (int i = 0; i < lat.length && err == null; i++) {
				long s = System.nanoTime();
				err = runQuery(pool, q, i);
				lat[i] = System.nanoTime() - s;
			}
			if (err != null) {
				bad(q[0] + " 실패: " + err);
				continue;
			}
			Arrays.sort(lat);
			double avg = 0;
			for (long x : lat) avg += x;
			avg = avg / lat.length / 1e6;
			String warn = avg > 100 ? "   ← 느림: 인덱스 확인 필요" : "";
			ok(String.format("%-28s 평균 %6.1fms  최대 %6.1fms%s", q[0], avg, lat[lat.length - 1] / 1e6, warn));
		}

		section("4. 동시 부하 (동시 " + threads + ", " + requests + "건, 위 쿼리를 섞어서)");
		final AtomicInteger next = new AtomicInteger(), okCount = new AtomicInteger(), errCount = new AtomicInteger();
		final long[] lat = new long[requests];
		final List<String> errors = new ArrayList<String>();
		final DBCPConnectionMgr p = pool;
		ExecutorService ex = Executors.newFixedThreadPool(threads);
		final CountDownLatch start = new CountDownLatch(1);
		final AtomicInteger peak = new AtomicInteger();
		Thread watcher = new Thread(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					int active = activeConnections(p);
					if (active > peak.get()) peak.set(active);
					try { Thread.sleep(20); } catch (InterruptedException e) { return; }
				}
			}
		});
		watcher.setDaemon(true);
		watcher.start();
		t0 = System.nanoTime();
		for (int t = 0; t < threads; t++) {
			ex.execute(new Runnable() {
				public void run() {
					try { start.await(); } catch (InterruptedException e) { return; }
					int n;
					while ((n = next.getAndIncrement()) < requests) {
						long s = System.nanoTime();
						String err = runQuery(p, QUERIES[n % QUERIES.length], n);
						lat[n] = System.nanoTime() - s;
						if (err == null) okCount.incrementAndGet();
						else {
							errCount.incrementAndGet();
							synchronized (errors) { if (errors.size() < 5) errors.add(err); }
						}
					}
				}
			});
		}
		start.countDown();
		ex.shutdown();
		ex.awaitTermination(30, TimeUnit.MINUTES);
		double sec = (System.nanoTime() - t0) / 1e9;
		watcher.interrupt();
		Arrays.sort(lat);
		System.out.println(String.format("  성공 %d  실패 %d  초당 %.0f건  50%% %.1fms  95%% %.1fms  99%% %.1fms  최대 %.1fms  (%.1f초)",
				okCount.get(), errCount.get(), requests / sec, StressTest.Result.pct(lat, 0.5), StressTest.Result.pct(lat, 0.95),
				StressTest.Result.pct(lat, 0.99), lat[lat.length - 1] / 1e6, sec));
		for (String e : errors) System.out.println("    실패 예: " + e);
		if (errCount.get() == 0) ok("동시 부하 오류 없음");
		else bad("동시 부하 중 실패 " + errCount.get() + "건");

		section("5. 커넥션 풀");
		System.out.println("  부하 중 최대 동시 사용 커넥션 " + peak.get() + "개 (풀 최대 " + DbConfig.getPoolMaxActive() + ")");
		Thread.sleep(500);
		String status = pool.poolStatus();
		System.out.println("  끝난 뒤: " + status);
		if (status.startsWith("사용 0 ")) ok("반납 안 된 커넥션 없음");
		else bad("끝난 뒤에도 사용 중인 커넥션이 있음 (반납 누락 의심)");

		finish();
	}

	/** 쿼리 하나 실행. 성공하면 null, 실패하면 오류 메시지 */
	static String runQuery(DBCPConnectionMgr pool, String[] q, int n) {
		Connection c = pool.getConnection();
		if (c == null) return "커넥션을 빌리지 못함 (" + pool.poolStatus() + ")";
		try (PreparedStatement ps = c.prepareStatement(q[1])) {
			for (int i = 2; i < q.length; i++) {
				String v = q[i].equals("T") ? terminalID : q[i].equals("S") ? serial : q[i].equals("C") ? userCard
						: q[i].equals("N") ? "DBSTRESS" + n : "DBSTRESS" + n;
				ps.setString(i - 1, v);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					// 읽기만
				}
			}
			return null;
		} catch (Exception e) {
			return e.toString();
		} finally {
			pool.freeConnection(c);
		}
	}

	static int activeConnections(DBCPConnectionMgr pool) {
		String s = pool.poolStatus(); // "사용 n / 쉬는 중 n / ..."
		try {
			return Integer.parseInt(s.replaceAll("^사용 (\\d+).*$", "$1"));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static String one(Connection c, String sql) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			return rs.next() ? String.valueOf(rs.getString(1)) : "";
		}
	}

	static void check(Connection c, String name, String sql, String... params) {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) ok(name);
				else System.out.println("  ! " + name + " → 없음");
			}
		} catch (Exception e) {
			bad(name + " 확인 실패: " + e.getMessage());
		}
	}

	/** 접속 주소에서 서버·포트·DB 이름만 보여 준다 (계정·비밀번호 옵션은 숨김) */
	static String maskUrl(String url) {
		if (url == null) return "(없음)";
		return url.replaceAll("(?i);(password|user|username)=[^;]*", ";$1=…");
	}

	static void section(String title) {
		System.out.println();
		System.out.println("[" + title + "]");
	}

	static void ok(String m) {
		System.out.println("  ✓ " + m);
	}

	static void bad(String m) {
		fail++;
		System.out.println("  ✗ " + m);
	}

	static void finish() {
		System.out.println();
		System.out.println(fail == 0 ? "DB 테스트 이상 없음" : "DB 테스트 실패 " + fail + "건");
		System.exit(fail == 0 ? 0 : 1);
	}
}
