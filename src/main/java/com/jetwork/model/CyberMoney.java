/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

public class CyberMoney {
    String userID;
    String userCard;
    String money;
    String BBALANCE = "0";
    String result;
    String ABALANCE = "0";

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getUserID() {
        return this.userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserCard() {
        return this.userCard;
    }

    public void setUserCard(String userCard) {
        this.userCard = userCard;
    }

    public String getMoney() {
        return this.money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getBBALANCE() {
        return this.BBALANCE;
    }

    public void setBBALANCE(String bBALANCE) {
        this.BBALANCE = bBALANCE;
    }

    public String getABALANCE() {
        return this.ABALANCE;
    }

    public void setABALANCE(String aBALANCE) {
        this.ABALANCE = aBALANCE;
    }
}
