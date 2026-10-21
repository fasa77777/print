package com.jetwork.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.jetwork.bin.JetworkServerApplication;
import com.jetwork.bin.ConnectionPool;
import com.jetwork.test.fake.FakeDbDriver;

/**
 * 메모리 누수 시험 (가짜 DB, 실제 DB·서버에 접속하지 않음).
 *
 * 서버를 띄우고 여러 API 를 섞어 수십만 건을 보낸 뒤, 회차마다 GC 후 남은 힙·스레드·열린 파일·DB 커넥션을 잰다.
 * 누수가 있으면 회차가 지날수록 계속 늘어난다. 처음 몇 회차는 JIT·캐시가 채워지며 늘 수 있으므로 뒷 회차끼리 비교한다.
 *
 * 실행: java -Xmx256m -cp ... com.jetwork.test.MemorySoakTest [회차 수(기본 12)] [회차당 요청 수(기본 20000)] [trace]
 *   trace 를 붙이면 전체 로그(jetwork-all=debug)를 켜고 시험한다.
 */
public class MemorySoakTest {

	public static void main(String[] args) throws Exception {
		int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 12;
		int perRound = args.length > 1 ? Integer.parseInt(args[1]) : 20000;
		boolean trace = args.length > 2 && args[2].equals("trace");
		int port = 18092;

		File dir = new File(System.getProperty("java.io.tmpdir"), "jetwork-soak-test");
		dir.mkdirs();
		File dbConfig = new File(dir, "db.properties");
		OutputStream os = new FileOutputStream(dbConfig);
		os.write(("db.driver=" + FakeDbDriver.class.getName() + "\ndb.url=jdbc:fakedb:soak\ndb.username=test\ndb.password=test\n").getBytes(StandardCharsets.UTF_8));
		os.close();
		Class.forName(FakeDbDriver.class.getName());
		FakeDbDriver.logging = false;
		System.setProperty("db.config", dbConfig.getAbsolutePath());
		System.setProperty("socket.server", "false");

		File log = new File(dir, "server.log");
		log.delete();
		List<String> a = new ArrayList<String>();
		a.add("--server.port=" + port);
		a.add("--spring.main.banner-mode=off");
		a.add("--logging.file.name=" + log.getAbsolutePath());
		a.add("--logging.pattern.console=");
		a.add(trace ? "--logging.level.jetwork-all=debug" : "--logging.level.root=warn");
		JetworkServerApplication.main(a.toArray(new String[0]));

		StressTest.baseUrl = "http://127.0.0.1:" + port + "/jetwork";
		StressTest.terminalID = "T1";
		StressTest.serial = "S1";
		StressTest.userCard = "C1";
		FakeDbDriver.addRow("cybermoney", "cardNumber", "SOAK", "cyberMoney", "1000000000", "fran_id", "F1");

		System.out.println("메모리 누수 시험: " + rounds + "회 × " + perRound + "건, 최대 힙 "
				+ Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB, 전체 로그 " + (trace ? "켬" : "끔"));
		System.out.println("회차   누적요청   실패   남은힙(MB)  스레드  열린파일  DB커넥션(사용/전체)");
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		long[] heap = new long[rounds];
		int[] threads = new int[rounds];
		long[] fds = new long[rounds];
		long errors = 0;
		for (int i = 0; i < rounds; i++) {
			final int round = i;
			StressTest.Request req = StressTest.mixed(StressTest.terminal(), StressTest.price(), StressTest.balance(),
					StressTest.cardPayment("SOAK" + i + "-"), new StressTest.Request() {
						public String path(int n) { return "/postCyberMoneyPayment"; }
						public String body(int n) { return OfflineStressTest.cyber("SOAK", "1", "R" + round + "-" + n); }
					});
			StressTest.Result r = StressTest.run("회차 " + (i + 1), 50, perRound, req);
			errors += r.err;
			// 가짜 DB 의 결제 표가 커지는 것은 서버 메모리가 아니므로 비운다
			FakeDbDriver.clear("jw_payment");
			Thread.sleep(500);
			for (int g = 0; g < 3; g++) {
				System.gc();
				Thread.sleep(200);
			}
			heap[i] = mem.getHeapMemoryUsage().getUsed();
			threads[i] = Thread.activeCount();
			fds[i] = OfflineStressTest.openFiles();
			System.out.printf("%3d  %9d  %5d  %9.1f  %6d  %8d  %s%n", i + 1, (long) (i + 1) * perRound, r.err, heap[i] / 1048576.0,
					threads[i], fds[i], ConnectionPool.getInst().poolStatus());
		}

		// 판정: 뒤 절반의 첫 회차와 마지막 회차 비교
		int from = rounds / 2;
		double grow = (heap[rounds - 1] - heap[from]) / 1048576.0;
		long reqs = (long) (rounds - 1 - from) * perRound;
		System.out.println();
		System.out.printf("뒤 %d회차(%d건) 동안 남은 힙 변화 %+.1fMB (1만 건당 %+.2fMB), 스레드 %d→%d, 열린 파일 %d→%d, 실패 %d건%n",
				rounds - from, reqs, grow, reqs == 0 ? 0 : grow / (reqs / 10000.0), threads[from], threads[rounds - 1], fds[from],
				fds[rounds - 1], errors);
		boolean ok = grow < 5 && threads[rounds - 1] <= threads[from] + 5 && fds[rounds - 1] <= fds[from] + 20 && errors == 0;
		System.out.println(ok ? "결과: 누수 없음" : "결과: 누수 의심 — 위 숫자를 확인하세요");
		System.exit(ok ? 0 : 1);
	}
}
