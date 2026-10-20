/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

public class UserDetail {
    public String terminalID = "";
    public String userID = "";
    public String totalEMoney = "0";
    public String cBBalance = "";
    public String nBBalance = "";
    public String date = "";

    public String getTerminalID() {
        return this.terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public String getUserID() {
        return this.userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getTotalEMoney() {
        return this.totalEMoney;
    }

    public void setTotalEMoney(String totalEMoney) {
        this.totalEMoney = totalEMoney;
    }

    public String getcBBalance() {
        return this.cBBalance;
    }

    public void setcBBalance(String cBBalance) {
        this.cBBalance = cBBalance;
    }

    public String getnBBalance() {
        return this.nBBalance;
    }

    public void setnBBalance(String nBBalance) {
        this.nBBalance = nBBalance;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
