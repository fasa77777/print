/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.up.util;

public class HexUtils {
    private static final char[] hexchar = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String toHexString(byte b) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(hexchar[b >>> 4 & 0xF]);
        buffer.append(hexchar[b & 0xF]);
        return buffer.toString();
    }

    public static String toHexString(char c) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(hexchar[c >>> 12 & 0xF]);
        buffer.append(hexchar[c >>> 8 & 0xF]);
        buffer.append(hexchar[c >>> 4 & 0xF]);
        buffer.append(hexchar[c & 0xF]);
        return buffer.toString();
    }

    public static String toHexString(short s) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(hexchar[s >>> 12 & 0xF]);
        buffer.append(hexchar[s >>> 8 & 0xF]);
        buffer.append(hexchar[s >>> 4 & 0xF]);
        buffer.append(hexchar[s & 0xF]);
        return buffer.toString();
    }

    public static String toHexString(int i) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(hexchar[i >>> 28 & 0xF]);
        buffer.append(hexchar[i >>> 24 & 0xF]);
        buffer.append(hexchar[i >>> 20 & 0xF]);
        buffer.append(hexchar[i >>> 16 & 0xF]);
        buffer.append(hexchar[i >>> 12 & 0xF]);
        buffer.append(hexchar[i >>> 8 & 0xF]);
        buffer.append(hexchar[i >>> 4 & 0xF]);
        buffer.append(hexchar[i & 0xF]);
        return buffer.toString();
    }

    public static String toHexString(long l) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(hexchar[(int)(l >>> 60 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 56 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 52 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 48 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 44 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 40 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 36 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 32 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 28 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 24 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 20 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 16 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 12 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 8 & 0xFL)]);
        buffer.append(hexchar[(int)(l >>> 4 & 0xFL)]);
        buffer.append(hexchar[(int)(l & 0xFL)]);
        return buffer.toString();
    }

    public static byte toByte(String hexStr) {
        byte result = 0;
        String hex = hexStr.toUpperCase();
        int i = 0;
        while (i < hex.length()) {
            char c = hex.charAt(hex.length() - i - 1);
            byte b = HexUtils.toByte(c);
            result = (byte)(result | (b & 0xF) << i * 4);
            ++i;
        }
        return result;
    }

    public static char toChar(String hexStr) {
        char result = '\u0000';
        String hex = hexStr.toUpperCase();
        int i = 0;
        while (i < hex.length()) {
            char c = hex.charAt(hex.length() - i - 1);
            byte b = HexUtils.toByte(c);
            result = (char)(result | (b & 0xF) << i * 4);
            ++i;
        }
        return result;
    }

    public static short toShort(String hexStr) {
        short result = 0;
        String hex = hexStr.toUpperCase();
        int i = 0;
        while (i < hex.length()) {
            char c = hex.charAt(hex.length() - i - 1);
            byte b = HexUtils.toByte(c);
            result = (short)(result | (b & 0xF) << i * 4);
            ++i;
        }
        return result;
    }

    public static int toInt(String hexStr) {
        int result = 0;
        String hex = hexStr.toUpperCase();
        int i = 0;
        while (i < hex.length()) {
            char c = hex.charAt(hex.length() - i - 1);
            byte b = HexUtils.toByte(c);
            result |= (b & 0xF) << i * 4;
            ++i;
        }
        return result;
    }

    public static long toLong(String hexStr) {
        long result = 0L;
        String hex = hexStr.toUpperCase();
        int i = 0;
        while (i < hex.length()) {
            char c = hex.charAt(hex.length() - i - 1);
            byte b = HexUtils.toByte(c);
            result |= (long)(b & 0xF) << i * 4;
            ++i;
        }
        return result;
    }

    private static byte toByte(char c) {
        switch (c) {
            case '0': {
                return 0;
            }
            case '1': {
                return 1;
            }
            case '2': {
                return 2;
            }
            case '3': {
                return 3;
            }
            case '4': {
                return 4;
            }
            case '5': {
                return 5;
            }
            case '6': {
                return 6;
            }
            case '7': {
                return 7;
            }
            case '8': {
                return 8;
            }
            case '9': {
                return 9;
            }
            case 'A': {
                return 10;
            }
            case 'B': {
                return 11;
            }
            case 'C': {
                return 12;
            }
            case 'D': {
                return 13;
            }
            case 'E': {
                return 14;
            }
            case 'F': {
                return 15;
            }
        }
        return 0;
    }

    public static String toHexString(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (i < buf.length) {
            sb.append(Integer.toHexString(256 + (buf[i] & 0xFF)).substring(1));
            ++i;
        }
        return sb.toString();
    }

    public static int StringHexToInt(String str) {
        byte[] b2 = new byte[str.length()];
        int temp = 0;
        int i = 0;
        while (i < str.length()) {
            b2[i] = str.substring(i, i + 1).equals("A") || str.substring(i, i + 1).equals("a") ? 10 : (str.substring(i, i + 1).equals("B") || str.substring(i, i + 1).equals("b") ? 11 : (str.substring(i, i + 1).equals("C") || str.substring(i, i + 1).equals("c") ? 12 : (str.substring(i, i + 1).equals("D") || str.substring(i, i + 1).equals("d") ? 13 : (str.substring(i, i + 1).equals("E") || str.substring(i, i + 1).equals("e") ? 14 : (str.substring(i, i + 1).equals("F") || str.substring(i, i + 1).equals("f") ? 15 : (byte)(Integer.parseInt(str.substring(i, i + 1)) & 0xF))))));
            temp += b2[i] << 4 * (str.length() - i);
            ++i;
        }
        return temp;
    }

    public static byte toHex(char c) {
        int b = 0;
        if (c == '0') {
            b = 0;
        } else if (c == '1') {
            b = 1;
        } else if (c == '2') {
            b = 2;
        } else if (c == '3') {
            b = 3;
        } else if (c == '4') {
            b = 4;
        } else if (c == '5') {
            b = 5;
        } else if (c == '6') {
            b = 6;
        } else if (c == '7') {
            b = 7;
        } else if (c == '8') {
            b = 8;
        } else if (c == '9') {
            b = 9;
        } else if (c == 'a' || c == 'A') {
            b = 10;
        } else if (c == 'b' || c == 'B') {
            b = 11;
        } else if (c == 'c' || c == 'C') {
            b = 12;
        } else if (c == 'd' || c == 'D') {
            b = 13;
        } else if (c == 'e' || c == 'E') {
            b = 14;
        } else if (c == 'f' || c == 'F') {
            b = 15;
        }
        return (byte)b;
    }
}
