package co.kr.up.util;


/**
 * ���ϸ� : HexUtils.java
 * �ۼ��� : 2013. 07. 23
 * �ۼ��� : son
 * ���ϼ��� : 
 */
public class HexUtils {

	private static final char hexchar[] = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	/**
	 * byte �����͸� Hex String�� ��ȯ
	 * 
	 * @param b Hex String���� ��ȯ�� byte ������
	 * @return Hex String
	 */
	public static String toHexString(byte b) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(hexchar[( b >>> 4) & 0xf]);
		buffer.append(hexchar[b & 0xf]);
		
		return buffer.toString();
	}
	
	/**
	 * char �����͸� Hex String�� ��ȯ
	 * 
	 * @param c Hex String���� ��ȯ�� char ������
	 * @return HexString
	 */
	public static String toHexString(char c) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(hexchar[( c >>> 12) & 0xf]);
		buffer.append(hexchar[( c >>> 8) & 0xf]);
		buffer.append(hexchar[( c >>> 4) & 0xf]);
		buffer.append(hexchar[c & 0xf]);
		
		return buffer.toString();
	}
	
	/**
	 * short �����͸� Hex String�� ��ȯ
	 * 
	 * @param s Hex String���� ��ȯ�� short ������
	 * @return HexString
	 */
	public static String toHexString(short s) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(hexchar[( s >>> 12) & 0xf]);
		buffer.append(hexchar[( s >>> 8) & 0xf]);
		buffer.append(hexchar[( s >>> 4) & 0xf]);
		buffer.append(hexchar[s & 0xf]);
		
		return buffer.toString();
	}
	
	/**
	 * int �����͸� Hex String�� ��ȯ
	 * 
	 * @param i Hex String���� ��ȯ�� int ������
	 * @return HexString
	 */
	public static String toHexString(int i) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(hexchar[( i >>> 28) & 0xf]);
		buffer.append(hexchar[( i >>> 24) & 0xf]);
		buffer.append(hexchar[( i >>> 20) & 0xf]);
		buffer.append(hexchar[( i >>> 16) & 0xf]);
		buffer.append(hexchar[( i >>> 12) & 0xf]);
		buffer.append(hexchar[( i >>> 8) & 0xf]);
		buffer.append(hexchar[( i >>> 4) & 0xf]);
		buffer.append(hexchar[i & 0xf]);
		
		return buffer.toString();
	}
	
	/**
	 * long �����͸� Hex String�� ��ȯ
	 * 
	 * @param l Hex String���� ��ȯ�� long ������
	 * @return HexString
	 */
	public static String toHexString(long l){
		StringBuffer buffer = new StringBuffer();

		buffer.append(hexchar[(int)(( l >>> 60) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 56) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 52) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 48) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 44) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 40) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 36) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 32) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 28) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 24) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 20) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 16) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 12) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 8) & 0xf)]);
		buffer.append(hexchar[(int)(( l >>> 4) & 0xf)]);
		buffer.append(hexchar[(int)(l & 0xf)]);
		
		return buffer.toString();
	}
	
	/**
	 * Hex String�� byte�� ��ȯ
	 * 
	 * @param hexStr
	 * @return byte
	 */
	public static byte toByte(String hexStr){
		byte result = 0;
		String hex = hexStr.toUpperCase();
		
		for(int i = 0 ; i < hex.length() ; i++){
			char c = hex.charAt(hex.length() - i - 1);
			byte b = toByte(c);
			result |= (b & 0x0f) << (i*4); 
		}
		
		return result;
	}
	
	/**
	 * Hex String�� char ������ ��ȯ
	 * 
	 * @param hexStr
	 * @return char
	 */
	public static char toChar(String hexStr){
		char result = 0;
		String hex = hexStr.toUpperCase();
		
		for(int i = 0 ; i < hex.length() ; i++){
			char c = hex.charAt(hex.length() - i - 1);
			byte b = toByte(c);
			result |= (b & 0x0f) << (i*4); 
		}
		
		return result;
	}
	
	/**
	 * Hex String�� short���� ��ȯ
	 * 
	 * @param hexStr
	 * @return short ��
	 */
	public static short toShort(String hexStr){
		short result = 0;
		String hex = hexStr.toUpperCase();
		
		for(int i = 0 ; i < hex.length() ; i++){
			char c = hex.charAt(hex.length() - i - 1);
			byte b = toByte(c);
			result |= (b & 0x0f) << (i*4); 
		}
		
		return result;
	}
	
	/**
	 * Hex String�� int�� ��ȯ
	 * 
	 * @param hexStr
	 * @return int ��
	 */
	public static int toInt(String hexStr){
		int result = 0;
		String hex = hexStr.toUpperCase();

		for(int i = 0 ; i < hex.length() ; i++){
			char c = hex.charAt(hex.length() - i - 1);
			byte b = toByte(c);
			result |= (b & 0x0f) << (i*4); 
		}
		
		return result;
	}
	
	/**
	 * Hex String�� long���� ��ȯ
	 * 
	 * @param hexStr
	 * @return long ��
	 */
	public static long toLong(String hexStr){
		long result = 0;
		String hex = hexStr.toUpperCase();

		for(int i = 0 ; i < hex.length() ; i++){
			char c = hex.charAt(hex.length() - i - 1);
			byte b = toByte(c);
			result |= (long)(b & 0x0f) << (i*4); 
		}
		
		return result;
	}
	
	private static byte toByte(char c){
		switch(c){
		case '0':
			return 0;
		case '1':
			return 1;
		case '2':
			return 2;
		case '3':
			return 3;
		case '4':
			return 4;
		case '5':
			return 5;
		case '6':
			return 6;
		case '7':
			return 7;
		case '8':
			return 8;
		case '9':
			return 9;
		case 'A':
			return 10;
		case 'B':
			return 11;
		case 'C':
			return 12;
		case 'D':
			return 13;
		case 'E':
			return 14;
		case 'F':
			return 15;
		}
		
		return 0;
	}
	
	public static String toHexString(byte buf[]){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            sb.append(Integer.toHexString(0x0100 + (buf[i] & 0x00FF)).substring(1));
        }
        return sb.toString();
    }
	
	
	public static int StringHexToInt(String str){
	   byte[] b2=new byte[str.length()];

		int temp=0;
		  for(int i=0;i<str.length();i++)
		  {
		   if(str.substring(i,i+1).equals("A") || str.substring(i,i+1).equals("a"))
		    b2[i]=(byte)0x0A; 
		   else if(str.substring(i,i+1).equals("B") || str.substring(i,i+1).equals("b"))
		    b2[i]=(byte)0x0B; 
		   else if(str.substring(i,i+1).equals("C") || str.substring(i,i+1).equals("c"))
		    b2[i]=(byte)0x0C;
		   else if(str.substring(i,i+1).equals("D") || str.substring(i,i+1).equals("d"))
		    b2[i]=(byte)0x0D;
		   else if(str.substring(i,i+1).equals("E") || str.substring(i,i+1).equals("e"))
		    b2[i]=(byte)0x0E;
		   else if(str.substring(i,i+1).equals("F") || str.substring(i,i+1).equals("f"))
		    b2[i]=(byte)0x0F;
		   else
		    b2[i]=(byte)(Integer.parseInt(str.substring(i,i+1)) & 0x0F);
	
		   temp=temp+(b2[i] << 4*(str.length()-i));
	
		  }
	   return temp;
	 }

	    public static byte toHex(char c) {   
		       byte b = 0x00;   
		    
		        if (c == '0') {   
		            b = 0x00;   
		        } else if (c == '1') {   
		            b = 0x01;   
		        } else if (c == '2') {   
		            b = 0x02;   
		        } else if (c == '3') {   
		           b = 0x03;   
		       } else if (c == '4') {   
		           b = 0x04;   
		        } else if (c == '5') {   
		            b = 0x05;   
		        } else if (c == '6') {   
		            b = 0x06;   
		        } else if (c == '7') {   
		            b = 0x07;   
		        } else if (c == '8') {   
		           b = 0x08;   
		        } else if (c == '9') {   
		            b = 0x09;   
		        } else if (c == 'a' || c == 'A') {   
		            b = 0x0A;   
		        } else if (c == 'b' || c == 'B') {   
		            b = 0x0B;   
		       } else if (c == 'c' || c == 'C') {   
		            b = 0x0C;   
		        } else if (c == 'd' || c == 'D') {   
		            b = 0x0D;   
		        } else if (c == 'e' || c == 'E') {   
		            b = 0x0E;   
		        } else if (c == 'f' || c == 'F') {   
		            b = 0x0F;   
		        }   
		    
		        return b;   
		    }  


}