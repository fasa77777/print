package com.jetwork.model;

import com.fasterxml.jackson.annotation.JsonSetter;

public class PaymentKicc {
/**
  {"R01":"P","R02":"A","R03":"0000","R04":"0000","R05":"031","R06":"0000","R07":"2006272309186","R08":"","R09":"99230915    ",
  "R10":"","R11":"N","R12":"031","R13":"국민은행","R14":"166127521   ","R15":" Ｚī           ",
  "R16":"d","R17":"","R18":"00000000","R19":"TEST ","R20":"272341031896","R21":"","R22":"N","R23":"379183","R24":"","R25":"",
  "R26":"","R27":"","R28":"","R29":"","R30":"","SecureID":"328","SecureKey":"52347227924328","cmd":"pay","color":false,"copies":1,
  "duplex":false,"jobType":"Copy","mac":"30:cd:a7:c8:fa:90","nUp":1,"paperSize":"A4","scanCount":1,"serial":"0BY8BJEH500015W",
  "terminalID":"0788888","timeOut":70,"totalAmount":10,"totalPages":1, "userCard":"53C797A2"} 
 * 
 */
	String R01;
	String R02;
	String R03;
	String R04;
	String R05;
	String R06;
	String R07;
	String R08;
	String R09; //승인번호
	String R10; 
	String R11;// C:체크 카드 N:일반 카드
	String R12;
	String R13; //발급사명
	String R14;
	String R15;
	String R16;
	String R17;
	String R18;
	String R19;
	String R20;
	String R21;
	String R22;
	String R23;
	String R24;
	String R25;
	String R26;
	String R27;
	String R28;
	String R29;
	String R30;
	String R31;
	String R32;
	String R33; //TM : 티머니, EB : 캐시비 구분
	String R34;

	
	
	
	String SecureID;
	String SecureKey;
	
	String cmd;
	String color;
	String copies;
	String duplex;
	String jobType;
	String mac;
	String nUp;
	String paperSize;
	String scanCount;
	String serial;
	String terminalID;
	String timeOut;
	String totalAmount;
	String totalPages;
	
	String result;
	
	String userCard;
	
	String cyberNo;
	
	

	public String getCyberNo() {
		return cyberNo;
	}

	public void setCyberNo(String cyberNo) {
		this.cyberNo = cyberNo;
	}

	public String getUserCard() {
		return userCard;
	}

	public void setUserCard(String userCard) {
		this.userCard = userCard;
	}

	public String getR01() {
		return R01;
	}

	@JsonSetter("R01")
	public void setR01(String r01) {
		R01 = r01;
	}

	public String getR02() {
		return R02;
	}
	
	@JsonSetter("R02")
	public void setR02(String r02) {
		R02 = r02;
	}

	public String getR03() {
		return R03;
	}
	@JsonSetter("R03")
	public void setR03(String r03) {
		R03 = r03;
	}

	public String getR04() {
		return R04;
	}
	@JsonSetter("R04")
	public void setR04(String r04) {
		R04 = r04;
	}

	public String getR05() {
		return R05;
	}
	@JsonSetter("R05")
	public void setR05(String r05) {
		R05 = r05;
	}

	public String getR06() {
		return R06;
	}
	@JsonSetter("R06")
	public void setR06(String r06) {
		R06 = r06;
	}

	public String getR07() {
		return R07;
	}
	@JsonSetter("R07")
	public void setR07(String r07) {
		R07 = r07;
	}

	public String getR08() {
		return R08;
	}
	@JsonSetter("R08")
	public void setR08(String r08) {
		R08 = r08;
	}

	public String getR09() {
		return R09;
	}
	@JsonSetter("R09")
	public void setR09(String r09) {
		R09 = r09;
	}

	public String getR10() {
		return R10;
	}
	@JsonSetter("R10")
	public void setR10(String r10) {
		R10 = r10;
	}

	public String getR11() {
		return R11;
	}
	@JsonSetter("R11")
	public void setR11(String r11) {
		R11 = r11;
	}

	public String getR12() {
		return R12;
	}
	@JsonSetter("R12")
	public void setR12(String r12) {
		R12 = r12;
	}

	public String getR13() {
		return R13;
	}
	@JsonSetter("R13")
	public void setR13(String r13) {
		R13 = r13;
	}
	
	public String getR14() {
		return R14;
	}
	@JsonSetter("R14")
	public void setR14(String r14) {
		R14 = r14;
	}

	public String getR15() {
		return R15;
	}
	@JsonSetter("R15")
	public void setR15(String r15) {
		R15 = r15;
	}

	public String getR16() {
		return R16;
	}
	@JsonSetter("R16")
	public void setR16(String r16) {
		R16 = r16;
	}

	public String getR17() {
		return R17;
	}
	@JsonSetter("R17")
	public void setR17(String r17) {
		R17 = r17;
	}

	public String getR18() {
		return R18;
	}
	@JsonSetter("R18")
	public void setR18(String r18) {
		R18 = r18;
	}

	public String getR19() {
		return R19;
	}
	@JsonSetter("R19")
	public void setR19(String r19) {
		R19 = r19;
	}

	public String getR20() {
		return R20;
	}
	@JsonSetter("R20")
	public void setR20(String r20) {
		R20 = r20;
	}

	public String getR21() {
		return R21;
	}
	@JsonSetter("R21")
	public void setR21(String r21) {
		R21 = r21;
	}

	public String getR22() {
		return R22;
	}
	@JsonSetter("R22")
	public void setR22(String r22) {
		R22 = r22;
	}

	public String getR23() {
		return R23;
	}
	@JsonSetter("R23")
	public void setR23(String r23) {
		R23 = r23;
	}

	public String getR24() {
		return R24;
	}
	@JsonSetter("R24")
	public void setR24(String r24) {
		R24 = r24;
	}

	public String getR25() {
		return R25;
	}
	@JsonSetter("R25")
	public void setR25(String r25) {
		R25 = r25;
	}

	public String getR26() {
		return R26;
	}
	@JsonSetter("R26")
	public void setR26(String r26) {
		R26 = r26;
	}

	public String getR27() {
		return R27;
	}
	@JsonSetter("R27")
	public void setR27(String r27) {
		R27 = r27;
	}

	public String getR28() {
		return R28;
	}
	@JsonSetter("R28")
	public void setR28(String r28) {
		R28 = r28;
	}

	public String getR29() {
		return R29;
	}
	@JsonSetter("R29")
	public void setR29(String r29) {
		R29 = r29;
	}
	
	public String getR30() {
		return R30;
	}
	@JsonSetter("R30")
	public void setR30(String r30) {
		R30 = r30;
	}

	public String getR31() {
		return R31;
	}
	@JsonSetter("R31")
	public void setR31(String r31) {
		R31 = r31;
	}

	public String getR32() {
		return R32;
	}
	@JsonSetter("R32")
	public void setR32(String r32) {
		R32 = r32;
	}
	
	public String getR33() {
		return R33;
	}
	@JsonSetter("R33")
	public void setR33(String r33) {
		R33 = r33;
	}

	public String getR34() {
		return R34;
	}
	@JsonSetter("R34")
	public void setR34(String r34) {
		R34 = r34;
	}

	public String getSecureID() {
		return SecureID;
	}

	public void setSecureID(String secureID) {
		SecureID = secureID;
	}

	public String getSecureKey() {
		return SecureKey;
	}

	public void setSecureKey(String secureKey) {
		SecureKey = secureKey;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getCopies() {
		return copies;
	}

	public void setCopies(String copies) {
		this.copies = copies;
	}

	public String getDuplex() {
		return duplex;
	}

	public void setDuplex(String duplex) {
		this.duplex = duplex;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getnUp() {
		return nUp;
	}

	public void setnUp(String nUp) {
		this.nUp = nUp;
	}

	public String getPaperSize() {
		return paperSize;
	}

	public void setPaperSize(String paperSize) {
		this.paperSize = paperSize;
	}

	public String getScanCount() {
		return scanCount;
	}

	public void setScanCount(String scanCount) {
		this.scanCount = scanCount;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getTerminalID() {
		return terminalID;
	}

	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}

	public String getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(String timeOut) {
		this.timeOut = timeOut;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(String totalPages) {
		this.totalPages = totalPages;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	

	
	
}
