package com.jetwork.model;

/**
 * 프린터 등록 응답 (예전 /printnuri/postPrinter. /stech 버전에는 이 API 가 없어 지금은 쓰지 않음).
 */
public class PrinterResult {
	/** 프린터 시리얼 번호 */
	String serialnumber;
	/** OK = 이미 등록된 프린터 갱신, FALSE = 새로 등록 */
	String result;
	public String getSerialnumber() {
		return serialnumber;
	}
	public void setSerialnumber(String serialnumber) {
		this.serialnumber = serialnumber;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
}
