package com.stech.Network;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.TerminalRegistry;
import com.jetwork.bin.TerminalState;

import co.kr.json.JSonForm;
import co.kr.json.JSonForm1;
import co.kr.json.JSonForm2;
import co.kr.json.JSonForm3;
import co.kr.up.util.Bytes;
import co.kr.up.util.HexUtils;
import co.kr.up.util.Mask;

/**
 * 결제 단말기 소켓 서버 (포트 10020).
 *
 * 접속을 받는 스레드는 받기만 하고, 실제 처리(전문 해석, 중계 서버 HTTP 요청)는
 * 작업 스레드 풀에서 단말기마다 따로 한다. 한 단말기 처리가 느려도 다른 단말기는 기다리지 않는다.
 */
public class SocketServerDriver implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SocketServerDriver.class);

	private static final int PORT = 10020;
	/** 동시에 처리할 수 있는 단말기 접속 수 */
	private static final int WORKER_THREADS = 50;
	/** 작업 스레드가 모두 바쁠 때 줄 세워 둘 수 있는 접속 수. 넘으면 접속 받는 스레드가 직접 처리한다 */
	private static final int QUEUE_SIZE = 1000;
	/** 단말기가 접속 후 이 시간 안에 데이터를 보내지 않으면 끊는다 */
	private static final int READ_TIMEOUT_MILLIS = 10000;

	private final ExecutorService workers = new ThreadPoolExecutor(
			WORKER_THREADS, WORKER_THREADS, 60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(QUEUE_SIZE),
			new ThreadFactory() {
				private final AtomicInteger seq = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "terminal-socket-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			},
			new ThreadPoolExecutor.CallerRunsPolicy());

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			logger.info("단말기 소켓 서버 시작 port={} (작업 스레드 {}개)", PORT, WORKER_THREADS);

			while (true) {
				final Socket socket;
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					logger.warn("단말기 접속 받기 실패: {}", e.toString());
					continue;
				}
				workers.execute(new Runnable() {
					@Override
					public void run() {
						handleConnection(socket);
					}
				});
			}
		} catch (IOException ex) {
			logger.error("단말기 소켓 서버 종료 (포트 " + PORT + " 사용 불가?)", ex);
		}
	}

	/** 단말기 접속 하나를 처리하고 소켓을 닫는다. */
	private void handleConnection(Socket socket) {
		try {
			socket.setSoTimeout(READ_TIMEOUT_MILLIS);
			handle(socket);
		} catch (Exception e) {
			logger.warn("단말기 전문 처리 중 오류 {} : {}", socket.getInetAddress().getHostAddress(), e.toString());
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// 이미 닫힘
			}
		}
	}

	/** 한 번에 받을 수 있는 전문 최대 크기 */
	private static final int MAX_MESSAGE_BYTES = 8192;
	/** 전문이 나눠서 도착할 때 나머지 조각을 기다리는 시간 */
	private static final int CONTINUATION_WAIT_MILLIS = 300;

	/**
	 * 단말기가 보낸 전문 한 건을 끝까지 읽는다.
	 * TCP 는 한 전문을 여러 조각으로 나눠 보낼 수 있어서, 한 번만 읽으면 뒷부분(R04 등)이 잘릴 수 있다.
	 * 헤더의 길이 필드(전체 길이 - 4)만큼 모이거나, ETX(0x03) + CRC 2바이트로 끝나거나,
	 * 다음 조각이 CONTINUATION_WAIT_MILLIS 안에 오지 않으면 거기까지를 한 전문으로 본다.
	 *
	 * @return 받은 바이트. 아무것도 안 보내고 끊으면 길이 0
	 */
	private static byte[] readMessage(Socket socket) throws IOException {
		InputStream is = socket.getInputStream();
		byte[] buf = new byte[MAX_MESSAGE_BYTES];
		int total = is.read(buf);
		if (total <= 0) {
			return new byte[0];
		}
		int expected = -1;
		if (total >= 3 && buf[0] == 0x02) {
			expected = (((buf[1] & 0xff) << 8) | (buf[2] & 0xff)) + 4;
			if (expected < 10 || expected > MAX_MESSAGE_BYTES) {
				expected = -1;
			}
		}
		socket.setSoTimeout(CONTINUATION_WAIT_MILLIS);
		while (total < buf.length && !isComplete(buf, total, expected)) {
			int n;
			try {
				n = is.read(buf, total, buf.length - total);
			} catch (SocketTimeoutException e) {
				break;
			}
			if (n <= 0) {
				break;
			}
			total += n;
		}
		return Arrays.copyOf(buf, total);
	}

	private static boolean isComplete(byte[] buf, int total, int expected) {
		if (expected > 0) {
			return total >= expected;
		}
		return total >= 3 && buf[total - 3] == 0x03;
	}

	/** 단말기가 보낸 전문 한 건을 해석해서 처리한다. */
	private void handle(Socket socket) throws Exception {
		String ip = socket.getInetAddress().getHostAddress();

		// 1. 전문을 끝까지 읽는다
		byte[] rcvData = readMessage(socket);
		if (rcvData.length == 0) {
			logger.debug("단말기 접속 후 데이터 없이 끊김 {}", ip);
			return;
		}

		// 2. 이 단말기(IP)의 상태를 가져온다. 단말기 ID·진행 중인 결제·결과는 단말기마다 따로 관리한다
		TerminalState terminal = TerminalRegistry.connected(ip);

		// 3. 헤더: [0]=STX, [1~2]=길이, [3]=순번, [4]=CMD, [5]=GCD, [6]=작업 코드
		if (rcvData.length < 7) {
			logger.warn("헤더보다 짧은 전문 무시 {} : {}", terminal, Arrays.toString(rcvData));
			return;
		}
		String ck = Bytes.toHexString(rcvData[6]).trim();
		logger.info("전문 수신 {} 작업코드={} {}bytes", terminal, ck, rcvData.length);
		logger.debug("전문 원본 {}", Arrays.toString(rcvData));

		if ("02".equals(ck)) {
			// 02: 가격 정책 요청. 인덱스 44부터 10바이트가 단말기 ID.
			// 단말기마다 처음 한 번(성공할 때까지) 중계 서버에 가격 정책을 요청한다
			if (rcvData.length < 44 + 10) {
				logger.warn("02 전문이 짧아 단말기 ID 를 읽을 수 없음 {} {}bytes", terminal, rcvData.length);
				return;
			}
			StringBuffer stBuffer = new StringBuffer();
			for (byte b : getbytes(rcvData, 44, 10)) {
				stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b)));
			}
			terminal.setTerminalID(stBuffer.toString().trim());
			if (!terminal.isPolicyRequested()) {
				new JSonForm1().setPolicyJson(terminal);
			}

		} else if ("09".equals(ck)) {
			// 09: 기타 전문. 처리하지 않는다

		} else if ("06".equals(ck)) {
			// 06: 사이버머니 카드 태그. 인덱스 8부터 4바이트가 카드번호(16진수).
			// 이 단말기에서 진행 중인 출력 작업 금액으로 중계 서버에 사이버머니 결제를 요청한다 (JSonForm2)
			if (rcvData.length < 8 + 4) {
				logger.warn("06 전문이 짧아 카드번호를 읽을 수 없음 {} {}bytes", terminal, rcvData.length);
				return;
			}
			StringBuffer stBuffer = new StringBuffer();
			for (byte b : getbytes(rcvData, 8, 4)) {
				stBuffer.append(HexUtils.toHexString(b));
			}
			logger.info("사이버머니 카드 {} card={}", terminal, Mask.card(stBuffer.toString()));
			new JSonForm2().setPaymentJson(terminal, stBuffer.toString());

		} else if ("04".equals(ck)) {
			handlePaymentResult(terminal, new String(rcvData, "UTF-8"));
		}
	}

	/**
	 * 04: 카드·티머니 결제 결과. 본문은 "S03=...;S10=...;R04=...;" 처럼 ';' 로 나뉜 "키=값" 목록이다.
	 *  - CANCEL 이 들어 있으면 취소
	 *  - R32 값이 있으면 티머니 결과다. 첫 글자가 FF 면 거절, 아니면 승인 (JSonForm3).
	 *    티머니 결과에서는 R04 를 보지 않는다 (카드 결제로 한 번 더 기록되지 않게)
	 *  - 티머니가 아니면 R04=0000 이 카드 승인 (JSonForm), 그 밖의 값은 거절
	 * 승인·거절이 정해지면 그 단말기의 결제 결과로 기록해서, 기다리던 Xerox 결제 API 를 깨운다.
	 */
	private void handlePaymentResult(TerminalState terminal, String message) {
		String[] sD = message.split(";");
		java.util.Map<String, String> f = co.kr.json.JSonForm.toMap(sD);

		if (message.contains("CANCEL")) {
			terminal.setResult(false);
			logger.info("결제 취소 {}", terminal);
			return;
		}

		String r32 = f.get("R32");
		if (r32 != null && !r32.isEmpty()) {
			if (String.format("%02X", (int) r32.charAt(0)).startsWith("FF")) {
				terminal.setResult(false);
				logger.info("티머니 승인 거절 {}", terminal);
			} else {
				terminal.setResult(true);
				logger.info("티머니 승인 {} 금액={}", terminal, f.get("S10"));
				new JSonForm3().setPaymentJson(terminal, sD);
			}
			return;
		}

		String code = f.get("R04");
		if (code == null || code.isEmpty()) {
			logger.warn("결제 결과 코드(R04)가 없는 04 전문 {}", terminal);
			return;
		}
		if ("0000".equals(code)) {
			terminal.setResult(true);
			logger.info("카드 승인 {} 금액={} 승인번호={}", terminal, f.get("S10"), f.get("R09"));
			new JSonForm().setPaymentJson(sD);
		} else {
			terminal.setResult(false);
			logger.info("카드 승인 거절 {} R04={}", terminal, code);
		}
	}

	/** 문자열의 각 글자를 "%02X " 형식의 16진수로 바꾼다. 예) "AB" -> "41 42 " */
	  public String stringToHex(String s) {
		    String result = "";

		    for (int i = 0; i < s.length(); i++) {
		      result += String.format("%02X ", (int) s.charAt(i));
		    }

		    return result;
	  }
	  
			/** src 의 offset 부터 length 바이트를 잘라 새 배열로 돌려준다. */
		public static final byte[] getbytes(byte src[], int offset, int length){
	        byte dest[] = new byte[length];
	        System.arraycopy(src, offset, dest, 0, length);
	        return dest;
	    }

}
