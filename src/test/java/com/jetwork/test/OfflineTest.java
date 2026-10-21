package com.jetwork.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jetwork.bin.JetworkServerApplication;
import com.jetwork.test.fake.FakeDbDriver;
import com.jetwork.test.fake.FakeRelay;
import com.jetwork.test.fake.FakeTerminal;

/**
 * DB 없이 돌리는 자동 테스트 프로그램.
 *
 * 실행 방법 (이클립스)
 *   이 파일 우클릭 → Run As → Java Application. 끝나면 콘솔에 "통과 n  실패 n" 이 나온다.
 *
 * 하는 일
 *   1. 메모리 위의 가짜 DB(FakeDbDriver), 가짜 중계 서버(FakeRelay), 가짜 결제 단말기(FakeTerminal)를 띄우고
 *   2. 실제 서버(JetworkServerApplication)를 그 가짜들에 붙여서 18090 포트로 띄운 뒤
 *   3. 모든 HTTP API 를 호출해 응답과 DB 기록을 확인하고
 *   4. 단말기 소켓(10020)으로 가격 정책·카드 승인·사이버머니 전문과 Xerox 결제 흐름을 확인한다.
 *
 * 실제 DB·중계 서버·단말기에는 접속하지 않는다. 실제 DB 로 확인하려면 PaymentTestClient 를 쓴다.
 *
 * 주의
 *   - 10020, 10000 포트를 쓰는 프로그램(실제 서버, 결제 단말기 프로그램)이 같은 PC 에서 돌고 있으면 소켓 테스트는 건너뛴다.
 *   - HTTP 포트는 18090 (인수 port=번호 로 바꿀 수 있음). 실제 서버의 8090 과 겹치지 않는다.
 */
public class OfflineTest {

	static int pass = 0;
	static int fail = 0;
	static final List<String> failures = new ArrayList<String>();
	static String base;

	public static void main(String[] args) throws Exception {
		int port = 18090;
		for (String a : args) {
			if (a.startsWith("port=")) port = Integer.parseInt(a.substring(5));
		}
		boolean socketTest = free(FakeTerminal.SERVER_SOCKET_PORT) && free(FakeTerminal.PAY_PORT);
		if (!free(port)) {
			System.out.println(port + " 포트를 이미 쓰고 있습니다. port=다른번호 로 실행하세요.");
			return;
		}

		// 1. 가짜 DB·중계 서버 준비, 서버 설정
		File dir = new File(System.getProperty("java.io.tmpdir"), "jetwork-offline-test");
		dir.mkdirs();
		File dbConfig = new File(dir, "db.properties");
		OutputStream os = new FileOutputStream(dbConfig);
		os.write(("db.driver=" + FakeDbDriver.class.getName() + "\ndb.url=jdbc:fakedb:test\ndb.username=test\ndb.password=test\n")
				.getBytes(StandardCharsets.UTF_8));
		os.close();
		FakeRelay relay = new FakeRelay();
		Class.forName(FakeDbDriver.class.getName());
		System.setProperty("db.config", dbConfig.getAbsolutePath());
		System.setProperty("relay.baseUrl", "http://127.0.0.1:" + relay.port() + "/jetwork");
		System.setProperty("socket.server", String.valueOf(socketTest));

		// 2. 서버 시작
		System.out.println("서버 시작 중 (가짜 DB, HTTP " + port + (socketTest ? ", 소켓 10020" : ", 소켓 테스트 건너뜀") + ") ...");
		JetworkServerApplication.main(new String[] { "--server.port=" + port, "--spring.main.banner-mode=off",
				"--logging.file.name=" + new File(dir, "server.log").getAbsolutePath(), "--logging.level.root=error" });
		base = "http://127.0.0.1:" + port + "/jetwork";
		System.out.println("서버 로그: " + new File(dir, "server.log").getAbsolutePath());
		System.out.println("==========================================================================");

		try {
			httpTests();
			if (socketTest) {
				socketTests(relay);
			} else {
				System.out.println();
				System.out.println("[소켓·Xerox] 10020 또는 10000 포트를 다른 프로그램이 쓰고 있어 건너뜀");
			}
		} catch (Throwable t) {
			bad("테스트 중 예외: " + t);
			t.printStackTrace();
		}

		System.out.println("==========================================================================");
		System.out.println("통과 " + pass + "  실패 " + fail);
		for (String f : failures) {
			System.out.println("  ✗ " + f);
		}
		relay.close();
		System.exit(fail == 0 ? 0 : 1);
	}

	// ---------------- HTTP API ----------------

	static void httpTests() throws Exception {
		section("1. 단말기");
		Map<String, String> r = post("/postTerminal", "terminalID", "T1", "serial", "S1", "agentVersion", "A1", "terminalVersion", "TV1");
		eq("등록 단말기 → OK", r, "result", "OK");
		Map<String, String> t = FakeDbDriver.find("jw_terminal", "terminal_id", "T1");
		check("버전 갱신 (agent_version=A1, terminalVersion=TV1)", "A1".equals(t.get("agent_version")) && "TV1".equals(t.get("terminalVersion")));
		eq("시리얼 틀림 → FALSE", post("/postTerminal", "terminalID", "T1", "serial", "XX"), "result", "FALSE");
		r = post("/printnuri/pricepolicy", "terminalID", "T1", "agentVersion", "A2", "version", "V2");
		check("가격 정책 → 컬러 200 / 흑백 100", "OK".equals(r.get("result")) && "200".equals(r.get("a4color")) && "100".equals(r.get("a4mono")));
		check("가격 정책 요청 때 terminalVersion=V2 갱신", "V2".equals(FakeDbDriver.find("jw_terminal", "terminal_id", "T1").get("terminalVersion")));
		r = post("/printnuri/pricepolicy", "terminalID", "T9", "agentVersion", "PRT_1.11.01");
		check("가격 정보 없는 단말기 T9 → 컬러 60 / 흑백 60", "OK".equals(r.get("result")) && "60".equals(r.get("a4color")) && "60".equals(r.get("a4mono")));
		FakeDbDriver.addRow("jw_paper", "terminalID", "T2", "paper_colorYN", "Y", "paper_price", " 300 ", "paper_size", "A4");
		r = post("/printnuri/pricepolicy", "terminalID", "T2", "agentVersion", "PRT_1.11.01");
		check("컬러 가격만 있는 단말기 T2 → 컬러 300 (공백 제거) / 흑백 60", "300".equals(r.get("a4color")) && "60".equals(r.get("a4mono")));

		section("2. 사이버머니");
		eq("잔액 조회 C1 → 1000", post("/postCyberMoney", "userID", "T1", "userCard", "C1"), "money", "1000");
		eq("처음 보는 카드 C9 → 0", post("/postCyberMoney", "userID", "T1", "userCard", "C9"), "money", "0");
		check("C9 를 0원으로 등록", FakeDbDriver.find("cybermoney", "cardNumber", "C9") != null);

		r = cyber("/postCyberMoneyPayment", "C1", "300", "N1", "false");
		check("C1 300원 결제 → OK, 잔액 700", "OK".equals(r.get("result")) && "700".equals(r.get("balance")));
		check("cybermoney 잔액 700", "700".equals(balance("C1")));
		Map<String, String> p = FakeDbDriver.find("jw_payment", "cyberNo", "N1");
		check("결제 기록 (사이버머니, 가맹점명, payment_ok=cyberNo)", p != null && "사이버머니".equals(p.get("payment_type"))
				&& "홍대점".equals(p.get("payment_cardName")) && "N1".equals(p.get("payment_ok")));
		check("단말기 last_dt 갱신", "<now>".equals(FakeDbDriver.find("jw_terminal", "terminal_id", "T1").get("last_dt")));

		r = cyber("/postCyberMoneyPayment", "C1", "300", "N1", "false");
		check("같은 결제번호 다시 → FAIL duplicate, 차감 없음", "duplicate".equals(r.get("reason")) && "700".equals(balance("C1")));
		r = cyber("/postCyberMoneyPayment", "C2", "100", "N2", "false");
		check("잔액 100원에 100원 결제 → OK, 잔액 0", "OK".equals(r.get("result")) && "0".equals(balance("C2")));
		r = cyber("/postCyberMoneyPayment", "C1", "5000", "N3", "false");
		check("잔액 부족 → FAIL no money, balance -4300, 차감 없음", "no money".equals(r.get("reason")) && "-4300".equals(r.get("balance")) && "700".equals(balance("C1")));
		r = cyber("/postCyberMoneyPayment", "C8", "100", "N4", "false");
		check("미등록 카드 → FAIL no card, 0원 등록", "no card".equals(r.get("reason")) && "0".equals(balance("C8")));
		r = cyber("/postCyberMoneyPayment", "", "100", "N5", "false");
		check("빈 카드번호 → FAIL no card, 등록 안 함", "no card".equals(r.get("reason")) && FakeDbDriver.find("cybermoney", "cardNumber", "") == null);
		r = cyber("/postCyberMoneyPayment", "C1", "abc", "N6", "false");
		check("금액이 숫자가 아님 → FAIL error", "error".equals(r.get("reason")));

		r = cyber("/kyocera/postCyberMoneyPayment", "C1", "200", "K1", "1");
		check("교세라 200원 → OK, 잔액 500", "OK".equals(r.get("result")) && "500".equals(balance("C1")));
		p = FakeDbDriver.find("jw_payment", "cyberNo", "K1");
		check("교세라 컬러 1 → true 로 기록", p != null && "true".equals(p.get("payment_colorYN")));

		section("3. 카드·교통카드");
		int before = payments();
		eq("신용카드 신한 → OK", card("/postCraditPayment", "R09", "A100", "R11", "N", "R12", "029", "R33", ""), "result", "OK");
		p = FakeDbDriver.find("jw_payment", "payment_ok", "A100");
		check("신용카드·신한 카드사로 기록", p != null && "신용카드".equals(p.get("payment_type")) && "신한 카드사".equals(p.get("payment_cardName")));
		card("/postCraditPayment", "R09", "A100", "R11", "N", "R12", "029", "R33", "");
		check("같은 승인번호 다시 → 기록 안 함", payments() == before + 1);
		card("/postCraditPayment", "R09", "A200", "R11", "Y", "R12", "999", "R33", "");
		p = FakeDbDriver.find("jw_payment", "payment_ok", "A200");
		check("모르는 카드사 코드 → 체크카드·해외카드", p != null && "체크카드".equals(p.get("payment_type")) && "해외카드".equals(p.get("payment_cardName")));
		card("/postCraditPayment", "R11", "", "R33", "TM");
		card("/postCraditPayment", "R11", "", "R33", "EB");
		card("/postCraditPayment", "R11", "", "R33", "RP");
		check("티머니·캐시비·기타 교통카드 → 티머니·캐시비·교통카드", has("티머니") && has("캐시비") && has("교통카드"));
		int b2 = payments();
		card("/postCraditPayment", "R09", "", "R11", "N", "R12", "016", "R33", "");
		card("/postCraditPayment", "R09", "", "R11", "N", "R12", "016", "R33", "");
		check("승인번호 빈 카드 결제 2건 → 2건 모두 기록", payments() == b2 + 2);
		eq("R11·R33 모두 없음 → 오류 없이 OK", card("/postCraditPayment", "R11", ""), "result", "OK");
		eq("KICC 국민 → OK", card("/kicc/postCraditPayment", "R09", "K9", "R10", "Y", "R12", "016", "R33", ""), "result", "OK");
		p = FakeDbDriver.find("jw_payment", "payment_ok", "K9");
		check("KICC 체크카드·국민 카드사로 기록", p != null && "체크카드".equals(p.get("payment_type")) && "국민 카드사".equals(p.get("payment_cardName")));
		eq("프린트누리 → OK", post("/printnuri/payment", "terminalID", "T1", "price", "500", "payOK", "P77", "craditName", "031",
				"payType", "N", "payTypeTM", "", "color", "true", "count", "3", "paperSize", "A4"), "result", "OK");
		p = FakeDbDriver.find("jw_payment", "payment_ok", "P77");
		check("프린트누리 신용카드·삼성 카드사·500원·3장", p != null && "삼성 카드사".equals(p.get("payment_cardName")) && "500".equals(p.get("payment_pay")) && "3".equals(p.get("payment_totalCount")));

		section("3-1. 실제 결제 요청 형식 (test-requests/real-card-payment.json)");
		PaymentTestClient.terminalID = "T1";
		PaymentTestClient.serial = "S1";
		String approval = PaymentTestClient.newApprovalNo();
		String real = PaymentTestClient.realPaymentJson(approval);
		r = postRaw("/postCraditPayment", real);
		eq("실제 형식(subJobData 배열·숫자·true/false 포함) → OK", r, "result", "OK");
		postRaw("/postCraditPayment", real);
		List<Map<String, String>> found = new ArrayList<Map<String, String>>();
		for (Map<String, String> row : FakeDbDriver.rows("jw_payment")) {
			if (approval.trim().equals(row.get("payment_ok"))) found.add(row);
		}
		check("같은 요청 두 번 → 1건만 기록 (승인번호 뒤 공백은 떼고 비교)", found.size() == 1);
		p = found.isEmpty() ? new java.util.HashMap<String, String>() : found.get(0);
		check("체크카드(R11=Y)·비씨 카드사(R12=026)", "체크카드".equals(p.get("payment_type")) && "비씨 카드사".equals(p.get("payment_cardName")));
		check("금액 40 (totalAmount), 매수 4 (totalPages), 용지 [A4], 컬러 true, 작업 Print",
				"40".equals(p.get("payment_pay")) && "4".equals(p.get("payment_totalCount")) && "[A4]".equals(p.get("payment_paperType"))
						&& "true".equals(p.get("payment_colorYN")) && "Print".equals(p.get("payment_printType")));

		section("4. 프린터·주소");
		eq("등록된 프린터 P1 → OK (갱신)", post("/postPrinter", "serialnumber", "P1", "devid", "D1", "sitename", "홍대"), "result", "OK");
		eq("새 프린터 P2 → FALSE (등록)", post("/postPrinter", "serialnumber", "P2", "devid", "D2", "sitename", "신촌"), "result", "FALSE");
		check("P2 등록됨", FakeDbDriver.find("printer", "serialnumber", "P2") != null);
		check("예전 주소 /stech → 404", status("http://127.0.0.1:" + base.split(":")[2].split("/")[0] + "/stech/postTerminal") == 404);
	}

	// ---------------- 단말기 소켓·Xerox ----------------

	static void socketTests(FakeRelay relay) throws Exception {
		section("5. 단말기 소켓 (10020)");
		relay.requests.clear();
		FakeTerminal.send(FakeTerminal.policyFrame("T1"));
		check("02 가격 정책 전문 → 중계 서버 /printnuri/pricepolicy (terminalID T1)",
				relay.containing("/jetwork/printnuri/pricepolicy").size() == 1 && relay.containing("\"terminalID\":\"T1\"").size() == 1);

		FakeTerminal terminal = new FakeTerminal();
		ExecutorService ex = Executors.newSingleThreadExecutor();
		try {
			section("6. Xerox 결제 (결제 단말기 10000 ↔ 소켓 10020)");
			// 카드 승인
			relay.requests.clear();
			terminal.onCharge = sendLater(FakeTerminal.frame(4, "S01=A1;S03=T1;S10=700;R04=0000;R09=X123;R11=N;R12=029;R33=;"));
			String r = xerox(ex, "700");
			check("카드 승인 → Xerox 결제 OK", "OK".equals(r));
			check("결제 단말기가 700원 결제 요청을 받음", terminal.lastCharge.contains("S10=700"));
			check("승인 결과를 중계 서버 /printnuri/payment 로 보냄 (승인번호 X123)", waitFor(relay, "\"payOK\":\"X123\"") == 1);

			// 카드 거절
			terminal.onCharge = sendLater(FakeTerminal.frame(4, "S01=A1;S03=T1;S10=700;R04=8001;"));
			check("카드 거절(R04=8001) → NO", "NO".equals(xerox(ex, "700")));
			// 취소
			terminal.onCharge = sendLater(FakeTerminal.frame(4, "S01=A1;CANCEL;"));
			check("결제 취소 → NO", "NO".equals(xerox(ex, "700")));

			// 티머니 승인 (S10 없음 → 진행 중인 작업 금액)
			relay.requests.clear();
			terminal.onCharge = sendLater(FakeTerminal.frame(4, "S03=T1;R32=X1;"));
			check("티머니 승인 → OK", "OK".equals(xerox(ex, "900")));
			check("티머니 기록에 작업 금액 900원", waitFor(relay, "\"payTypeTM\":\"TM\"") == 1 && relay.containing("\"price\":\"900\"").size() == 1);
			// 티머니 거절 (R32 첫 바이트 0xFF)
			byte[] body = "S03=T1;R32=?1;".getBytes(StandardCharsets.US_ASCII);
			body[11] = (byte) 0xFF;
			terminal.onCharge = sendLater(FakeTerminal.frame(4, body));
			check("티머니 거절 (R32 첫 바이트 0xFF) → NO", "NO".equals(xerox(ex, "900")));

			// 사이버머니 카드 태그
			relay.requests.clear();
			relay.body = "{\"result\":\"OK\"}";
			terminal.onCharge = sendLater(FakeTerminal.cardTagFrame((byte) 0x53, (byte) 0xC7, (byte) 0x97, (byte) 0xA2));
			check("사이버머니 카드 태그 → 중계 서버 승인 → OK", "OK".equals(xerox(ex, "1200")));
			check("중계 서버 /kyocera/postCyberMoneyPayment 에 1200원·카드 53C797A2",
					relay.containing("/kyocera/postCyberMoneyPayment").size() == 1 && relay.containing("\"totalAmount\":1200").size() == 1
							&& relay.containing("53C797A2").size() == 1);
			relay.body = "{\"result\":\"FAIL\",\"reason\":\"no money\"}";
			check("중계 서버가 잔액 부족(FAIL) → NO", "NO".equals(xerox(ex, "1200")));
			relay.status = 500;
			long t0 = System.currentTimeMillis();
			String r500 = xerox(ex, "1200");
			check("중계 서버 오류(500) → 기다리지 않고 바로 NO (" + (System.currentTimeMillis() - t0) + "ms)", "NO".equals(r500) && System.currentTimeMillis() - t0 < 10000);
			relay.status = 200;
			relay.body = "{\"result\":\"OK\"}";

			// 결제가 끝난 뒤 카드 태그 → 이전 금액으로 결제하지 않음
			relay.requests.clear();
			FakeTerminal.send(FakeTerminal.cardTagFrame((byte) 1, (byte) 2, (byte) 3, (byte) 4));
			check("결제가 끝난 뒤 카드 태그 → 결제 요청 안 함", relay.containing("/kyocera/").isEmpty());

			// 금액 오류
			check("Xerox 금액 abc → NO", "NO".equals(postXerox("abc")));
		} finally {
			terminal.close();
			ex.shutdownNow();
		}
		// 단말기가 응답하지 않으면 5초 안에 NO
		long t0 = System.currentTimeMillis();
		String r = postXerox("700");
		check("결제 단말기가 꺼져 있음 → NO (" + (System.currentTimeMillis() - t0) + "ms)", "NO".equals(r) && System.currentTimeMillis() - t0 < 8000);
	}

	/** 중계 서버 기록에 text 가 들어간 요청이 생길 때까지 최대 3초 기다리고 그 개수를 돌려준다 */
	static int waitFor(FakeRelay relay, String text) throws InterruptedException {
		for (int i = 0; i < 30 && relay.containing(text).isEmpty(); i++) {
			Thread.sleep(100);
		}
		return relay.containing(text).size();
	}

	static Runnable sendLater(final byte[] message) {
		return new Runnable() {
			public void run() {
				try {
					FakeTerminal.send(message);
				} catch (Exception e) {
					System.out.println("   (가짜 단말기 전송 실패: " + e + ")");
				}
			}
		};
	}

	/** Xerox 결제를 요청하고 결과(OK/NO)를 기다린다 (최대 30초) */
	static String xerox(ExecutorService ex, final String amount) throws Exception {
		Future<String> f = ex.submit(new Callable<String>() {
			public String call() {
				return postXerox(amount);
			}
		});
		return f.get(30, TimeUnit.SECONDS);
	}

	static String postXerox(String amount) {
		return post("/xerox/xeroxPayment", "terminalID", "T1", "totalAmount", amount, "color", "true", "totalPages", "3", "jobType", "print").get("result");
	}

	// ---------------- 도구 ----------------

	static Map<String, String> cyber(String path, String card, String amount, String cyberNo, String color) {
		return post(path, "terminalID", "T1", "userCard", card, "totalAmount", amount, "cyberNo", cyberNo, "paperSize", "A4",
				"color", color, "jobType", "print", "totalPages", "1");
	}

	static Map<String, String> card(String path, String... extra) {
		String[] base = { "terminalID", "T1", "totalAmount", "100", "paperSize", "A4", "color", "false", "jobType", "print", "totalPages", "1" };
		String[] all = new String[base.length + extra.length];
		System.arraycopy(base, 0, all, 0, base.length);
		System.arraycopy(extra, 0, all, base.length, extra.length);
		return post(path, all);
	}

	static String balance(String card) {
		Map<String, String> r = FakeDbDriver.find("cybermoney", "cardNumber", card);
		return r == null ? null : r.get("cyberMoney");
	}

	static int payments() {
		return FakeDbDriver.rows("jw_payment").size();
	}

	static boolean has(String paymentType) {
		for (Map<String, String> r : FakeDbDriver.rows("jw_payment")) {
			if (paymentType.equals(r.get("payment_type")) && paymentType.equals(r.get("payment_cardName"))) return true;
		}
		return false;
	}

	static Map<String, String> post(String path, String... kv) {
		String req = PaymentTestClient.toJson(PaymentTestClient.json(kv));
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(base + path).openConnection();
			con.setConnectTimeout(5000);
			con.setReadTimeout(60000);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			OutputStream os = con.getOutputStream();
			os.write(req.getBytes(StandardCharsets.UTF_8));
			os.close();
			int st = con.getResponseCode();
			InputStream is = st < 400 ? con.getInputStream() : con.getErrorStream();
			String res = is == null ? "" : PaymentTestClient.read(is);
			Map<String, String> m = PaymentTestClient.parse(res);
			m.put("_status", String.valueOf(st));
			m.put("_body", res);
			return m;
		} catch (Exception e) {
			Map<String, String> m = new java.util.LinkedHashMap<String, String>();
			m.put("_status", "ERR " + e);
			return m;
		}
	}

	static Map<String, String> postRaw(String path, String json) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(base + path).openConnection();
			con.setConnectTimeout(5000);
			con.setReadTimeout(60000);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			OutputStream os = con.getOutputStream();
			os.write(json.getBytes(StandardCharsets.UTF_8));
			os.close();
			int st = con.getResponseCode();
			InputStream is = st < 400 ? con.getInputStream() : con.getErrorStream();
			String res = is == null ? "" : PaymentTestClient.read(is);
			Map<String, String> m = PaymentTestClient.parse(res);
			m.put("_status", String.valueOf(st));
			m.put("_body", res);
			return m;
		} catch (Exception e) {
			Map<String, String> m = new java.util.LinkedHashMap<String, String>();
			m.put("_status", "ERR " + e);
			return m;
		}
	}

	static int status(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json");
			con.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));
			return con.getResponseCode();
		} catch (Exception e) {
			return -1;
		}
	}

	static boolean free(int port) {
		try {
			new ServerSocket(port, 1, InetAddress.getByName("0.0.0.0")).close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static void section(String title) {
		System.out.println();
		System.out.println("[" + title + "]");
	}

	static void eq(String name, Map<String, String> r, String key, String expected) {
		check(name + (expected.equals(r.get(key)) ? "" : "  (실제 " + key + "=" + r.get(key) + ", HTTP " + r.get("_status") + " " + r.get("_body") + ")"),
				expected.equals(r.get(key)));
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

	static void bad(String msg) {
		check(msg, false);
	}
}
