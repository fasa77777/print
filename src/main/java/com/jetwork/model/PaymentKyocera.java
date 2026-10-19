package com.jetwork.model;

/**
 * 교세라 사이버머니 결제 요청 (/printnuri/kyocera/postCyberMoneyPayment, 현재 비활성).
 */
public class PaymentKyocera {
	/** 보안 ID */
	String SecureID;
	/** 보안 키 */
	String SecureKey;
	
	/** 명령 (pay 등) */
	String cmd;
	/** 컬러 여부 (1 = 컬러, 2 = 흑백) */
	String color;
	/** 부수 */
	String copies;
	/** 양면 여부 */
	String duplex;
	/** 작업 종류 (print, copy 등) */
	String jobType;
	/** MAC 주소 */
	String mac;
	/** 모아찍기 수 */
	String nUp;
	/** 용지 크기 */
	String paperSize;
	/** 스캔 매수 */
	String scanCount;
	/** 단말기 시리얼 */
	String serial;
	/** 단말기 ID */
	String terminalID;
	/** 타임아웃 */
	String timeOut;
	/** 결제 금액 (따옴표가 붙어 올 수 있음) */
	String totalAmount;
	/** 총 매수 */
	String totalPages;
	
	/** 처리 결과 */
	String result;
	
	/** 사이버머니 카드번호 */
	String userCard;
	
	/** 사이버머니 결제번호 (중복 결제 확인용) */
	String cyberNo;

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

	public String getUserCard() {
		return userCard;
	}

	public void setUserCard(String userCard) {
		this.userCard = userCard;
	}

	public String getCyberNo() {
		return cyberNo;
	}

	public void setCyberNo(String cyberNo) {
		this.cyberNo = cyberNo;
	}
	
	
}
