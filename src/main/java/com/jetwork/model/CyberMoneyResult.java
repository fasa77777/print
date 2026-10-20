package com.jetwork.model;

/**
 * 사이버머니 결제 처리 결과.
 * 응답(PaymentResult)의 result / reason / text / balance 는 실서버 jar 와 같은 값을 쓴다.
 */
public class CyberMoneyResult {

	public enum Status {
		/** 결제 완료 */
		OK,
		/** 등록되지 않은 카드 (이번에 0원으로 새로 등록함) */
		NO_CARD,
		/** 잔액 부족 */
		INSUFFICIENT,
		/** 같은 카드·결제번호(cyberNo)로 이미 처리된 결제 */
		DUPLICATE,
		/** 금액 형식 오류나 DB 오류 */
		ERROR
	}

	private final Status status;
	/**
	 * 응답에 넣을 잔액.
	 * OK: 결제 후 잔액, INSUFFICIENT: 잔액 - 결제 금액 (음수, 실서버 jar 와 같음), 그 밖에는 0
	 */
	private final int balance;

	public CyberMoneyResult(Status status, int balance) {
		this.status = status;
		this.balance = balance;
	}

	public Status getStatus() {
		return status;
	}

	public int getBalance() {
		return balance;
	}

	/** 컨트롤러 응답(PaymentResult)으로 옮겨 담는다. */
	public PaymentResult toPaymentResult(String terminalID) {
		PaymentResult pr = new PaymentResult();
		pr.setTerminalID(terminalID);
		pr.setBalance(balance);
		switch (status) {
		case OK:
			pr.setResult("OK");
			pr.setText("정상결제 되었습니다.");
			pr.setReason("ok");
			break;
		case NO_CARD:
			pr.setResult("FAIL");
			pr.setText("등록되지않은 카드입니다.");
			pr.setReason("no card");
			break;
		case INSUFFICIENT:
			pr.setResult("FAIL");
			pr.setText("잔액부족 충전후 사용하세요");
			pr.setReason("no money");
			break;
		case DUPLICATE:
			// 실서버 jar 는 "등록되지 않은 카드"(no card)로 잘못 안내했다. 결과(FAIL)는 같다.
			pr.setResult("FAIL");
			pr.setText("이미 처리된 결제입니다.");
			pr.setReason("duplicate");
			break;
		default:
			// 실서버 jar 는 DB 오류 시 500 오류로 응답했다.
			pr.setResult("FAIL");
			pr.setText("결제 처리 중 오류가 발생했습니다.");
			pr.setReason("error");
			break;
		}
		return pr;
	}
}
