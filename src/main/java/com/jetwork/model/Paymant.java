/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

public class Paymant {
    String terminalID;
    String userID;
    String price;
    String cardCompanyName;
    String okNo;
    String colorYN;
    String paperSize;
    String printType;
    String result;

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

    public String getTerminalID() {
        return this.terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCardCompanyName() {
        return this.cardCompanyName;
    }

    public void setCardCompanyName(String cardCompanyName) {
        this.cardCompanyName = cardCompanyName;
    }

    public String getOkNo() {
        return this.okNo;
    }

    public void setOkNo(String okNo) {
        this.okNo = okNo;
    }

    public String getColorYN() {
        return this.colorYN;
    }

    public void setColorYN(String colorYN) {
        this.colorYN = colorYN;
    }

    public String getPaperSize() {
        return this.paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public String getPrintType() {
        return this.printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }
}
