package com.jetwork.model;

/** 카드·교통카드 결제 기록 결과. 단말기 응답은 실서버 jar 와 같이 항상 OK 다. */
public enum RecordStatus {
	/** 기록함 */
	OK,
	/** 오늘 같은 승인번호로 이미 기록되어 있어 다시 기록하지 않음 */
	DUPLICATE,
	/** DB 오류 (서버 로그 확인) */
	ERROR;

	/** 카드 결제 API 응답 (PaymentResult). 실서버 jar 와 같이 항상 OK */
	public PaymentResult toPaymentResult(String terminalID) {
		PaymentResult pr = new PaymentResult();
		pr.setTerminalID(terminalID);
		pr.setResult("OK");
		return pr;
	}

	/** 프린트누리 결제 API 응답 (PriceSendResult). 실서버 jar 와 같이 항상 OK */
	public PriceSendResult toPriceSendResult() {
		PriceSendResult ps = new PriceSendResult();
		ps.setResult("OK");
		return ps;
	}
}
