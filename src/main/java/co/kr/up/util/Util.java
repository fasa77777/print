/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.up.util;

import java.net.Socket;
import java.util.Vector;

public class Util {
    public static int THREADMAX = 300;
    public static Vector<Socket> terminal_socket = new Vector();
    public static Vector<Object> thread = new Vector();

    public static String migrateByteDatas(byte[] datas) {
        StringBuffer sb = new StringBuffer();
        if (datas != null) {
            sb.append("bytedatas is : ");
            int i = 0;
            while (i < datas.length) {
                sb.append("|");
                sb.append(datas[i]);
                ++i;
            }
            sb.append("|");
        }
        return sb.toString();
    }

    public static String stringToHex(String s) {
        String result = "";
        int i = 0;
        while (i < s.length()) {
            result = String.valueOf(result) + String.format("%02X ", s.charAt(i));
            ++i;
        }
        return result;
    }

    public static String stringToHex0x(String s) {
        String result = "";
        int i = 0;
        while (i < s.length()) {
            result = String.valueOf(result) + String.format("0x%02X ", s.charAt(i));
            ++i;
        }
        return result;
    }

    public static String padLeftZero(String numberString, int n) {
        int number = Integer.parseInt(numberString, 16);
        return String.format("%0" + n + "x", number);
    }

    public static String padRightSpace(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeftSpace(String s, int n) {
        return String.format("%1$-" + n + "x", s);
    }

    public static String StrNum(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0) {
            return "";
        }
        int i = 0;
        int len = s.length();
        i = 0;
        while (i < len) {
            if (s.charAt(i) != ' ' && s.charAt(i) != '0') break;
            ++i;
        }
        if (i >= len) {
            return "";
        }
        return s.substring(i, len);
    }
}
