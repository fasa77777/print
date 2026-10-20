/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.stech.Network;

import co.kr.json.JSonForm;
import co.kr.json.JSonForm1;
import co.kr.json.JSonForm2;
import co.kr.json.JSonForm3;
import co.kr.up.util.Bytes;
import co.kr.up.util.HexUtils;
import com.jetwork.bin.JetworkServerApplication;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class SocketServerDriver
implements Runnable {
    Socket socket = null;

    @Override
    public void run() {
        int port = Integer.parseInt("10020");
        try (ServerSocket serverSocket = new ServerSocket(port);){
            System.out.println("Server is listening on port " + port);
            while (true) {
                try {
                    byte[] b_command6;
                    this.socket = serverSocket.accept();
                    System.out.println("New Terminal connected  10020");
                    InputStream is = this.socket.getInputStream();
                    byte[] rcvData = new byte[1000];
                    int readByteCount = is.read(rcvData);
                    String message = new String(rcvData, 0, readByteCount, "UTF-8");
                    System.out.println("[[데이터 받기 성공]]: " + message);
                    System.out.println(this.socket.getInetAddress().toString());
                    String[] ss = this.socket.getInetAddress().toString().split("/");
                    System.out.println("[" + ss[1] + "]");
                    JetworkServerApplication.TerminalIP = ss[1];
                    String newString = new String(rcvData, "EUC-KR");
                    System.out.println(Arrays.toString(rcvData));
                    byte[] b_command = SocketServerDriver.getbytes(rcvData, 0, 1);
                    StringBuffer stBuffer = new StringBuffer();
                    int i = 0;
                    while (i < b_command.length) {
                        stBuffer.append(Bytes.toHexString(b_command[i]));
                        ++i;
                    }
                    System.out.println("1[" + stBuffer.toString() + "]");
                    byte[] b_command1 = SocketServerDriver.getbytes(rcvData, 1, 2);
                    stBuffer = new StringBuffer();
                    int i2 = 0;
                    while (i2 < b_command1.length) {
                        stBuffer.append(Bytes.toHexString(b_command1[i2]));
                        ++i2;
                    }
                    System.out.println("2[" + stBuffer.toString() + "]");
                    byte[] b_command2 = SocketServerDriver.getbytes(rcvData, 3, 1);
                    stBuffer = new StringBuffer();
                    int i3 = 0;
                    while (i3 < b_command2.length) {
                        stBuffer.append(Bytes.toHexString(b_command2[i3]));
                        ++i3;
                    }
                    System.out.println("3[" + stBuffer.toString() + "]");
                    byte[] b_command3 = SocketServerDriver.getbytes(rcvData, 4, 1);
                    stBuffer = new StringBuffer();
                    int i4 = 0;
                    while (i4 < b_command3.length) {
                        stBuffer.append(Bytes.toHexString(b_command3[i4]));
                        ++i4;
                    }
                    System.out.println("4[" + stBuffer.toString() + "]");
                    byte[] b_command4 = SocketServerDriver.getbytes(rcvData, 5, 1);
                    stBuffer = new StringBuffer();
                    int i5 = 0;
                    while (i5 < b_command4.length) {
                        stBuffer.append(Bytes.toHexString(b_command4[i5]));
                        ++i5;
                    }
                    System.out.println("5[" + stBuffer.toString() + "]");
                    byte[] b_command5 = SocketServerDriver.getbytes(rcvData, 6, 1);
                    stBuffer = new StringBuffer();
                    int i6 = 0;
                    while (i6 < b_command5.length) {
                        stBuffer.append(Bytes.toHexString(b_command5[i6]));
                        ++i6;
                    }
                    System.out.println("6[" + stBuffer.toString() + "]");
                    String poli = stBuffer.toString();
                    String ck = stBuffer.toString().trim();
                    System.out.println("ck[" + ck + "]");
                    if ("02".equals(ck)) {
                        b_command6 = SocketServerDriver.getbytes(rcvData, 44, 10);
                        stBuffer = new StringBuffer();
                        int i7 = 0;
                        while (i7 < b_command6.length) {
                            stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b_command6[i7])));
                            ++i7;
                        }
                        System.out.println("[" + stBuffer.toString().trim() + "]");
                        JetworkServerApplication.terID = stBuffer.toString().trim();
                        if (JetworkServerApplication.chk2) {
                            JSonForm1 js1 = new JSonForm1();
                            js1.setPolicyJson(stBuffer.toString().trim());
                            System.out.println("=========================");
                            System.out.println("=========================");
                        }
                    } else if ("09".equals(ck)) {
                        b_command6 = SocketServerDriver.getbytes(rcvData, 6, 2000);
                        stBuffer = new StringBuffer();
                        int i8 = 0;
                        while (i8 < b_command6.length) {
                            stBuffer.append(Bytes.UnitoStr(HexUtils.toHexString(b_command6[i8])));
                            ++i8;
                        }
                        System.out.println("ck========" + ck);
                    } else if ("06".equals(ck)) {
                        System.out.println("사이버머니");
                        b_command6 = SocketServerDriver.getbytes(rcvData, 6, 1);
                        stBuffer = new StringBuffer();
                        int i9 = 0;
                        while (i9 < b_command6.length) {
                            stBuffer.append(HexUtils.toHexString(b_command6[i9]));
                            ++i9;
                        }
                        System.out.println("ISO-8859-1 -> EUC-KR        : " + stBuffer.toString());
                        byte[] b_command7 = SocketServerDriver.getbytes(rcvData, 8, 4);
                        stBuffer = new StringBuffer();
                        int i10 = 0;
                        while (i10 < b_command7.length) {
                            stBuffer.append(HexUtils.toHexString(b_command7[i10]));
                            ++i10;
                        }
                        System.out.println("ISO-8859-1 -> EUC-KR        : " + stBuffer.toString());
                        JSonForm2 jf = new JSonForm2();
                        jf.setPaymentJson(stBuffer.toString());
                    } else if ("04".equals(ck)) {
                        System.out.println("[" + message + "]");
                        String ss1 = message.trim();
                        System.out.println(ss1.matches(".*CANCEL.*"));
                        if (ss1.matches(".*CANCEL.*")) {
                            JetworkServerApplication.chk = false;
                            JetworkServerApplication.chk1 = true;
                            System.out.println("취소");
                        }
                        String[] sD = message.split(";");
                        System.out.println("sD == " + sD.toString());
                        String b = "";
                        String c = "";
                        String tm = "";
                        int i11 = 0;
                        while (i11 < sD.length) {
                            Object jf;
                            System.out.println(String.valueOf(sD[i11]) + "[" + i11 + "]");
                            b = sD[i11].replaceAll("R04=", "");
                            String R32 = sD[i11].substring(0, 3);
                            c = sD[i11].replaceAll("R32=", "");
                            System.out.println("R32=" + c.trim().length());
                            System.out.println("R32 stringToHex=" + this.stringToHex(c.trim()).substring(0, 2));
                            if ("R32".equals(R32)) {
                                System.out.println("=====================R32=========================" + this.stringToHex(c.trim()).substring(0, 2));
                                if ("FF".equals(this.stringToHex(c.trim()).substring(0, 2))) {
                                    System.out.println("R32 포함");
                                    JetworkServerApplication.chk = false;
                                    JetworkServerApplication.chk1 = true;
                                    System.out.println("티머니 승인 거절");
                                } else {
                                    System.out.println("티머니승인");
                                    JetworkServerApplication.chk = true;
                                    JetworkServerApplication.chk1 = true;
                                    System.out.println("승인");
                                    jf = new JSonForm3();
                                    ((JSonForm3)jf).setPaymentJson();
                                }
                            }
                            if ("0000".equals(b)) {
                                JetworkServerApplication.chk = true;
                                JetworkServerApplication.chk1 = true;
                                System.out.println("승인");
                                jf = new JSonForm();
                                ((JSonForm)jf).setPaymentJson(sD);
                            } else {
                                String sss = "";
                                int j = 7000;
                                while (j < 10000) {
                                    sss = String.valueOf(j);
                                    if (sss.equals(b)) {
                                        JetworkServerApplication.chk = false;
                                        JetworkServerApplication.chk1 = true;
                                        System.out.println("승인거절");
                                    }
                                    ++j;
                                }
                            }
                            ++i11;
                        }
                    }
                    this.socket.close();
                }
                catch (Exception e) {
                    System.out.println("Socket close" + e);
                    this.socket.close();
                }
            }
        }
        catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }
    }

    public String stringToHex(String s) {
        String result = "";
        int i = 0;
        while (i < s.length()) {
            result = String.valueOf(result) + String.format("%02X ", s.charAt(i));
            ++i;
        }
        return result;
    }

    public static final byte[] getbytes(byte[] src, int offset, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(src, offset, dest, 0, length);
        return dest;
    }
}
