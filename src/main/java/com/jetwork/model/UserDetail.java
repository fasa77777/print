package com.jetwork.model;

public class UserDetail {
	public String terminalID = "";
	public String userID  = ""; //user ID
	public String totalEMoney  = "0"; // 
	public String cBBalance  = ""; // 
	public String nBBalance  = ""; // 
	public String date  = ""; 
	
	
	public String getTerminalID() {
		return terminalID;
	}
	public void setTerminalID(String terminalID) {
		this.terminalID = terminalID;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getTotalEMoney() {
		return totalEMoney;
	}
	public void setTotalEMoney(String totalEMoney) {
		this.totalEMoney = totalEMoney;
	}
	public String getcBBalance() {
		return cBBalance;
	}
	public void setcBBalance(String cBBalance) {
		this.cBBalance = cBBalance;
	}
	public String getnBBalance() {
		return nBBalance;
	}
	public void setnBBalance(String nBBalance) {
		this.nBBalance = nBBalance;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}

}
