/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.model;

public class Terminal {
    String terminalID;
    String key;
    String license;
    String serial;
    String result;
    String black;
    String blue;
    String red;
    String yello;
    String terminalSerial;
    // 기본값을 두지 않는다. 요청에 없으면 null → 버전 갱신을 하지 않는다. (실서버 jar 는 "1.1.1" / "1052" 를 기본값으로 저장했다)
    String agentVersion;
    String terminalVersion;

    public String getTerminalVersion() {
        return this.terminalVersion;
    }

    public void setTerminalVersion(String terminalVersion) {
        this.terminalVersion = terminalVersion;
    }

    public String getAgentVersion() {
        return this.agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getTerminalSerial() {
        return this.terminalSerial;
    }

    public void setTerminalSerial(String terminalSerial) {
        this.terminalSerial = terminalSerial;
    }

    public String getBlack() {
        return this.black;
    }

    public void setBlack(String black) {
        this.black = black;
    }

    public String getBlue() {
        return this.blue;
    }

    public void setBlue(String blue) {
        this.blue = blue;
    }

    public String getRed() {
        return this.red;
    }

    public void setRed(String red) {
        this.red = red;
    }

    public String getYello() {
        return this.yello;
    }

    public void setYello(String yello) {
        this.yello = yello;
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTerminalID() {
        return this.terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLicense() {
        return this.license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}
