package com.jetwork.model;

/**
 * jw_paper 테이블의 용지 가격 한 줄.
 */
public class PriceResult {
	
	/** 용지 크기 (paper_size) */
	String A4PaperSize;
	/** 장당 가격 (paper_price) */
	String A4PaperPrice;
	/** 컬러 여부 (paper_colorYN, Y = 컬러, N = 흑백) */
	String A4Color;
	/** 처리 결과 */
	String result;
	public String getA4PaperSize() {
		return A4PaperSize;
	}
	public void setA4PaperSize(String a4PaperSize) {
		A4PaperSize = a4PaperSize;
	}
	public String getA4PaperPrice() {
		return A4PaperPrice;
	}
	public void setA4PaperPrice(String a4PaperPrice) {
		A4PaperPrice = a4PaperPrice;
	}
	public String getA4Color() {
		return A4Color;
	}
	public void setA4Color(String a4Color) {
		A4Color = a4Color;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
	


}
