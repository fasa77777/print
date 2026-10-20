package com.stech.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.jetwork.bin.JetworkServerApplication;

import co.kr.json.JSonForm;
import co.kr.json.JSonForm1;
import co.kr.json.JSonForm2;
import co.kr.json.JSonForm3;
import co.kr.up.util.Bytes;
import co.kr.up.util.HexUtils;
/**
 * 결제 단말기 소켓 서버 (포트 10020).
 *
 * 접속을 받는 스레드는 받기만 하고, 실제 처리(전문 해석, 중계 서버 HTTP 요청)는
 * 작업 스레드 풀에서 단말기마다 따로 한다. 한 단말기 처리가 느려도 다른 단말기는 기다리지 않는다.
 */
public class SocketServerDriver implements Runnable {

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
			System.out.println("Server is listening on port " + PORT + " (worker threads " + WORKER_THREADS + ")");

			while (true) {
				final Socket socket;
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					System.out.println("accept 실패: " + e);
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
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/** 단말기 접속 하나를 처리하고 소켓을 닫는다. */
	private void handleConnection(Socket socket) {
		try {
			socket.setSoTimeout(READ_TIMEOUT_MILLIS);
			handle(socket);
		} catch (Exception e) {
			System.out.println("Socket close" + e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// 이미 닫힘
			}
		}
	}

	/** 단말기가 보낸 전문 한 건을 해석해서 처리한다. (처리 내용은 기존과 같다) */
	@SuppressWarnings("static-access")
	private void handle(Socket socket) throws Exception {
                     System.out.println("New Terminal connected  10020");
                     // 1. 단말기가 보낸 데이터를 최대 1000바이트 읽는다 (10초 안에 안 오면 SocketTimeoutException)
                     InputStream is = socket.getInputStream();

                     byte[]rcvData  = new byte[1000];

                     int readByteCount = is.read(rcvData );

                     String message = new String(rcvData , 0, readByteCount, "UTF-8");

                     System.out.println("[[데이터 받기 성공]]: " + message);

                     System.out.println(socket.getInetAddress().toString());
          			// 2. 단말기 IP 를 기억해 둔다. Xerox 결제 API 가 이 IP 의 10000번 포트로 결제 요청을 보낸다
          			String [] ss = socket.getInetAddress().toString().split("/");
          			System.out.println("["+ss[1]+"]");
          			JetworkServerApplication.TerminalIP = ss[1];
          			String newString = new String(rcvData, "EUC-KR");
         			System.out.println(Arrays.toString(rcvData));
         			// 3. 헤더를 1바이트씩 16진수로 찍어 본다 (로그용): [0]=STX, [1~2]=길이, [3]=순번, [4]=CMD, [5]=GCD, [6]=작업 코드
         			byte[]b_command = getbytes(rcvData,0,1);
         			StringBuffer stBuffer = new StringBuffer();
         	        for (int i = 0; i < b_command.length; i++) {
         	        	stBuffer.append(Bytes.toHexString(b_command[i]));
         	        }	
         	        
         	        System.out.println("1["+stBuffer.toString()+"]");
         	       byte[]b_command1 = getbytes(rcvData,1,2);
        	        stBuffer = new StringBuffer();
        	        for (int i = 0; i < b_command1.length; i++) {
        	        	stBuffer.append(Bytes.toHexString(b_command1[i]));
        	        }	
        	        
        	        System.out.println("2["+stBuffer.toString()+"]");
        	        
        	        
        	        
        	        byte[]b_command2 = getbytes(rcvData,3,1);
        	        stBuffer = new StringBuffer();
        	        for (int i = 0; i < b_command2.length; i++) {
        	        	stBuffer.append(Bytes.toHexString(b_command2[i]));
        	        }	
        	        
        	        System.out.println("3["+stBuffer.toString()+"]");
        	        
        	        byte[]b_command3 = getbytes(rcvData,4,1);
        	        stBuffer = new StringBuffer();
        	        for (int i = 0; i < b_command3.length; i++) {
        	        	stBuffer.append(Bytes.toHexString(b_command3[i]));
        	        }	
        	        
        	        System.out.println("4["+stBuffer.toString()+"]");
        	        
        	        byte[]b_command4 = getbytes(rcvData,5,1);
        	        stBuffer = new StringBuffer();
        	        for (int i = 0; i < b_command4.length; i++) {
        	        	stBuffer.append(Bytes.toHexString(b_command4[i]));
        	        }	
        	        
        	        System.out.println("5["+stBuffer.toString()+"]");
        	        
        	        byte[]b_command5 = getbytes(rcvData,6,1);
        	        stBuffer = new StringBuffer();
        	        for (int i = 0; i < b_command5.length; i++) {
        	        	stBuffer.append(Bytes.toHexString(b_command5[i]));
        	        }	
        	        
        	        System.out.println("6["+stBuffer.toString()+"]");
        	        String poli = stBuffer.toString();
        	 
        	        // 4. 7번째 바이트(인덱스 6)가 작업 코드. 코드별로 처리가 나뉜다
        	        String ck= stBuffer.toString().trim();
        	        
        	        System.out.println("ck["+ck+"]");
        	        
        	        // 02: 가격 정책 요청. 인덱스 44부터 10바이트가 단말기 ID. 서버 시작 후 처음 한 번만(chk2) 중계 서버에 정책을 요청한다
        	        if("02".equals(ck)) {
         	            byte[]b_command6 = getbytes(rcvData,6+38,10);
         	            stBuffer = new StringBuffer();
         	            for (int i = 0; i < b_command6.length; i++) {
         	            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
         	            	stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b_command6[i])));//01067367774
         	            }	
         	            
         	            System.out.println("["+stBuffer.toString().trim()+"]");
         	           JetworkServerApplication.terID = stBuffer.toString().trim();

         	           if(JetworkServerApplication.chk2) {
         	            	JSonForm1 js1= new JSonForm1();
             	            js1.setPolicyJson(stBuffer.toString().trim());
//             	           Socket sck = null;
//	    	    			OutputStream sos = null;
//	    	    			InputStream sis = null;
//	    	    			sck = new Socket(JetworkServerApplication.TerminalIP, 10000);
//	    	    			sck.setSoTimeout(60000);

	    	    			System.out.println("=========================");
	    	    	 	    	    			
	    	    	    	System.out.println("=========================");
//	    	    	    	ReqM r = new ReqM();
//	    	    			byte[] reqByte = r.assemble();
//	    	    			sos = sck.getOutputStream();
//	    	    			System.out.println("=========================");
//	    	    			sos.write(reqByte);
//	    	    			sos.flush();
         	           }
         	         
  	    			
  	 
         	            
         	        // 09: 기타 전문. 읽기만 하고 처리하지 않는다 (1000바이트 버퍼에서 2000바이트를 읽으려 해 예외가 나고 접속이 닫힌다)
         	        }else if("09".equals(ck)) {
         	            byte[]b_command6 = getbytes(rcvData,6,2000);
         	            stBuffer = new StringBuffer();
         	            for (int i = 0; i < b_command6.length; i++) {
         	            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
         	            	stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b_command6[i])));
         	            }	
         	         //   System.out.println("UTF-8 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("UTF-8"), "EUC-KR"));
         	         //   System.out.println("EUC-KR -> UTF-8         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "UTF-8"));
//         	            
//         	            System.out.println("UTF-8 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("UTF-8"), "EUC-KR"));
//         	            System.out.println("UTF-8 -> KSC5601       : " + new String(stBuffer.toString().getBytes("UTF-8"), "KSC5601"));
//         	            System.out.println("UTF-8 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("UTF-8"), "X-WINDOWS-949"));
//         	            System.out.println("UTF-8 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("UTF-8"), "ISO-8859-1"));
//         	            System.out.println("UTF-8 -> MS949         : " + new String(stBuffer.toString().getBytes("UTF-8"), "MS949"));
//         	             
//         	            System.out.println("UTF-8 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("UTF-8"), "EUC-KR"));
//         	            System.out.println("UTF-8 -> KSC5601       : " + new String(stBuffer.toString().getBytes("UTF-8"), "KSC5601"));
//         	            System.out.println("UTF-8 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("UTF-8"), "X-WINDOWS-949"));
//         	            System.out.println("UTF-8 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("UTF-8"), "ISO-8859-1"));
//         	            System.out.println("UTF-8 -> MS949         : " + new String(stBuffer.toString().getBytes("UTF-8"), "MS949"));
////         	             
//         	            System.out.println("ISO-8859-1 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "EUC-KR"));
//         	            System.out.println("ISO-8859-1 -> KSC5601       : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "KSC5601"));
//         	            System.out.println("ISO-8859-1 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "X-WINDOWS-949"));
//         	            System.out.println("ISO-8859-1 -> UTF-8         : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "UTF-8"));
//         	            System.out.println("ISO-8859-1 -> MS949         : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "MS949"));
//         	             
//         	            System.out.println("EUC-KR -> UTF-8         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "UTF-8"));
//         	            System.out.println("EUC-KR -> KSC5601       : " + new String(stBuffer.toString().getBytes("EUC-KR"), "KSC5601"));
//         	            System.out.println("EUC-KR -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("EUC-KR"), "X-WINDOWS-949"));
//         	            System.out.println("EUC-KR -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("EUC-KR"), "ISO-8859-1"));
//         	            System.out.println("EUC-KR -> MS949         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "MS949"));
//         	             
//         	            System.out.println("KSC5601 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("KSC5601"), "EUC-KR"));
//         	            System.out.println("KSC5601 -> UTF-8         : " + new String(stBuffer.toString().getBytes("KSC5601"), "UTF-8"));
//         	            System.out.println("KSC5601 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("KSC5601"), "X-WINDOWS-949"));
//         	            System.out.println("KSC5601 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("KSC5601"), "ISO-8859-1"));
//         	            System.out.println("KSC5601 -> MS949         : " + new String(stBuffer.toString().getBytes("KSC5601"), "MS949"));
//         	             
//         	            System.out.println("X-WINDOWS-949 -> EUC-KR     : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "EUC-KR"));
//         	            System.out.println("X-WINDOWS-949 -> UTF-8      : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "UTF-8"));
//         	            System.out.println("X-WINDOWS-949 -> KSC5601    : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "KSC5601"));
//         	            System.out.println("X-WINDOWS-949 -> ISO-8859-1 : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "ISO-8859-1"));
//         	            System.out.println("X-WINDOWS-949 -> MS949      : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "MS949"));
//         	                            
//         	            System.out.println("MS949 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("MS949"), "EUC-KR"));
//         	            System.out.println("MS949 -> UTF-8         : " + new String(stBuffer.toString().getBytes("MS949"), "UTF-8"));
//         	            System.out.println("MS949 -> KSC5601       : " + new String(stBuffer.toString().getBytes("MS949"), "KSC5601"));
//         	            System.out.println("MS949 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("MS949"), "ISO-8859-1"));
//         	            System.out.println("MS949 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("MS949"), "X-WINDOWS-949"));
         	            
         	            System.out.println("ck========"+ck);
         	        // 06: 사이버머니 카드 태그. 인덱스 8부터 4바이트가 카드번호(16진수). 중계 서버에 사이버머니 결제를 요청한다 (JSonForm2)
         	        }else if("06".equals(ck)) {
         	        	
         	        	System.out.println("사이버머니");
         	        	byte[]b_command6 = getbytes(rcvData,6,1);
         	            stBuffer = new StringBuffer();
         	            for (int i = 0; i < b_command6.length; i++) {
         	            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
         	            	stBuffer.append(HexUtils.toHexString(b_command6[i]));
         	            }
         	          //  System.out.println("ISO-8859-1 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "EUC-KR"));
         	            System.out.println("ISO-8859-1 -> EUC-KR        : " +stBuffer.toString());
         	            byte[]b_command7 = getbytes(rcvData,8,4);
         	            stBuffer = new StringBuffer();
         	            for (int i = 0; i < b_command7.length; i++) {
         	            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
         	            	stBuffer.append(HexUtils.toHexString(b_command7[i]));
         	            }
         	            System.out.println("사이버머니 카드 : " + co.kr.up.util.Mask.card(stBuffer.toString()));
         	            
         	            JSonForm2 jf = new JSonForm2();
         				jf.setPaymentJson(stBuffer.toString());
         	            
         	        // 04: 카드·티머니 결제 결과. 본문은 "S03=...;S10=...;R04=...;" 처럼 ';' 로 나뉜 텍스트다
         	        //  - CANCEL 이 들어 있으면 취소
         	        //  - R32 첫 바이트가 FF 면 티머니 거절, 아니면 티머니 승인 (JSonForm3)
         	        //  - R04=0000 이면 카드 승인 (JSonForm), 7000~9999 면 거절
         	        //  승인·거절이 정해지면 chk(승인 여부)와 chk1(결과 도착)을 바꿔 Xerox 결제 API 의 대기를 끝낸다
         	        }else if("04".equals(ck)) {
         	         //   byte[]b_command6 = MessageEvent.getbytes(rcvData,6,2000);
         	         //   stBuffer = new StringBuffer();
         	        //    for (int i = 0; i < b_command6.length; i++) {
         	            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
         	       //     	stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b_command6[i])));
         	        //    }	
         	         //   System.out.println("UTF-8 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("UTF-8"), "EUC-KR"));
         	         //   System.out.println("EUC-KR -> UTF-8         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "UTF-8"));
         	            
//         	            System.out.println("UTF-8 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("UTF-8"), "EUC-KR"));
//         	            System.out.println("UTF-8 -> KSC5601       : " + new String(stBuffer.toString().getBytes("UTF-8"), "KSC5601"));
//         	            System.out.println("UTF-8 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("UTF-8"), "X-WINDOWS-949"));
//         	            System.out.println("UTF-8 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("UTF-8"), "ISO-8859-1"));
//         	            System.out.println("UTF-8 -> MS949         : " + new String(stBuffer.toString().getBytes("UTF-8"), "MS949"));
////         	             
//         	            System.out.println("ISO-8859-1 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "EUC-KR"));
//         	            System.out.println("ISO-8859-1 -> KSC5601       : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "KSC5601"));
//         	            System.out.println("ISO-8859-1 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "X-WINDOWS-949"));
//         	            System.out.println("ISO-8859-1 -> UTF-8         : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "UTF-8"));
//         	            System.out.println("ISO-8859-1 -> MS949         : " + new String(stBuffer.toString().getBytes("ISO-8859-1"), "MS949"));
//         	             
//         	            System.out.println("EUC-KR -> UTF-8         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "UTF-8"));
//         	            System.out.println("EUC-KR -> KSC5601       : " + new String(stBuffer.toString().getBytes("EUC-KR"), "KSC5601"));
//         	            System.out.println("EUC-KR -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("EUC-KR"), "X-WINDOWS-949"));
//         	            System.out.println("EUC-KR -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("EUC-KR"), "ISO-8859-1"));
//         	            System.out.println("EUC-KR -> MS949         : " + new String(stBuffer.toString().getBytes("EUC-KR"), "MS949"));
//         	             
//         	            System.out.println("KSC5601 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("KSC5601"), "EUC-KR"));
//         	            System.out.println("KSC5601 -> UTF-8         : " + new String(stBuffer.toString().getBytes("KSC5601"), "UTF-8"));
//         	            System.out.println("KSC5601 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("KSC5601"), "X-WINDOWS-949"));
//         	            System.out.println("KSC5601 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("KSC5601"), "ISO-8859-1"));
//         	            System.out.println("KSC5601 -> MS949         : " + new String(stBuffer.toString().getBytes("KSC5601"), "MS949"));
//         	             
//         	            System.out.println("X-WINDOWS-949 -> EUC-KR     : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "EUC-KR"));
//         	            System.out.println("X-WINDOWS-949 -> UTF-8      : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "UTF-8"));
//         	            System.out.println("X-WINDOWS-949 -> KSC5601    : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "KSC5601"));
//         	            System.out.println("X-WINDOWS-949 -> ISO-8859-1 : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "ISO-8859-1"));
//         	            System.out.println("X-WINDOWS-949 -> MS949      : " + new String(stBuffer.toString().getBytes("X-WINDOWS-949"), "MS949"));
//         	                            
//         	            System.out.println("MS949 -> EUC-KR        : " + new String(stBuffer.toString().getBytes("MS949"), "EUC-KR"));
//         	            System.out.println("MS949 -> UTF-8         : " + new String(stBuffer.toString().getBytes("MS949"), "UTF-8"));
//         	            System.out.println("MS949 -> KSC5601       : " + new String(stBuffer.toString().getBytes("MS949"), "KSC5601"));
//         	            System.out.println("MS949 -> ISO-8859-1    : " + new String(stBuffer.toString().getBytes("MS949"), "ISO-8859-1"));
//         	            System.out.println("MS949 -> X-WINDOWS-949 : " + new String(stBuffer.toString().getBytes("MS949"), "X-WINDOWS-949"));
         	            
         	         //   String sdata = new String(stBuffer.toString().getBytes("ISO-8859-1"), "EUC-KR");
         	            System.out.println("["+message+"]");
         	            String ss1 = message.trim();
         	            System.out.println(ss1.matches((".*CANCEL.*")));
         	            
         	            if(ss1.matches((".*CANCEL.*"))) {
         	            	JetworkServerApplication.chk = false;
         	            	JetworkServerApplication.chk1 = true;
         					System.out.println("취소");
         					//JSonForm jf = new JSonForm();
         					//jf.setPaymentJson(sD);
         	            }
         	            String [] sD = message.split(";");
         	            
         	           System.out.println("sD == "+sD.toString());
         	            String b = "";
         	            String c = "";
         	            String tm = "";
         	            for (int i = 0; i < sD.length; i++) {
         					System.out.println(sD[i]+"["+i+"]");
         					b = sD[i].replaceAll("R04=", "");
         					String R32 = sD[i].substring(0, 3);
         					//System.out.println("=====================R32========================="+R32);
         					c = sD[i].replaceAll("R32=", "");
         					System.out.println("R32="+c.trim().length());
         					System.out.println("R32 stringToHex="+this.stringToHex(c.trim()).substring(0, 2));
//         					byte []p = this.hexStringToByteArray(this.stringToHex0x(this.stringToHex(c.trim())));
////         					  stBuffer = new StringBuffer();
////         			            for (int ii = 0; ii < p.length; ii++) {
////         			            	//stBuffer.append(Bytes.toHexString(b_command6[i]));
////         			            	stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(p[ii])));
////         			            }	
//         					System.out.println("["+p.toString()+"]");
//         					tm = sD[i].replaceAll("R33=", "");
         					
//         					if("ff".equals(this.stringToHex(c.trim()).substring(0, 2))) {
//         						System.out.println("R32 포함");
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("티머니 승인 거절");
//         						
//         					}else {
//         						System.out.println("R32 미포함");
//         						UPrintServer.chk = true;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인");
//         						JSonForm jf = new JSonForm();
//         						jf.setPaymentJson(sD);
         //
//         					}
//         				
         					if("R32".equals(R32)) {
         						System.out.println("=====================R32========================="+this.stringToHex(c.trim()).substring(0, 2));
         						if("FF".equals(this.stringToHex(c.trim()).substring(0, 2))) {
         						System.out.println("R32 포함");
         						JetworkServerApplication.chk = false;
         						JetworkServerApplication.chk1 = true;
         						System.out.println("티머니 승인 거절");
         						
         						}else {
         							System.out.println("티머니승인");
         							JetworkServerApplication.chk = true;
         							JetworkServerApplication.chk1 = true;
         							System.out.println("승인");
         							JSonForm3 jf = new JSonForm3();
         							jf.setPaymentJson();
         						}
         					}
         					
         					if("0000".equals(b)) {
         						JetworkServerApplication.chk = true;
         						JetworkServerApplication.chk1 = true;
         						System.out.println("승인");
         						JSonForm jf = new JSonForm();
         						jf.setPaymentJson(sD);
         					
         					
         						
         					}else {
         						String sss = "";
         						for (int j = 7000; j < 10000; j++) {
         							sss = j+"";
         							if(sss.equals(b)) {
         								JetworkServerApplication.chk = false;
         								JetworkServerApplication.chk1 = true;
         								System.out.println("승인거절");
         								//JSonForm jf = new JSonForm();
         								//jf.setPaymentJson(sD);
         							}
         						}
         					}
         					
         					
//         					else if("7974".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);
//         					}else if("7979".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);
//         					}else if("8122".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);
//         					}else if("8000".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}else if("8035".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}else if("8521".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}
//         					else if("8520".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}
//         					else if("8522".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}else if("8523".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}else if("8524".equals(b)){
//         						UPrintServer.chk = false;
//         						UPrintServer.chk1 = true;
//         						System.out.println("승인거절");
//         						//JSonForm jf = new JSonForm();
//         						//jf.setPaymentJson(sD);8521  8035
//         					}

         				}
         	            

         	            
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
