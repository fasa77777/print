package com.jetwork.bin;

/**
 * 결제 단말기로 보내는 요청 전문의 공통 틀.
 *
 * 전문 구조: STX(0x02) + 본문 길이(2바이트) + 순번(1) + CMD(0xFB) + GCD(0x14) + 작업 코드(1) + 본문 + ETX(0x03) + CRC(2바이트)
 * 본문과 작업 코드는 하위 클래스(예: ReqCharge)가 정한다.
 */
public abstract class ReqMessage {
	private byte stx = 0x02;
	private short cmd = 0xFB;
	/** 전문 순번. 보낼 때마다 1씩 늘고 255 다음은 1 이다 */
	private static short cnt = 0;
	private byte gcd = 0x14;
	private byte etx = 0x03;

	/** 헤더·본문·꼬리·CRC 를 붙여 단말기로 보낼 전문 바이트를 만든다. */
	public byte[] assemble() {
		byte[] header = new byte[7];
		byte[] body = assembleBody();
		byte[] tail = new byte[1];
		byte[] crc = new byte[2];
		
		byte[] message = new byte[header.length+body.length+tail.length+crc.length];

		
		
		header[0] = stx;
		header[1] = ((byte)(body.length >> 8));
		header[2] = (byte)body.length;
		if(++cnt > 255) cnt = 1;
		header[3] = (byte)cnt;
		header[4] = (byte)cmd;
		header[5] = gcd;
		header[6] = getJobCode();
		tail[0] = etx;
		
		
		
		
		System.arraycopy(header, 0, message, 0, header.length);
		System.arraycopy(body, 0, message, header.length, body.length);
		System.arraycopy(tail, 0, message, header.length+body.length, tail.length);
		
		setCrc(message);
				
		
//		printMessage(message);
		
		return message;
	}
	
	/** 디버깅용: 전문을 16진수로 출력한다. */
	private void printMessage(byte[] strBytes) {
		System.out.print("<<");
		
		for(byte b : strBytes) {
			System.out.print("[");
			System.out.print(String.format("%02X", b&0xff));
			System.out.print("]");
		}
		
		System.out.println(">>");
		
	}
	
	/** 전문 끝 2바이트에 CRC-16 값을 채우고, 길이 필드도 실제 길이로 다시 쓴다. */
	private void setCrc(byte[] data) {
	    int SEED = 32773;
	    
	    int crc = 65535;
	    
	    int length = data.length;
	    int len = data.length - 4;
	    if (len > 0)
	    {
	      data[1] = ((byte)(len >> 8));
	      data[2] = ((byte)len);
	      for (int j = 1; j < length - 2; j++)
	      {
	        int temp = (byte)(data[j] & 0xFF);
	        for (int i = 0; i < 8; i++)
	        {
	          if ((crc & 0x1 ^ temp & 0x1) != 0) {
	            crc = crc >> 1 ^ SEED;
	          } else {
	            crc >>= 1;
	          }
	          temp >>= 1;
	        }
	      }
	      crc ^= 0xFFFFFFFF;
	      int temp = crc;
	      crc = crc << 8 | temp >> 8 & 0xFF;
	      
	      data[(length - 2)] = ((byte)(crc >> 8));
	      data[(length - 1)] = ((byte)crc);
	    }
		
	}

	/** 전문 본문 (예: "S01=A1;S02=40;S09=00;S10=금액;") */
	public abstract byte[] assembleBody();
	/** 작업 코드 (헤더 7번째 바이트) */
	public abstract byte getJobCode();


	
}
