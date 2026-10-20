package com.jetwork.model;

/**
 * 단말기 등록 확인 응답 (/stech/postTerminal).
 */
public class PayRresult {

		/** 단말기 ID */
		String terminalID;
		/** OK = 등록된 단말기, FALSE = 미등록, ERROR = DB 오류 */
		String result;
//		String text;
//		String reason;
		
		public String getTerminalID() {
			return terminalID;
		}
		public void setTerminalID(String terminalID) {
			this.terminalID = terminalID;
		}
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}
//		public String getText() {
//			return text;
//		}
//		public void setText(String text) {
//			this.text = text;
//		}
//		public String getReason() {
//			return reason;
//		}
//		public void setReason(String reason) {
//			this.reason = reason;
//		}
		
		

	}
