package com.jetwork.model;

public class PrintNuriPay {
//	data.put("terminalID", sD[2]); //S03
//	data.put("price", sD[9]); //S10
//	data.put("payOK", sD[2]);//R09
//	data.put("craditName", "");//R13
//	data.put("payType","");//R11
//	data.put("paperSize", "");
//	data.put("color", "");
//	data.put("count", "");
//	ata.put("payTypeTM","");//R33
	
	String terminalID;
	String price;
	String payOK;
	String craditName;
	String payType;
	String color;
	String count;
	String paperSize;
	String payTypeTM;
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
