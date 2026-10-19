package com.jetwork.model;

/**
 * 가격 정책 응답 (/printnuri/printnuri/pricepolicy, /printnuri/printnuri/payment).
 */
public class PriceSendResult {
	/** A4 컬러 장당 가격 */
	String A4color;
	/** A4 흑백 장당 가격 */
	String A4mono;
	/** 처리 결과 (OK) */
	String result;
	public String getA4color() {
		return A4color;
	}
	public void setA4color(String a4color) {
		A4color = a4color;
	}
	public String getA4mono() {
		return A4mono;
	}
	public void setA4mono(String a4mono) {
		A4mono = a4mono;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
}
