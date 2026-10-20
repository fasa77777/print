/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

import com.fasterxml.jackson.annotation.JsonSetter;

public class PaymentCreaditKicc {
    String R09;
    String R10;
    String R12;
    String R13;
    String R33;
    String color;
    String copies;
    String duplex;
    String jobType;
    String nUp;
    String paperSize;
    String scanCount;
    String terminalID;
    String totalAmount;
    String totalPages;

    public String getR09() {
        return this.R09;
    }

    @JsonSetter(value="R09")
    public void setR09(String r09) {
        this.R09 = r09;
    }

    public String getR10() {
        return this.R10;
    }

    @JsonSetter(value="R10")
    public void setR10(String r10) {
        this.R10 = r10;
    }

    public String getR12() {
        return this.R12;
    }

    @JsonSetter(value="R12")
    public void setR12(String r12) {
        this.R12 = r12;
    }

    public String getR13() {
        return this.R13;
    }

    @JsonSetter(value="R13")
    public void setR13(String r13) {
        this.R13 = r13;
    }

    public String getR33() {
        return this.R33;
    }

    @JsonSetter(value="R33")
    public void setR33(String r33) {
        this.R33 = r33;
    }

    public String getColor() {
        return this.color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCopies() {
        return this.copies;
    }

    public void setCopies(String copies) {
        this.copies = copies;
    }

    public String getDuplex() {
        return this.duplex;
    }

    public void setDuplex(String duplex) {
        this.duplex = duplex;
    }

    public String getJobType() {
        return this.jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getnUp() {
        return this.nUp;
    }

    public void setnUp(String nUp) {
        this.nUp = nUp;
    }

    public String getPaperSize() {
        return this.paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public String getScanCount() {
        return this.scanCount;
    }

    public void setScanCount(String scanCount) {
        this.scanCount = scanCount;
    }

    public String getTerminalID() {
        return this.terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public String getTotalAmount() {
        return this.totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(String totalPages) {
        this.totalPages = totalPages;
    }
}
