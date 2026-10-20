package com.jetwork.model;

/** 카드·교통카드 결제 기록 결과. */
public enum RecordStatus {
	/** 기록함 */
	OK,
	/** 오늘 같은 승인번호로 이미 기록되어 있어 다시 기록하지 않음 (단말기에는 OK 로 응답) */
	DUPLICATE,
	/** jw_terminal 에 없는 단말기 ID 라서 기록하지 않음 (FAIL 로 응답) */
	NO_TERMINAL,
	/** DB 오류 (예전처럼 단말기에는 OK 로 응답, 서버 로그 확인) */
	ERROR;

	/** 단말기 ID 가 없어 거절할 때 단말기에 보여 줄 문구 */
	public static final String NO_TERMINAL_TEXT = "등록되지 않은 단말기입니다.";

	/** 카드 결제 API 응답 (PaymentResult) */
	public PaymentResult toPaymentResult(String terminalID) {
		PaymentResult pr = new PaymentResult();
		pr.setTerminalID(terminalID);
		if (this == NO_TERMINAL) {
			pr.setResult("FAIL");
			pr.setText(NO_TERMINAL_TEXT);
			pr.setReason("no terminal");
		} else {
			pr.setResult("OK");
		}
		return pr;
	}

	/** 프린트누리 결제 API 응답 (PriceSendResult) */
	public PriceSendResult toPriceSendResult() {
		PriceSendResult ps = new PriceSendResult();
		ps.setResult(this == NO_TERMINAL ? "FAIL" : "OK");
		return ps;
	}
}
