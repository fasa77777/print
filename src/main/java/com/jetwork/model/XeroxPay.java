package com.jetwork.model;
//{"cmd":"pay","jobType":"Copy","totalAmount":10,"totalPages":1}"color":false,"paperSize":"A4"}
/**
 * Xerox 출력 결제 요청 (/stech/xerox/xeroxPayment).
 */
public class XeroxPay {
	/** 명령 (pay) */
	String cmd;
	/** 작업 종류 (Copy, Print 등) */
	String jobType;
	/** 결제 금액 */
	String totalAmount;
	/** 총 매수 */
	String totalPages;
	
	/** 컬러 여부 */
	String color;
	/** 용지 크기 */
	String paperSize;
	
	
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
	public String getPaperSize() {
		return paperSize;
	}
	public void setPaperSize(String paperSize) {
		this.paperSize = paperSize;
	}
	public String getJobType() {
		return jobType;
	}
	public void setJobType(String jobType) {
		this.jobType = jobType;
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
	
	
}
