package com.jetwork.model;

/**
 * 사이버머니 잔액 조회 요청·응답 (/printnuri/postCyberMoney).
 */
public class CyberMoney {
	/** 사용자 ID. 컨트롤러는 이 값을 가맹점 조회용 단말기 ID 로 넘긴다 */
	String userID;
	/** 사이버머니 카드번호 */
	String userCard;
	/** 조회된 잔액 (응답) */
	String money;
	String BBALANCE = "0";//이전사이버머니	
	/** 처리 결과 (응답, 항상 OK) */
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
