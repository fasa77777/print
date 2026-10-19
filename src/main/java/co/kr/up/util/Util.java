package co.kr.up.util;

import java.net.Socket;
import java.util.Vector;

public class Util {
	

	public static int THREADMAX = 300;
	
	public static Vector<Socket> terminal_socket = new Vector<Socket>();
	public static Vector<Object> thread = new Vector<Object>();
	public static String migrateByteDatas(byte[] datas) {
		StringBuffer sb = new StringBuffer();
		if (datas != null) {
			sb.append("bytedatas is : ");
			for (int i = 0; i < datas.length; i++) {
				sb.append("|");
				sb.append(datas[i]);
			}
			sb.append("|");
		}
		return sb.toString();
	}
	
	
	  public static String stringToHex(String s) {
	    String result = "";

	    for (int i = 0; i < s.length(); i++) {
	      result += String.format("%02X ", (int) s.charAt(i));
	    }

	    return result;
	  }


	  // ��� ���λ� "0x" ���̴� ����
	  public static String stringToHex0x(String s) {
	    String result = "";

	    for (int i = 0; i < s.length(); i++) {
	      result += String.format("0x%02X ", (int) s.charAt(i));
	    }

	    return result;
	  }
	  
	  
	  //zero padding
	  public static String padLeftZero(String numberString, int n) {
	      int number = Integer.parseInt(numberString, 16);
	      return String.format("%0" + n + "x", number);
	   }
	  
	  //space padding
	public static String padRightSpace(String s, int n){
		return String.format("%1$-" + n + "s", s);
	}
	
	public static String padLeftSpace(String s, int n){
		return String.format("%1$-" + n + "x", s);
	}


	  public static String StrNum(String s){
	      if (s == null)
	          return null;
	      else if (s.length() == 0)
	          return "";

	      int i = 0;
	      int len = s.length();

	      for (i = 0; i < len; i++) {
	          if ( s.charAt(i) != ' ' && s.charAt(i) != '0' )
	              break;
	      }
	      if (i >= len)
	           return "";
	      return s.substring(i, len);
	  }

}
