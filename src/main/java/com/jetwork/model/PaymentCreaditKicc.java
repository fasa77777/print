package com.jetwork.model;

import com.fasterxml.jackson.annotation.JsonSetter;
//신용거래
public class PaymentCreaditKicc {
	/**
	  {"R09":"99230915    ",
	  "R10":"C:체크 카드 N:일반 카드","R12":"발급사코드","R13":"국민은행","R33":"TM:티머니 , “B:캐시비","color":false,"copies":1,
	  "duplex":false,"jobType":"Copy","nUp":1,"paperSize":"A4","scanCount":1,
	  "terminalID":"0788888","totalAmount":10,"totalPages":1} 
	 * 
	 */

		String R09; //승인번호
		String R10; 

		String R12;
		String R13; //발급사명

		String R33; //TM : 티머니, EB : 캐시비 구분


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
			return R09;
		}
		@JsonSetter("R09")
		public void setR09(String r09) {
			R09 = r09;
		}
		public String getR10() {
			return R10;
		}
		@JsonSetter("R10")
		public void setR10(String r10) {
			R10 = r10;
		}
		
		public String getR12() {
			return R12;
		}
		@JsonSetter("R12")
		public void setR12(String r12) {
			R12 = r12;
		}
		public String getR13() {
			return R13;
		}
		@JsonSetter("R13")
		public void setR13(String r13) {
			R13 = r13;
		}
		public String getR33() {
			return R33;
		}
		@JsonSetter("R33")
		public void setR33(String r33) {
			R33 = r33;
		}
		public String getColor() {
			return color;
		}
		public void setColor(String color) {
			this.color = color;
		}
		public String getCopies() {
			return copies;
		}
		public void setCopies(String copies) {
			this.copies = copies;
		}
		public String getDuplex() {
			return duplex;
		}
		public void setDuplex(String duplex) {
			this.duplex = duplex;
		}
		public String getJobType() {
			return jobType;
		}
		public void setJobType(String jobType) {
			this.jobType = jobType;
		}
		public String getnUp() {
			return nUp;
		}
		public void setnUp(String nUp) {
			this.nUp = nUp;
		}
		public String getPaperSize() {
			return paperSize;
		}
		public void setPaperSize(String paperSize) {
			this.paperSize = paperSize;
		}
		public String getScanCount() {
			return scanCount;
		}
		public void setScanCount(String scanCount) {
			this.scanCount = scanCount;
		}
		public String getTerminalID() {
			return terminalID;
		}
		public void setTerminalID(String terminalID) {
			this.terminalID = terminalID;
		}
		public String getTotalAmount() {
			return totalAmount;
		}
		public void setTotalAmount(String totalAmount) {
			this.totalAmount = totalAmount;
		}
		public String getTotalPages() {
			return totalPages;
		}
		public void setTotalPages(String totalPages) {
			this.totalPages = totalPages;
		}



		
	}
