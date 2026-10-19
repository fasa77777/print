package com.jetwork.bin;

public abstract class ReqMessage {
	private byte stx = 0x02;
	private short cmd = 0xFB;
	private static short cnt = 0;
	private byte gcd = 0x14;
	private byte etx = 0x03;

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
	
	private void printMessage(byte[] strBytes) {
		System.out.print("<<");
		
		for(byte b : strBytes) {
			System.out.print("[");
			System.out.print(String.format("%02X", b&0xff));
			System.out.print("]");
		}
		
		System.out.println(">>");
		
	}
	
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

	public abstract byte[] assembleBody();
	public abstract byte getJobCode();


	
}
