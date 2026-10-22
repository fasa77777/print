package com.jetwork.bin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jetwork.sql.DBCPConnectionMgr;
import com.jetwork.sql.DbConfig;

import co.kr.json.JsonHttpClient;

/**
 * 디버깅 페이지 (http://서버:8090/jetwork/debug).
 *
 * 기본은 꺼져 있다. 실행 옵션 --jetwork.debug-ui=true (또는 -Djetwork.debug-ui=true) 로 켰을 때만 이 클래스가 만들어지고,
 * 꺼져 있으면 /jetwork/debug 주소는 404 다.
 * 비밀번호가 없으므로 주소를 아는 사람은 누구나 볼 수 있다. 문제를 볼 때만 켜고 다 보면 끈다.
 *
 * - 서버 상태: DB 커넥션 풀, 접속한 결제 단말기, 메모리·스레드, 설정
 * - 최근 요청: 요청·응답 본문, 걸린 시간, 그 요청에서 실행한 SQL (RequestRecorder, 최근 200건, 카드번호 가림)
 * - 전체 로그 켜기/끄기: 재시작 없이 logging.group.jetwork-all 을 DEBUG / 기본으로
 * - 테스트 요청: 페이지에서 각 API 를 직접 호출 (페이지의 자바스크립트가 보냄)
 */
@RestController
@ConditionalOnProperty(name = "jetwork.debug-ui", havingValue = "true")
public class DebugController {

	private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

	static final String PATH = "/jetwork/debug";
	private static final String TRACE_GROUP = "logging.group.jetwork-all";
	private static final long STARTED = System.currentTimeMillis();

	private final Environment env;
	private final LoggingSystem loggingSystem;

	public DebugController(Environment env) {
		this.env = env;
		this.loggingSystem = LoggingSystem.get(DebugController.class.getClassLoader());
		RequestRecorder.enable();
		logger.warn("디버깅 페이지 켜짐: http://<서버 주소>:{}{} (비밀번호 없음. 다 쓰면 jetwork.debug-ui 옵션을 빼고 재시작하세요)",
				env.getProperty("server.port", "8090"), PATH);
	}

	/** 디버깅 페이지 */
	@GetMapping(path = { PATH, PATH + "/" }, produces = "text/html;charset=UTF-8")
	public ResponseEntity<String> page() throws IOException {
		try (InputStream in = DebugController.class.getResourceAsStream("/debug/index.html")) {
			if (in == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
					.body(StreamUtils.copyToString(in, StandardCharsets.UTF_8));
		}
	}

	/** 서버 상태 */
	@GetMapping(PATH + "/api/status")
	public Map<String, Object> status() {
		Map<String, Object> out = new LinkedHashMap<String, Object>();

		Map<String, Object> server = new LinkedHashMap<String, Object>();
		server.put("version", version());
		server.put("java", System.getProperty("java.version"));
		server.put("startedAt", STARTED);
		server.put("uptimeMillis", System.currentTimeMillis() - STARTED);
		server.put("port", env.getProperty("local.server.port", env.getProperty("server.port", "8090")));
		server.put("socketServer", Boolean.getBoolean("socket.server"));
		server.put("relayBaseUrl", JsonHttpClient.BASE_URL);
		server.put("traceOn", traceOn());
		out.put("server", server);

		Map<String, Object> db = new LinkedHashMap<String, Object>();
		db.put("url", maskUrl(DbConfig.getUrl()));
		db.put("user", DbConfig.getUsername());
		db.put("pool", ConnectionPool.getInst().poolStatus());
		out.put("db", db);

		Map<String, Object> jvm = new LinkedHashMap<String, Object>();
		MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		jvm.put("heapUsedMB", heap.getUsed() / 1048576);
		jvm.put("heapMaxMB", heap.getMax() / 1048576);
		jvm.put("threads", ManagementFactory.getThreadMXBean().getThreadCount());
		out.put("jvm", jvm);

		List<Map<String, Object>> terminals = new ArrayList<Map<String, Object>>();
		for (TerminalState t : TerminalRegistry.all()) {
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("ip", t.getIp());
			m.put("terminalID", t.getTerminalID());
			m.put("lastSeen", t.getLastSeen());
			m.put("policyRequested", t.isPolicyRequested());
			TerminalState.Job job = t.currentJob();
			m.put("job", job == null ? null : job.getPrice() + "원 / 컬러 " + job.getColor() + " / " + job.getCount() + "장");
			terminals.add(m);
		}
		out.put("terminals", terminals);
		return out;
	}

	/** DB 연결 확인 (SELECT 1 을 한 번 실행하고 걸린 시간) */
	@PostMapping(PATH + "/api/dbcheck")
	public Map<String, Object> dbCheck() {
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		DBCPConnectionMgr pool = ConnectionPool.getInst();
		long t0 = System.nanoTime();
		Connection conn = pool.getConnection();
		if (conn == null) {
			out.put("ok", false);
			out.put("message", "커넥션을 가져오지 못함 (" + pool.poolStatus() + ")");
			return out;
		}
		try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
			rs.next();
			out.put("ok", true);
			out.put("message", "정상");
		} catch (Exception e) {
			out.put("ok", false);
			out.put("message", e.toString());
		} finally {
			pool.freeConnection(conn);
		}
		out.put("elapsedMillis", (System.nanoTime() - t0) / 1000000);
		return out;
	}

	/** 최근 요청. after 보다 번호가 큰 것만 (최신 순) */
	@GetMapping(PATH + "/api/requests")
	public List<RequestRecorder.Record> requests(@RequestParam(name = "after", defaultValue = "0") long after) {
		return RequestRecorder.since(after);
	}

	/** 최근 요청 기록 지우기 */
	@PostMapping(PATH + "/api/requests/clear")
	public Map<String, Object> clearRequests() {
		RequestRecorder.clear();
		return result("ok", true);
	}

	/** 전체 로그 상태 */
	@GetMapping(PATH + "/api/trace")
	public Map<String, Object> trace() {
		return result("on", traceOn());
	}

	/** 전체 로그 켜기/끄기 ({"on": true}). 서버를 재시작하면 application.properties 설정으로 돌아간다 */
	@PostMapping(PATH + "/api/trace")
	public Map<String, Object> trace(@RequestBody Map<String, Object> body) {
		boolean on = Boolean.TRUE.equals(body.get("on"));
		for (String name : env.getProperty(TRACE_GROUP, "jetwork.trace,com.jetwork,com.stech,co.kr").split(",")) {
			loggingSystem.setLogLevel(name.trim(), on ? LogLevel.DEBUG : null);
		}
		logger.warn("디버깅 페이지에서 전체 로그 {}", on ? "켬" : "끔");
		return result("on", traceOn());
	}

	private static boolean traceOn() {
		return LoggerFactory.getLogger("jetwork.trace.http").isDebugEnabled();
	}

	private static Map<String, Object> result(String key, Object value) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put(key, value);
		return m;
	}

	private static String version() {
		String v = DebugController.class.getPackage().getImplementationVersion();
		return v == null ? "(이클립스 실행)" : v;
	}

	/** URL 에 비밀번호가 들어 있으면 가린다 */
	static String maskUrl(String url) {
		return url == null ? null : url.replaceAll("(?i)(password|pwd)=[^;]*", "$1=****");
	}
}
