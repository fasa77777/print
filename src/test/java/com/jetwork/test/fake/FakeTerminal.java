package com.jetwork.test.fake;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트용 가짜 결제 단말기.
 * - 127.0.0.1:10000 에서 서버의 결제 요청(Xerox 결제)을 받고 응답(ack)을 보낸 뒤 onCharge 를 실행한다
 * - frame(...) 으로 단말기 전문을 만들고 send(...) 로 서버 소켓(10020)에 보낸다
 */
public class FakeTerminal implements AutoCloseable {

	public static final int PAY_PORT = 10000;
	public static final int SERVER_SOCKET_PORT = 10020;

	private final ServerSocket server;
	/** 받은 결제 요청 수 */
	public final AtomicInteger charges = new AtomicInteger();
	/** 마지막으로 받은 결제 요청 본문 (예: S01=A1;S02=40;S09=00;S10=700;) */
	public volatile String lastCharge = "";
	/** 결제 요청을 받은 뒤 할 일 (예: 승인 결과 전문을 10020 으로 보냄). null 이면 아무것도 안 함 */
	public volatile Runnable onCharge;

	public FakeTerminal() throws Exception {
		server = new ServerSocket(PAY_PORT, 50, InetAddress.getByName("127.0.0.1"));
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (!server.isClosed()) {
					try {
						Socket s = server.accept();
						try {
							s.setSoTimeout(3000);
							byte[] buf = new byte[512];
							int n = s.getInputStream().read(buf);
							lastCharge = n > 7 ? new String(buf, 7, Math.max(0, n - 10), StandardCharsets.US_ASCII) : "";
							s.getOutputStream().write(new byte[] { 0x06, 0x06, 0x06 });
							s.getOutputStream().flush();
						} finally {
							s.close();
						}
						charges.incrementAndGet();
						final Runnable r = onCharge;
						if (r != null) {
							new Thread(new Runnable() {
								public void run() {
									sleep(300);
									r.run();
								}
							}).start();
						}
					} catch (Exception e) {
						// 닫힘
					}
				}
			}
		}, "fake-terminal");
		t.setDaemon(true);
		t.start();
	}

	/** 단말기 전문: [STX][길이 2][순번][CMD][GCD][작업 코드] 본문 [ETX][CRC 2] */
	public static byte[] frame(int jobCode, byte[] body) {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		int len = 7 + body.length + 3 - 4;
		o.write(0x02);
		o.write(len >> 8);
		o.write(len & 0xff);
		o.write(1);
		o.write(0x41);
		o.write(0x30);
		o.write(jobCode);
		o.write(body, 0, body.length);
		o.write(0x03);
		o.write(0);
		o.write(0);
		return o.toByteArray();
	}

	public static byte[] frame(int jobCode, String body) {
		return frame(jobCode, body.getBytes(StandardCharsets.US_ASCII));
	}

	/** 02 (가격 정책 요청): 인덱스 44 부터 10바이트가 단말기 ID */
	public static byte[] policyFrame(String terminalID) {
		byte[] body = new byte[37 + 10 + 3];
		Arrays.fill(body, (byte) '0');
		byte[] id = String.format("%-10s", terminalID).getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(id, 0, body, 37, 10);
		return frame(2, body);
	}

	/** 06 (사이버머니 카드 태그): 인덱스 8 부터 4바이트가 카드번호 */
	public static byte[] cardTagFrame(byte b1, byte b2, byte b3, byte b4) {
		return frame(6, new byte[] { 0, b1, b2, b3, b4 });
	}

	/** 서버 소켓(10020)에 전문을 보내고, 서버가 접속을 닫을 때까지 기다린다 */
	public static void send(byte[] message) throws Exception {
		Socket s = new Socket("127.0.0.1", SERVER_SOCKET_PORT);
		try {
			s.getOutputStream().write(message);
			s.getOutputStream().flush();
			s.setSoTimeout(15000);
			InputStream in = s.getInputStream();
			while (in.read() >= 0) {
				// 서버가 닫을 때까지
			}
		} finally {
			s.close();
		}
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void close() throws Exception {
		server.close();
	}
}
