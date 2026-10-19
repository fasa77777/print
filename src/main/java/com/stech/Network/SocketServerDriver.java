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

import com.jetwork.bin.JetworkServerApplication;

import co.kr.json.JSonForm;
import co.kr.json.JSonForm1;
import co.kr.json.JSonForm2;
import co.kr.json.JSonForm3;
import co.kr.up.util.Bytes;
import co.kr.up.util.HexUtils;
//드라이버와 소켓연결
public class SocketServerDriver  implements Runnable {
	
	Socket socket  = null;
	@SuppressWarnings("static-access")
	public void run() {
		  
        int port = Integer.parseInt("10020");
 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Server is listening on port " + port);
 
            while (true) {
            	try {
            		 socket = serverSocket.accept();
                     System.out.println("New Terminal connected  10020");
                     InputStream is = socket.getInputStream();

                     byte[]rcvData  = new byte[1000];

                     int readByteCount = is.read(rcvData );

                     String message = new String(rcvData , 0, readByteCount, "UTF-8");

                     System.out.println("[[데이터 받기 성공]]: " + message);

                     System.out.println(socket.getInetAddress().toString());
          			String [] ss = socket.getInetAddress().toString().split("/");
          			System.out.println("["+ss[1]+"]");
          			JetworkServerApplication.TerminalIP = ss[1];
          			String newString = new String(rcvData, "EUC-KR");
         			System.out.println(Arrays.toString(rcvData));
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
        	 
        	        String ck= stBuffer.toString().trim();
        	        
        	        System.out.println("ck["+ck+"]");
        	        
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
         	            System.out.println("ISO-8859-1 -> EUC-KR        : " +stBuffer.toString());
         	            
         	            JSonForm2 jf = new JSonForm2();
         				jf.setPaymentJson(stBuffer.toString());
         	            
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
        	        
        	        
        	        
                     socket.close();
            	}catch (Exception e) {
					// TODO: handle exception
            		System.out.println("Socket close"+e);
            		socket.close();
				}
               
            }
 
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } 
	}
	
	  public String stringToHex(String s) {
		    String result = "";

		    for (int i = 0; i < s.length(); i++) {
		      result += String.format("%02X ", (int) s.charAt(i));
		    }

		    return result;
	  }
	  
		public static final byte[] getbytes(byte src[], int offset, int length){
	        byte dest[] = new byte[length];
	        System.arraycopy(src, offset, dest, 0, length);
	        return dest;
	    }

}
