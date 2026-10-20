package com.jetwork.model;

public class CyberMoney {
	String userID;
	String userCard;
	String money;
	String BBALANCE = "0";//이전사이버머니	
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
	public String getUserCard() {
		return userCard;
	}
	public void setUserCard(String userCard) {
		this.userCard = userCard;
	}
	public String getMoney() {
		return money;
	}
	public void setMoney(String money) {
		this.money = money;
	}
	public String getBBALANCE() {
		return BBALANCE;
	}
	public void setBBALANCE(String bBALANCE) {
		BBALANCE = bBALANCE;
	}
	public String getABALANCE() {
		return ABALANCE;
	}
	public void setABALANCE(String aBALANCE) {
		ABALANCE = aBALANCE;
	}
	String ABALANCE = "0";//이전사이버머니
}
