package com.jetwork.model;

public class Paymant {
	String terminalID;
	
	String userID;
	String price;
	//신용카드사이름,티머니,캐시비,사이버머니구분
	String cardCompanyName;
	String okNo;
	//컬럭흑백여부
	String colorYN;
	//용지규격
	String paperSize;
	//프린트타입(복사,프린터,스캔,팩스)
	String printType;
	String result;
	
	

	
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
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
	public String getCardCompanyName() {
		return cardCompanyName;
	}
	public void setCardCompanyName(String cardCompanyName) {
		this.cardCompanyName = cardCompanyName;
	}
	public String getOkNo() {
		return okNo;
	}
	public void setOkNo(String okNo) {
		this.okNo = okNo;
	}
	public String getColorYN() {
		return colorYN;
	}
	public void setColorYN(String colorYN) {
		this.colorYN = colorYN;
	}
	public String getPaperSize() {
		return paperSize;
	}
	public void setPaperSize(String paperSize) {
		this.paperSize = paperSize;
	}
	public String getPrintType() {
		return printType;
	}
	public void setPrintType(String printType) {
		this.printType = printType;
	}
	
	
}
