package com.jetwork.bin;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import com.jetwork.sql.DBCPConnectionMgr;
import com.stech.Network.SocketServerDriver;

/**
 * 서버 시작점.
 *
 * 1. Spring Boot 웹 서버(HTTP API, 포트는 application.properties 의 server.port)를 띄우고
 * 2. DB 커넥션 풀(DBCP)을 만들고
 * 3. 결제 단말기와 통신하는 소켓 서버(포트 10020)를 별도 스레드로 띄운다.
 *
 * 컨트롤러는 이 클래스와 같은 패키지(com.jetwork.bin) 아래에 있어야 Spring 이 찾는다.
 *
 * DB 는 DBCPConnectionMgr 커넥션 풀만 쓴다. Spring 이 따로 만드는 DataSource(Hikari 풀)는
 * 아무도 쓰지 않으면서 DB 커넥션만 차지하므로 끈다.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class JetworkServerApplication {

	// ---------------------------------------------------------------
	// 소켓 서버(SocketServerDriver)와 Xerox 결제 API(/xerox/xeroxPayment)가
	// 주고받는 공유 상태. 동시에 여러 결제가 들어오면 서로 덮어쓸 수 있으므로
	// 단말기 1대 기준으로만 안전하다. (docs/ARCHITECTURE.md 참고)
	// ---------------------------------------------------------------

	/** 마지막으로 소켓에 접속한 결제 단말기 IP */
	public static String TerminalIP = "";
	/** 마지막으로 받은 단말기 ID */
	public static String terID = "";
	/** 결제 승인 여부 (true = 승인) */
	public static boolean chk = false;
	/** 결제 결과 도착 여부 (true 가 되면 Xerox 결제 API 가 대기를 끝냄) */
	public static boolean chk1 = false;
	public static boolean result = false;
	/** 가격 정책 요청을 보낼지 여부 */
	public static boolean chk2 = true;
	/** 현재 진행 중인 출력 작업 정보 */
	public static int printPrice = 0;
	public static String printColor = "";
	public static String printPaperSize = "";
	public static String printCount = "";

	public static void main(String[] args) {
		SpringApplication.run(JetworkServerApplication.class, args);

		// DB 커넥션 풀 생성
		ConnectionPool cp = new ConnectionPool();
		cp.connection();

		// 결제 단말기 소켓 서버 시작
		Thread socketThread = new Thread(new SocketServerDriver());
		socketThread.setName("SocketServerDriver #1");
		socketThread.start();

		// 시작할 때 DB 에 접속되는지 한 번 확인 (확인 후 커넥션은 바로 반납)
		DBCPConnectionMgr dm = ConnectionPool.getInst();
		Connection conn = dm.getConnection();
		System.out.println("DB 접속 확인: " + (conn != null ? "성공" : "실패"));
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
