/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package co.kr.up.util;

import java.util.StringTokenizer;

public class Bytes {
    public static byte toByte(String value, byte defaultValue) {
        try {
            return Byte.parseByte(value);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static Byte toByteObject(String value, Byte defaultValue) {
        try {
            return new Byte(value);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static int unsignedByte(byte b) {
        return b & 0xFF;
    }

    public static int toInt(byte[] src, int srcPos) {
        int dword = 0;
        int i = 0;
        while (i < 4) {
            dword = (dword << 8) + (src[i + srcPos] & 0xFF);
            ++i;
        }
        return dword;
    }

    public static int toInt(byte[] src) {
        return Bytes.toInt(src, 0);
    }

    public static long toLong(byte[] src, int srcPos) {
        long qword = 0L;
        int i = 0;
        while (i < 8) {
            qword = (qword << 8) + (long)(src[i + srcPos] & 0xFF);
            ++i;
        }
        return qword;
    }

    public static long toLong(byte[] src) {
        return Bytes.toLong(src, 0);
    }

    public static void toBytes(int value, byte[] dest, int destPos) {
        int i = 0;
        while (i < 4) {
            dest[i + destPos] = (byte)(value >> (7 - i) * 8);
            ++i;
        }
    }

    public static byte[] toBytes(int value) {
        byte[] dest = new byte[4];
        Bytes.toBytes(value, dest, 0);
        return dest;
    }

    public static void toBytes(long value, byte[] dest, int destPos) {
        int i = 0;
        while (i < 8) {
            dest[i + destPos] = (byte)(value >> (7 - i) * 8);
            ++i;
        }
    }

    public static byte[] toBytes(long value) {
        byte[] dest = new byte[8];
        Bytes.toBytes(value, dest, 0);
        return dest;
    }

    public static byte[] toBytes(String digits, int radix) throws IllegalArgumentException, NumberFormatException {
        if (digits == null) {
            return null;
        }
        if (radix != 16 && radix != 10 && radix != 8) {
            throw new IllegalArgumentException("For input radix: \"" + radix + "\"");
        }
        int divLen = radix == 16 ? 2 : 3;
        int length = digits.length();
        if (length % divLen == 1) {
            throw new IllegalArgumentException("For input string: \"" + digits + "\"");
        }
        byte[] bytes = new byte[length /= divLen];
        int i = 0;
        while (i < length) {
            int index = i * divLen;
            bytes[i] = (byte)Short.parseShort(digits.substring(index, index + divLen), radix);
            ++i;
        }
        return bytes;
    }

    public static byte[] hexStringToBytes(String digits) throws IllegalArgumentException, NumberFormatException {
        if (digits == null) {
            return null;
        }
        int length = digits.length();
        if (length % 2 == 1) {
            throw new IllegalArgumentException("For input string: \"" + digits + "\"");
        }
        byte[] bytes = new byte[length /= 2];
        int i = 0;
        while (i < length) {
            int index = i * 2;
            bytes[i] = (byte)Short.parseShort(digits.substring(index, index + 2), 16);
            ++i;
        }
        return bytes;
    }

    public static String toHexString(byte b) {
        StringBuffer result = new StringBuffer(3);
        result.append(Integer.toString((b & 0xF0) >> 4, 16));
        result.append(Integer.toString(b & 0xF, 16));
        return result.toString();
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        byte[] byArray = bytes;
        int n = bytes.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            result.append(Integer.toString((b & 0xF0) >> 4, 16));
            result.append(Integer.toString(b & 0xF, 16));
            ++n2;
        }
        return result.toString();
    }

    public static boolean equals(byte[] array1, byte[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }
        int i = 0;
        while (i < array1.length) {
            if (array1[i] != array2[i]) {
                return false;
            }
            ++i;
        }
        return true;
    }

    public static String UnitoStr(String uni) {
        String str = "";
        StringTokenizer str1 = new StringTokenizer(uni, "\\/u");
        while (str1.hasMoreTokens()) {
            String str2 = str1.nextToken();
            int i = Integer.parseInt(str2, 16);
            str = String.valueOf(str) + (char)i;
        }
        return str;
    }
}
