package com.jetwork.bin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(JetworkServerApplication.class);

	// 단말기별 상태(단말기 IP·ID·진행 중인 결제·결제 결과)는 TerminalRegistry 에서 단말기마다 따로 관리한다.

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
		logger.info("DB 접속 확인: " + (conn != null ? "성공" : "실패"));
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("JetworkServerApplication 처리 중 오류", e);
			}
		}
	}
}
