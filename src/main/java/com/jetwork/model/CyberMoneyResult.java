package com.jetwork.model;

/** 사이버머니 결제 처리 결과. */
public class CyberMoneyResult {

	public enum Status {
		/** 결제 완료 */
		OK,
		/** 등록되지 않은 카드 (이번에 0원으로 새로 등록함) */
		NO_CARD,
		/** 잔액 부족 */
		INSUFFICIENT,
		/** 같은 결제번호로 이미 처리된 결제 */
		DUPLICATE,
		/** jw_terminal 에 없는 단말기 ID */
		NO_TERMINAL,
		/** 금액 형식 오류나 DB 오류 */
		ERROR
	}

	private final Status status;
	/** 처리 후 잔액 (잔액 부족이면 현재 잔액) */
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
		pr.setBalance(String.valueOf(balance));
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
		case NO_TERMINAL:
			pr.setResult("FAIL");
			pr.setText(RecordStatus.NO_TERMINAL_TEXT);
			pr.setReason("no terminal");
			break;
		case DUPLICATE:
			pr.setResult("FAIL");
			pr.setText("이미 처리된 결제입니다.");
			pr.setReason("duplicate");
			break;
		default:
			pr.setResult("FAIL");
			pr.setText("결제 처리 중 오류가 발생했습니다.");
			pr.setReason("error");
			break;
		}
		return pr;
	}
}
