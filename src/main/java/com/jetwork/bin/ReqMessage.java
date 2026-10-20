/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

public abstract class ReqMessage {
    private byte stx = (byte)2;
    private short cmd = (short)251;
    private static short cnt = 0;
    private byte gcd = (byte)20;
    private byte etx = (byte)3;

    public byte[] assemble() {
        byte[] header = new byte[7];
        byte[] body = this.assembleBody();
        byte[] tail = new byte[1];
        byte[] crc = new byte[2];
        byte[] message = new byte[header.length + body.length + tail.length + crc.length];
        header[0] = this.stx;
        header[1] = (byte)(body.length >> 8);
        header[2] = (byte)body.length;
        if ((cnt = (short)(cnt + 1)) > 255) {
            cnt = 1;
        }
        header[3] = (byte)cnt;
        header[4] = (byte)this.cmd;
        header[5] = this.gcd;
        header[6] = this.getJobCode();
        tail[0] = this.etx;
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);
        System.arraycopy(tail, 0, message, header.length + body.length, tail.length);
        this.setCrc(message);
        return message;
    }

    private void printMessage(byte[] strBytes) {
        System.out.print("<<");
        byte[] byArray = strBytes;
        int n = strBytes.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            System.out.print("[");
            System.out.print(String.format("%02X", b & 0xFF));
            System.out.print("]");
            ++n2;
        }
        System.out.println(">>");
    }

    private void setCrc(byte[] data) {
        int SEED = 32773;
        int crc = 65535;
        int length = data.length;
        int len = data.length - 4;
        if (len > 0) {
            data[1] = (byte)(len >> 8);
            data[2] = (byte)len;
            int j = 1;
            while (j < length - 2) {
                int temp = data[j] & 0xFF;
                int i = 0;
                while (i < 8) {
                    crc = (crc & 1 ^ temp & 1) != 0 ? crc >> 1 ^ SEED : (crc >>= 1);
                    temp >>= 1;
                    ++i;
                }
                ++j;
            }
            int temp = crc ^= 0xFFFFFFFF;
            crc = crc << 8 | temp >> 8 & 0xFF;
            data[length - 2] = (byte)(crc >> 8);
            data[length - 1] = (byte)crc;
        }
    }

    public abstract byte[] assembleBody();

    public abstract byte getJobCode();
}
