package com.jetwork.test.fake;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 테스트용 가짜 중계 서버 (pay1.printnuri.com 대신). 받은 요청을 "경로 본문" 으로 기록하고 정해 둔 응답을 돌려준다.
 * 서버는 -Drelay.baseUrl=http://127.0.0.1:포트/jetwork 로 이쪽을 보게 한다.
 */
public class FakeRelay implements AutoCloseable {

	private final ServerSocket server;
	/** 받은 요청 ("/jetwork/printnuri/payment {...}") */
	public final List<String> requests = Collections.synchronizedList(new ArrayList<String>());
	/** 돌려줄 HTTP 상태 코드와 본문 */
	public volatile int status = 200;
	public volatile String body = "{\"result\":\"OK\"}";

	public FakeRelay() throws Exception {
		server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (!server.isClosed()) {
					try {
						final Socket s = server.accept();
						new Thread(new Runnable() {
							public void run() {
								handle(s);
							}
						}).start();
					} catch (Exception e) {
						// 닫힘
					}
				}
			}
		}, "fake-relay");
		t.setDaemon(true);
		t.start();
	}

	public int port() {
		return server.getLocalPort();
	}

	/** 기록 중 text 가 들어간 요청들 */
	public List<String> containing(String text) {
		List<String> out = new ArrayList<String>();
		synchronized (requests) {
			for (String r : requests) {
				if (r.contains(text)) out.add(r);
			}
		}
		return out;
	}

	private void handle(Socket s) {
		try {
			s.setSoTimeout(5000);
			InputStream in = s.getInputStream();
			String head = readHead(in);
			String path = head.split(" ")[1];
			int length = 0;
			for (String line : head.split("\r\n")) {
				if (line.toLowerCase().startsWith("content-length:")) length = Integer.parseInt(line.substring(15).trim());
			}
			byte[] b = new byte[length];
			int off = 0;
			while (off < length) {
				int n = in.read(b, off, length - off);
				if (n < 0) break;
				off += n;
			}
			requests.add(path + " " + new String(b, 0, off, StandardCharsets.UTF_8));
			byte[] out = body.getBytes(StandardCharsets.UTF_8);
			OutputStream os = s.getOutputStream();
			os.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json\r\nContent-Length: " + out.length
					+ "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			os.write(out);
			os.flush();
		} catch (Exception e) {
			// 무시
		} finally {
			try {
				s.close();
			} catch (Exception e) {
				// 무시
			}
		}
	}

	private static String readHead(InputStream in) throws Exception {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		int c;
		while ((c = in.read()) >= 0) {
			b.write(c);
			byte[] a = b.toByteArray();
			int n = a.length;
			if (n >= 4 && a[n - 4] == '\r' && a[n - 3] == '\n' && a[n - 2] == '\r' && a[n - 1] == '\n') break;
		}
		return new String(b.toByteArray(), StandardCharsets.US_ASCII);
	}

	public void close() throws Exception {
		server.close();
	}
}
