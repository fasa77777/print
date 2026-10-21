package com.jetwork.model;

/**
 * 프린트누리 결제 기록 요청 (/jetwork/printnuri/payment). 소켓 서버가 단말기 결과를 이 형식으로 만들어 보낸다.
 */
public class PrintNuriPay {

	/** 단말기 ID (S03) */
	String terminalID;
	/** 결제 금액 (S10) */
	String price;
	/** 승인번호 (R09) */
	String payOK;
	/** 발급사 코드 (R12) */
	String craditName;
	/** 체크카드 여부 (R11, Y = 체크카드) */
	String payType;
	/** 컬러 여부 */
	String color;
	/** 매수 */
	String count;
	/** 용지 크기 */
	String paperSize;
	/** 교통카드 구분 (R33, TM = 티머니, EB = 캐시비) */
	String payTypeTM;
	/** 카드번호 (사용하지 않음) */
	String cardNum;

	public String getPayTypeTM() {
		return payTypeTM;
	}
	public void setPayTypeTM(String payTypeTM) {
		this.payTypeTM = payTypeTM;
	}
	public String getTerminalID() {
		return terminalID;
	}
	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getPayOK() {
		return payOK;
	}
	public void setPayOK(String payOK) {
		this.payOK = payOK;
	}
	public String getCraditName() {
		return craditName;
	}
	public void setCraditName(String craditName) {
		this.craditName = craditName;
	}
	public String getPayType() {
		return payType;
	}
	public void setPayType(String payType) {
		this.payType = payType;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getCount() {
		return count;
	}
	public void setCount(String count) {
		this.count = count;
	}
	public String getPaperSize() {
		return paperSize;
	}
	public void setPaperSize(String paperSize) {
		this.paperSize = paperSize;
	}
}
