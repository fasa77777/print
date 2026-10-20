package com.jetwork.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 결제 서버 테스트 프로그램.
 *
 * 실행 방법 (이클립스)
 *   이 파일 우클릭 → Run As → Java Application
 *   값을 바꾸려면 아래 "설정" 의 상수를 고치거나, Run Configurations → Arguments 에
 *     baseUrl=http://127.0.0.1:8090/jetwork terminalID=0788888 serial=0BY8BJEH500015W userCard=53C797A2
 *   처럼 넣는다. socket=true 를 넣으면 단말기 소켓(10020) 접속 테스트도 한다.
 *   (소켓 서버는 실서버 jar 처럼 기본으로 꺼져 있다. 서버를 -Dsocket.server=true 로 실행했을 때만 테스트)
 *
 * 응답 기대값은 실서버 jar(2022-04-18 빌드) 동작 기준이다.
 *
 * 실행 방법 (명령 프롬프트, Java 11 이상)
 *   java src\test\java\com\jetwork\test\PaymentTestClient.java terminalID=0788888 serial=...
 *
 * 주의
 *   - 실제 DB 에 결제 내역이 기록되고 사이버머니 잔액이 차감된다. 테스트용 단말기·카드로만 실행한다.
 *   - 단말기 확인이 OK 가 되려면 terminalID 와 serial 이 jw_terminal 에 등록되어 있어야 한다.
 *   - Xerox 결제(/xerox/xeroxPayment)는 실제 결제 단말기가 있어야 해서 넣지 않았다.
 *   - 소켓 테스트는 02(가격 정책) 전문만 보낸다. 04·06 전문은 중계 서버에 실제 결제를 기록하므로 보내지 않는다.
 */
public class PaymentTestClient {

	// ---------------- 설정 ----------------
	static String baseUrl = "http://127.0.0.1:8090/jetwork";
	static String terminalID = "TEST0001";
	static String serial = "TESTSERIAL0001";
	static String userCard = "TESTCARD";
	static String socketHost = "127.0.0.1";
	static int socketPort = 10020;
	static boolean socketTest = false;
	// --------------------------------------

	private static int pass = 0;
	private static int fail = 0;
	private static final List<String> failures = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			int eq = arg.indexOf('=');
			if (eq < 0) {
				continue;
			}
			String k = arg.substring(0, eq);
			String v = arg.substring(eq + 1);
			if ("baseUrl".equals(k)) baseUrl = v;
			else if ("terminalID".equals(k)) terminalID = v;
			else if ("serial".equals(k)) serial = v;
			else if ("userCard".equals(k)) userCard = v;
			else if ("socket".equals(k)) socketTest = Boolean.parseBoolean(v);
			else if ("socketHost".equals(k)) socketHost = v;
		}

		System.out.println("서버 " + baseUrl + "  단말기 " + terminalID + " / " + serial + "  카드 " + userCard);
		System.out.println("==========================================================================");

		// 0. 서버 연결
		if (!serverAlive()) {
			System.out.println("서버에 연결할 수 없습니다: " + baseUrl);
			System.out.println(" - JetworkServerApplication 을 실행했는지, 콘솔에 'Tomcat started on port(s): 8090' 이 있는지 확인하세요.");
			return;
		}

		// 1. 단말기
		section("1. 단말기");
		Map<String, String> r = post("단말기 등록 확인", "/postTerminal", json(
				"terminalID", terminalID, "serial", serial, "key", "", "license", "", "agentVersion", "Xerox_2.11.01"));
		expectIn(r, "result", "OK", "FALSE");
		if ("FALSE".equals(r.get("result"))) {
			System.out.println("   ※ 미등록 단말기입니다. jw_terminal 에 terminal_id='" + terminalID + "', serial='" + serial + "' 가 있어야 OK 가 됩니다.");
		}

		r = post("가격 정책 조회", "/printnuri/pricepolicy", json("terminalID", terminalID, "agentVersion", "Xerox_2.11.01"));
		expect(r, "result", "OK");

		// 2. 사이버머니
		section("2. 사이버머니");
		r = post("잔액 조회", "/postCyberMoney", json("userID", terminalID, "userCard", userCard));
		expect(r, "result", "OK");
		String balance = r.get("money");

		String no = newNo();
		Map<String, String> pay = json("terminalID", terminalID, "userCard", userCard, "cyberNo", no, "R09", no,
				"totalAmount", "100", "paperSize", "A4", "color", "false", "jobType", "print", "totalPages", "1");
		r = post("결제 100원 (잔액 " + balance + ")", "/postCyberMoneyPayment", pay);
		expectIn(r, "result", "OK", "FAIL");
		boolean firstOk = "OK".equals(r.get("result"));
		if (!firstOk) {
			System.out.println("   ※ 결제 실패 사유: " + r.get("reason") + " (no money = 잔액 부족, no card = 미등록 카드 또는 결제 후 잔액 0원)");
		}

		// 실서버 jar 는 같은 카드·결제번호로 이미 결제된 건이면 차감하지 않고 "no card" 로 응답한다.
		r = post("같은 결제번호로 다시 결제 → 차감 없이 FAIL(no card)", "/postCyberMoneyPayment", pay);
		if (firstOk) {
			expect(r, "reason", "no card");
		} else {
			System.out.println("   (첫 결제가 실패해서 중복 확인은 건너뜀)");
		}

		r = post("교세라 사이버머니 결제 100원", "/kyocera/postCyberMoneyPayment", json(
				"terminalID", terminalID, "userCard", userCard, "cyberNo", newNo(), "totalAmount", "100",
				"paperSize", "A4", "color", "2", "jobType", "print", "totalPages", "1"));
		expectIn(r, "result", "OK", "FAIL");

		// 3. 카드·교통카드 결제 기록 (실서버 jar 는 응답이 항상 OK, 기록 내용은 jw_payment 에서 확인)
		section("3. 카드·교통카드 결제 기록");
		expect(post("신용카드 (신한 029)", "/postCraditPayment", card("R09", newNo(), "R11", "N", "R12", "029", "R33", "")), "result", "OK");
		expect(post("체크카드 (카드사 코드 없음 → 카드사명 빈 값)", "/postCraditPayment", card("R09", newNo(), "R11", "Y", "R12", "", "R33", "")), "result", "OK");
		expect(post("티머니", "/postCraditPayment", card("R11", "", "R33", "TM")), "result", "OK");
		expect(post("캐시비", "/postCraditPayment", card("R11", "", "R33", "EB")), "result", "OK");
		expect(post("기타 교통카드 (실서버 jar 는 기록하지 않음)", "/postCraditPayment", card("R11", "", "R33", "RP")), "result", "OK");
		expect(post("KICC 단말기 카드 (국민 016)", "/kicc/postCraditPayment", card("R09", newNo(), "R10", "N", "R12", "016", "R33", "")), "result", "OK");
		expect(post("프린트누리 결제 (삼성 031)", "/printnuri/payment", json(
				"terminalID", terminalID, "price", "100", "payOK", newNo(), "craditName", "031", "payType", "N",
				"payTypeTM", "", "color", "false", "count", "1", "paperSize", "A4")), "result", "OK");

		// 4. 단말기 소켓 (선택)
		if (socketTest) {
			section("4. 단말기 소켓 " + socketHost + ":" + socketPort);
			socketPolicy();
		}

		// 결과
		System.out.println("==========================================================================");
		System.out.println("통과 " + pass + "  실패 " + fail);
		for (String f : failures) {
			System.out.println("  ✗ " + f);
		}
		System.out.println();
		System.out.println("결제 기록 확인 SQL:");
		System.out.println("  SELECT TOP 20 payment_type, payment_cardName, payment_pay, payment_ok, reg_date FROM jw_payment WHERE terminal_id = '" + terminalID + "' ORDER BY reg_date DESC;");
		System.out.println("테스트 기록 지우기:");
		System.out.println("  DELETE FROM jw_payment WHERE terminal_id = '" + terminalID + "' AND reg_date >= CAST(GETDATE() AS DATE);");
	}

	// ---------------- 요청 ----------------

	static Map<String, String> card(String... extra) {
		Map<String, String> m = json("terminalID", terminalID, "totalAmount", "100", "paperSize", "A4",
				"color", "false", "jobType", "print", "totalPages", "1");
		m.putAll(json(extra));
		return m;
	}

	static Map<String, String> post(String name, String path, Map<String, String> body) {
		String req = toJson(body);
		System.out.println("▶ " + name + "  POST " + path);
		System.out.println("   요청: " + req);
		long t0 = System.currentTimeMillis();
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + path).openConnection();
			con.setConnectTimeout(5000);
			con.setReadTimeout(30000);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			OutputStream os = con.getOutputStream();
			os.write(req.getBytes(StandardCharsets.UTF_8));
			os.close();
			int status = con.getResponseCode();
			InputStream is = status < 400 ? con.getInputStream() : con.getErrorStream();
			String res = is == null ? "" : read(is);
			long ms = System.currentTimeMillis() - t0;
			System.out.println("   응답: " + status + " (" + ms + "ms) " + res);
			Map<String, String> m = parse(res);
			m.put("_status", String.valueOf(status));
			return m;
		} catch (Exception e) {
			System.out.println("   실패: " + e);
			Map<String, String> m = new LinkedHashMap<String, String>();
			m.put("_status", "ERR");
			m.put("_error", e.toString());
			return m;
		}
	}

	static boolean serverAlive() {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + "/postTerminal").openConnection();
			con.setConnectTimeout(3000);
			con.setReadTimeout(5000);
			con.getResponseCode(); // GET 이면 405 가 정상 (POST 전용)
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** 02(가격 정책) 전문을 단말기처럼 보내고, 서버가 받아 주는지 확인한다. */
	static void socketPolicy() {
		byte[] body = new byte[60];
		Arrays.fill(body, (byte) ' ');
		byte[] id = terminalID.getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(id, 0, body, 44 - 7, Math.min(id.length, 10));
		byte[] msg = new byte[7 + body.length + 3];
		int len = msg.length - 4;
		msg[0] = 0x02;
		msg[1] = (byte) (len >> 8);
		msg[2] = (byte) len;
		msg[3] = 1;
		msg[4] = (byte) 0xFB;
		msg[5] = 0x14;
		msg[6] = 0x02;
		System.arraycopy(body, 0, msg, 7, body.length);
		msg[msg.length - 3] = 0x03;

		System.out.println("▶ 02 가격 정책 전문 전송 (" + msg.length + " bytes)");
		try (Socket s = new Socket(socketHost, socketPort)) {
			s.setSoTimeout(15000);
			s.getOutputStream().write(msg);
			s.getOutputStream().flush();
			int n = s.getInputStream().read(); // 서버는 처리 후 접속을 닫는다
			ok("소켓 서버가 전문을 받고 접속을 닫음 (read=" + n + ")");
			System.out.println("   ※ 서버 콘솔에 'ck[02]' 와 단말기 ID 가 찍혔는지 확인하세요.");
		} catch (Exception e) {
			bad("소켓 서버 접속/전송 실패: " + e);
		}
	}

	// ---------------- 확인 ----------------

	static void section(String title) {
		System.out.println();
		System.out.println("[" + title + "]");
	}

	static void expect(Map<String, String> r, String key, String expected) {
		String v = r.get(key);
		if (expected.equals(v)) {
			ok(key + "=" + v);
		} else {
			bad(key + " 기대값 " + expected + ", 실제 " + v + (r.containsKey("_error") ? " (" + r.get("_error") + ")" : " (HTTP " + r.get("_status") + ")"));
		}
	}

	static void expectIn(Map<String, String> r, String key, String... allowed) {
		String v = r.get(key);
		for (String a : allowed) {
			if (a.equals(v)) {
				ok(key + "=" + v);
				return;
			}
		}
		bad(key + " 기대값 " + Arrays.toString(allowed) + ", 실제 " + v + (r.containsKey("_error") ? " (" + r.get("_error") + ")" : " (HTTP " + r.get("_status") + ")"));
	}

	static void ok(String msg) {
		pass++;
		System.out.println("   ✓ " + msg);
	}

	static void bad(String msg) {
		fail++;
		failures.add(msg);
		System.out.println("   ✗ " + msg);
	}

	// ---------------- 도구 ----------------

	static String newNo() {
		try {
			Thread.sleep(2); // 연속 호출 때 같은 번호가 나오지 않게
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
	}

	static Map<String, String> json(String... kv) {
		Map<String, String> m = new LinkedHashMap<String, String>();
		for (int i = 0; i + 1 < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}

	static String toJson(Map<String, String> m) {
		StringBuilder sb = new StringBuilder("{");
		for (Map.Entry<String, String> e : m.entrySet()) {
			if (sb.length() > 1) {
				sb.append(',');
			}
			sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
		}
		return sb.append('}').toString();
	}

	/** 응답 JSON 의 "키":값 을 맵으로 (중첩 없는 단순 JSON 용) */
	static Map<String, String> parse(String json) {
		Map<String, String> m = new LinkedHashMap<String, String>();
		Matcher mt = Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|[^,}\\s]+)").matcher(json);
		while (mt.find()) {
			m.put(mt.group(1), mt.group(3) != null ? mt.group(3) : mt.group(2));
		}
		return m;
	}

	static String read(InputStream is) throws Exception {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		while ((n = is.read(buf)) > 0) {
			b.write(buf, 0, n);
		}
		return new String(b.toByteArray(), StandardCharsets.UTF_8);
	}
}
