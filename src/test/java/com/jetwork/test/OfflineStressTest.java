package com.jetwork.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.JetworkServerApplication;
import com.jetwork.test.fake.FakeDbDriver;

/**
 * DB 없이 돌리는 스트레스 테스트.
 *
 * 실행 방법 (이클립스): 이 파일 우클릭 → Run As → Java Application (1~2분)
 *
 * 가짜 DB(쿼리마다 3ms 지연)에 실제 서버를 붙여(HTTP 18091) 아래를 확인한다.
 *   1. 읽기 부하        : 동시 100, 10,000건 (단말기 확인·가격·잔액)       → 실패 0
 *   2. 결제 기록 부하   : 동시 200, 5,000건 (승인번호 모두 다름)          → 5,000건 모두 기록
 *   3. 같은 승인번호 경합: 동시 100, 같은 승인번호 100건                  → 1건만 기록
 *   4. 사이버머니 경합  : 잔액 30,000원 카드에 동시 200, 100원 400건      → 300건 성공·100건 잔액 부족·잔액 0원
 *   5. 같은 결제번호 연속 결제: 잔액 1,000원 카드에 같은 cyberNo 100원 50건 동시 → 10건 성공·잔액 0원 (음수 안 됨)
 *      같은 카드로 1초 안에 차례로 5건                                → 5건 모두 성공
 *   6. DB 장애          : 부하 중 3초 동안 DB 끊김                       → 끊긴 동안만 실패, 복구 후 재시작 없이 정상
 *   7. 자원             : 커넥션 최대 200 이하, 끝난 뒤 사용 중 커넥션 0, 스레드·파일(소켓) 수가 늘지 않음
 *
 * 처리량 숫자는 가짜 DB 기준이라 실제와 다르다. 실제 DB 는 DbStressTest, 실제 서버는 StressTest 로 잰다.
 */
public class OfflineStressTest {

	static int pass = 0;
	static int fail = 0;
	static final List<String> failures = new ArrayList<String>();
	static final List<StressTest.Result> results = new ArrayList<StressTest.Result>();

	public static void main(String[] args) throws Exception {
		int port = 18091;
		File dir = new File(System.getProperty("java.io.tmpdir"), "jetwork-stress-test");
		dir.mkdirs();
		File dbConfig = new File(dir, "db.properties");
		OutputStream os = new FileOutputStream(dbConfig);
		os.write(("db.driver=" + FakeDbDriver.class.getName() + "\ndb.url=jdbc:fakedb:stress\ndb.username=test\ndb.password=test\n").getBytes(StandardCharsets.UTF_8));
		os.close();
		Class.forName(FakeDbDriver.class.getName());
		FakeDbDriver.logging = false;
		FakeDbDriver.queryDelayMillis = 3;
		System.setProperty("db.config", dbConfig.getAbsolutePath());
		System.setProperty("socket.server", "false");

		System.out.println("서버 시작 중 (가짜 DB, 쿼리 3ms, HTTP " + port + ") ...");
		JetworkServerApplication.main(new String[] { "--server.port=" + port, "--spring.main.banner-mode=off",
				"--logging.file.name=" + new File(dir, "server.log").getAbsolutePath(), "--logging.level.root=error",
				"--logging.pattern.console=" }); // DB 장애 시험 중 서버 오류 로그는 파일에만 (콘솔에는 결과만)
		System.out.println("서버 로그: " + new File(dir, "server.log").getAbsolutePath());
		StressTest.baseUrl = "http://127.0.0.1:" + port + "/jetwork";
		StressTest.terminalID = "T1";
		StressTest.serial = "S1";
		StressTest.userCard = "C1";
		StressTest.run("준비", 4, 200, StressTest.terminal());
		long fdsBefore = openFiles();
		System.out.println("==========================================================================");

		// 1. 읽기 부하
		section("1. 읽기 부하");
		StressTest.Result r = keep(StressTest.run("읽기 섞어서 (동시 100)", 100, 10000,
				StressTest.mixed(StressTest.terminal(), StressTest.price(), StressTest.balance())));
		check("10,000건 실패 0", r.err == 0 && r.ok == 10000);

		// 2. 결제 기록 부하
		section("2. 카드 결제 기록 부하");
		int before = payments();
		r = keep(StressTest.run("카드 결제 (동시 200)", 200, 5000, StressTest.cardPayment("BULK")));
		check("5,000건 응답 OK", r.err == 0);
		check("jw_payment 에 정확히 5,000건 기록 (실제 " + (payments() - before) + ")", payments() - before == 5000);

		// 3. 같은 승인번호 경합
		section("3. 같은 승인번호 동시 요청");
		before = payments();
		r = keep(StressTest.run("같은 승인번호 100건 (동시 100)", 100, 100, StressTest.fixed("/postCraditPayment",
				"{\"terminalID\":\"T1\",\"totalAmount\":\"100\",\"paperSize\":\"A4\",\"color\":\"false\",\"jobType\":\"print\",\"totalPages\":\"1\","
						+ "\"R09\":\"SAME0001\",\"R11\":\"N\",\"R12\":\"029\",\"R33\":\"\"}")));
		check("1건만 기록 (실제 " + (payments() - before) + ")", payments() - before == 1);

		// 4. 사이버머니 잔액 경합
		section("4. 사이버머니 동시 결제 (잔액 30,000원, 100원 × 400건)");
		FakeDbDriver.addRow("cybermoney", "cardNumber", "SC1", "cyberMoney", "30000", "fran_id", "F1");
		final AtomicInteger okPay = new AtomicInteger(), noMoney = new AtomicInteger();
		before = payments();
		r = keep(StressTest.run("사이버머니 400건 (동시 200)", 200, 400, new StressTest.Request() {
			public String path(int n) { return "/postCyberMoneyPayment"; }
			public String body(int n) { return cyber("SC1", "100", "RACE" + n); }
		}, new StressTest.Check() {
			public boolean ok(int st, String body) {
				if (body.contains("\"reason\":\"ok\"")) okPay.incrementAndGet();
				else if (body.contains("\"reason\":\"no money\"")) noMoney.incrementAndGet();
				return st == 200 && (body.contains("\"reason\":\"ok\"") || body.contains("\"reason\":\"no money\""));
			}
		}));
		String bal = balance("SC1");
		check("성공 300·잔액 부족 100 (실제 " + okPay.get() + "·" + noMoney.get() + ")", okPay.get() == 300 && noMoney.get() == 100);
		check("남은 잔액 0원, 음수 아님 (실제 " + bal + ")", "0".equals(bal));
		check("결제 기록 300건 (실제 " + (payments() - before) + ")", payments() - before == 300);

		// 5. 같은 결제번호 연속 결제 (매장 에이전트는 결제번호를 초 단위로 만들어 1초 안의 연속 결제는 번호가 같다)
		section("5. 같은 cyberNo 연속 결제 동시 50건 (잔액 1,000원, 100원씩)");
		FakeDbDriver.addRow("cybermoney", "cardNumber", "SC2", "cyberMoney", "1000", "fran_id", "F1");
		final AtomicInteger okSame = new AtomicInteger(), noMoneySame = new AtomicInteger();
		before = payments();
		r = keep(StressTest.run("같은 cyberNo 50건 (동시 50)", 50, 50, StressTest.fixed("/postCyberMoneyPayment", cyber("SC2", "100", "20261021134500")),
				new StressTest.Check() {
					public boolean ok(int st, String body) {
						if (body.contains("\"reason\":\"ok\"")) okSame.incrementAndGet();
						else if (body.contains("\"reason\":\"no money\"")) noMoneySame.incrementAndGet();
						return st == 200 && (body.contains("\"reason\":\"ok\"") || body.contains("\"reason\":\"no money\""));
					}
				}));
		bal = balance("SC2");
		check("잔액만큼 10건 성공·40건 잔액 부족 (실제 " + okSame.get() + "·" + noMoneySame.get() + ")", okSame.get() == 10 && noMoneySame.get() == 40);
		check("잔액 0원, 음수 아님 (실제 " + bal + ")", "0".equals(bal));
		check("결제 기록 10건 = 차감 10번 (실제 " + (payments() - before) + ")", payments() - before == 10);

		// 5-1. 같은 카드 연속 결제 (차례로 빠르게)
		section("5-1. 같은 카드로 1초 안에 연속 결제 5건 (한 건씩 차례로)");
		FakeDbDriver.addRow("cybermoney", "cardNumber", "SC3", "cyberMoney", "500", "fran_id", "F1");
		int ok5 = 0;
		for (int i = 0; i < 5; i++) {
			OfflineTest.base = StressTest.baseUrl;
			Map<String, String> m = OfflineTest.postRaw("/postCyberMoneyPayment", cyber("SC3", "100", "20261021134501"));
			if ("ok".equals(m.get("reason"))) ok5++;
		}
		check("5건 모두 성공, 잔액 0원 (실제 " + ok5 + "건, 잔액 " + balance("SC3") + ")", ok5 == 5 && "0".equals(balance("SC3")));

		// 6. DB 장애
		section("6. 부하 중 DB 장애 3초");
		outage();

		// 7. 자원
		section("7. 자원");
		System.out.println("  가짜 DB 실제 커넥션: 최대 동시 " + FakeDbDriver.maxOpenConnections.get() + "개, 지금 열림 " + FakeDbDriver.openConnections.get()
				+ "개, 만든 총수 " + FakeDbDriver.createdConnections.get() + "개, 실행 쿼리 " + FakeDbDriver.statements.get() + "개");
		check("동시 커넥션이 풀 최대 200 이하", FakeDbDriver.maxOpenConnections.get() <= 200);
		Thread.sleep(1000);
		String status = ConnectionPool.getInst().poolStatus();
		check("끝난 뒤 사용 중 커넥션 0 (" + status + ")", status.startsWith("사용 0 "));
		Thread.sleep(3000);
		// Tomcat 은 부하가 오면 작업 스레드를 최대 200개까지 늘려 두고(1분 쉬면 줄임), 커넥션 풀·서버 기본 스레드가 수십 개 있다.
		// 따라서 "그 이상으로 늘지 않는지", "멈춘 뒤 더 늘지 않는지"를 본다.
		int threadsAfter = Thread.activeCount();
		Thread.sleep(3000);
		int threadsLater = Thread.activeCount();
		long fdsAfter = openFiles();
		System.out.println("  스레드 끝 " + threadsAfter + " → 3초 뒤 " + threadsLater + (fdsBefore >= 0 ? ",  열린 파일(소켓) " + fdsBefore + " → " + fdsAfter : ""));
		check("스레드 수가 Tomcat 최대(200) + 기본 스레드 범위 안 (250 이하), 부하가 끝난 뒤 더 늘지 않음", threadsAfter <= 250 && threadsLater <= threadsAfter);
		if (fdsBefore >= 0) check("열린 파일(소켓)이 계속 늘지 않음 (+300 이내)", fdsAfter - fdsBefore <= 300);

		System.out.println();
		System.out.println("==========================================================================");
		System.out.println(StressTest.Result.header());
		for (StressTest.Result x : results) System.out.println(x.line());
		System.out.println("(처리량은 가짜 DB 기준: 모든 쿼리를 한 줄로 세워 처리하므로 실제 DB 보다 낮게 나온다)");
		System.out.println();
		System.out.println("통과 " + pass + "  실패 " + fail);
		for (String f : failures) System.out.println("  ✗ " + f);
		System.exit(fail == 0 ? 0 : 1);
	}

	/** 50개 스레드가 계속 요청을 보내는 동안 3초 뒤 DB 를 3초 끊고, 초마다 성공·실패를 센다 */
	static void outage() throws Exception {
		final AtomicBoolean stop = new AtomicBoolean();
		final long t0 = System.currentTimeMillis();
		final int seconds = 12;
		final AtomicInteger[] ok = new AtomicInteger[seconds + 2], err = new AtomicInteger[seconds + 2];
		for (int i = 0; i < ok.length; i++) { ok[i] = new AtomicInteger(); err[i] = new AtomicInteger(); }
		final AtomicLong maxMs = new AtomicLong();
		OfflineTest.base = StressTest.baseUrl;
		List<Thread> ts = new ArrayList<Thread>();
		for (int t = 0; t < 50; t++) {
			Thread th = new Thread(new Runnable() {
				public void run() {
					while (!stop.get()) {
						long s = System.currentTimeMillis();
						boolean good;
						try {
							// 잔액 조회: DB 오류면 money 가 빈 값으로 온다
							Map<String, String> m = OfflineTest.postRaw("/postCyberMoney", "{\"userID\":\"T1\",\"userCard\":\"C1\"}");
							good = m.get("money") != null && !m.get("money").isEmpty() && !"null".equals(m.get("money"));
						} catch (Exception e) {
							good = false;
						}
						long e = System.currentTimeMillis();
						int sec = (int) ((e - t0) / 1000);
						if (sec < ok.length) (good ? ok[sec] : err[sec]).incrementAndGet();
						if (e - s > maxMs.get()) maxMs.set(e - s);
					}
				}
			});
			th.start();
			ts.add(th);
		}
		Thread.sleep(3000);
		FakeDbDriver.down = true;
		System.out.println("  3초: DB 끊김");
		Thread.sleep(3000);
		FakeDbDriver.down = false;
		System.out.println("  6초: DB 복구");
		Thread.sleep((seconds - 6) * 1000L);
		stop.set(true);
		for (Thread th : ts) th.join();
		StringBuilder sb = new StringBuilder("  초별 성공/실패: ");
		int okBefore = 0, errBefore = 0, okAfter = 0, errAfter = 0, errDuring = 0;
		for (int i = 0; i < seconds; i++) {
			sb.append(i).append("s ").append(ok[i].get()).append("/").append(err[i].get()).append("  ");
			if (i < 3) { okBefore += ok[i].get(); errBefore += err[i].get(); }
			else if (i < 8) errDuring += err[i].get();
			else { okAfter += ok[i].get(); errAfter += err[i].get(); }
		}
		System.out.println(sb);
		System.out.println("  가장 오래 걸린 요청 " + maxMs.get() + "ms");
		check("장애 전 실패 0 (성공 " + okBefore + ")", errBefore == 0 && okBefore > 0);
		check("장애 중에는 실패로 응답 (" + errDuring + "건)", errDuring > 0);
		check("복구 2초 뒤부터 실패 0, 재시작 없이 정상 (성공 " + okAfter + ", 실패 " + errAfter + ")", errAfter == 0 && okAfter > 0);
		check("멈춰 있는 요청 없음: 가장 긴 요청 15초 미만", maxMs.get() < 15000);
	}

	static String cyber(String card, String amount, String cyberNo) {
		return "{\"terminalID\":\"T1\",\"userCard\":\"" + card + "\",\"totalAmount\":\"" + amount + "\",\"cyberNo\":\"" + cyberNo
				+ "\",\"paperSize\":\"A4\",\"color\":\"false\",\"jobType\":\"print\",\"totalPages\":\"1\"}";
	}

	static StressTest.Result keep(StressTest.Result r) {
		results.add(r);
		return r;
	}

	static int payments() {
		return FakeDbDriver.rows("jw_payment").size();
	}

	static String balance(String card) {
		Map<String, String> r = FakeDbDriver.find("cybermoney", "cardNumber", card);
		return r == null ? null : r.get("cyberMoney");
	}

	/** 열린 파일 수 (리눅스만, 그 밖에는 -1) */
	static long openFiles() {
		File fd = new File("/proc/self/fd");
		String[] list = fd.list();
		return list == null ? -1 : list.length;
	}

	static void section(String title) {
		System.out.println();
		System.out.println("[" + title + "]");
	}

	static void check(String name, boolean ok) {
		if (ok) {
			pass++;
			System.out.println("  ✓ " + name);
		} else {
			fail++;
			failures.add(name);
			System.out.println("  ✗ " + name);
		}
	}
}
