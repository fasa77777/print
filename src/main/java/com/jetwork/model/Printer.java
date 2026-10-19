package com.jetwork.model;

public class Printer {
	String devid;
	String sitename;
	String desc;
	String errmsg;
	String modelname;
	int total;
	int mono;
	int color;
	int black;
	int cyan;
	int magenta;
	int yellow;
	String devicestatus;
	String printerstatus;
	String coverstatus;
	String serialnumber;
	String macaddr;
	public String getDevid() {
		return devid;
	}
	public void setDevid(String devid) {
		this.devid = devid;
	}
	public String getSitename() {
		return sitename;
	}
	public void setSitename(String sitename) {
		this.sitename = sitename;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getErrmsg() {
		return errmsg;
	}
	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}
	public String getModelname() {
		return modelname;
	}
	public void setModelname(String modelname) {
		this.modelname = modelname;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getMono() {
		return mono;
	}
	public void setMono(int mono) {
		this.mono = mono;
	}
	public int getColor() {
		return color;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public int getBlack() {
		return black;
	}
	public void setBlack(int black) {
		this.black = black;
	}
	public int getCyan() {
		return cyan;
	}
	public void setCyan(int cyan) {
		this.cyan = cyan;
	}
	public int getMagenta() {
		return magenta;
	}
	public void setMagenta(int magenta) {
		this.magenta = magenta;
	}
	public int getYellow() {
		return yellow;
	}
	public void setYellow(int yellow) {
		this.yellow = yellow;
	}
	public String getDevicestatus() {
		return devicestatus;
	}
	public void setDevicestatus(String devicestatus) {
		this.devicestatus = devicestatus;
	}
	public String getPrinterstatus() {
		return printerstatus;
	}
	public void setPrinterstatus(String printerstatus) {
		this.printerstatus = printerstatus;
	}
	public String getCoverstatus() {
		return coverstatus;
	}
	public void setCoverstatus(String coverstatus) {
		this.coverstatus = coverstatus;
	}
	public String getSerialnumber() {
		return serialnumber;
	}
	public void setSerialnumber(String serialnumber) {
		this.serialnumber = serialnumber;
	}
	public String getMacaddr() {
		return macaddr;
	}
	public void setMacaddr(String macaddr) {
		this.macaddr = macaddr;
	}
	
	
}
/**
 {

"devid": "11111",            # 장치ID
"sitename": "넥스파시스템", # 사이트명
"desc": "5층 개발팀#1",     # 장치설명 

"errmsg": "noerror",        # 복합기 상태 조회 에러메시지

                                        # 성공시 "noerror", 아래 항목 존재

                                        # 실패시 에러메시지, 아래 항목 모두 존재하지 않음.

"modelname": "HP ML-8000",  # 모델명, 실패시 "unknown" 

"total": 1234,          # 전체, 실패시 음수값 

"mono": 1200,           # 흑백, 실패시 음수값
"color": 34             # 컬러, 실패시 음수값 

    "black": 67,            # 검정색, 없을 수 있음, 실패시 음수값
    "cyan": 60,             # 청록색, 없을 수 있음, 실패시 음수값
    "magenta": 70,          # 보라색, 없을 수 있음, 실패시 음수값
    "yellow": 56            # 노란색, 없을 수 있음, 실패시 음수값 

    "devicestatus": "running",  # 장치상태
                                        # "unknown", "running", "warning", "testing", "down" 중 하나
   "printerstatus": "idle",    # 프린터상태
                                        # "other", "unknown", "idle", "printing", "warmup" 중 하나
   "coverstatus": "coverClosed",   # 커버상태
                                        # "unknown", "other", "doorOpen", "doorClosed", "interlockOpen", "interlockClosed" 중 하나 

"serialnumber": "PHXVHKDF67",   # 프린터 일련번호, 실패시 "unknown" 

macaddr":"b1:a2:fc:76:f4:b9"   # mac address, 여러개가 존재할 수 있음, 실패시 빈 배열 

}
  **/
