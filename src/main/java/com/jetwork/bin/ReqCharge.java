package com.jetwork.bin;

/**
 * 결제 단말기에 보내는 결제 요청 전문 (작업 코드 4).
 * 본문의 S01=A1 은 "신용 또는 전자화폐 지불결제", S10 은 결제 금액이다. 필드 설명은 아래 주석 참고.
 */
public class ReqCharge extends ReqMessage {
/*	
    % S01 = A1 :: 
        % "A1":ALL 지불결제 (신용 또는 전자화폐)
        % "T1":전자화폐 지불결제 "T2":전자화폐 지불취소
        % "D1":신용승인 "D4":신용당일취소/반품환불 
        % "B1":현금승인(공제) "B2":현금취소(공제) "B3":자진발급승인(공제) "B4":자진발급취소(공제)
        % "C1":수표조회
        % "CI":카드정보 요청 ( 마스킹 카드번호 요청 ) -> Rf Flag (S05) + Masked Card Number(S07)
        % "LA":마지막 자료 재전송
        % "CL":마직막 자료 삭제
        % "CC":통신취소 ( 마지막 거래 통신 취소 )
    % S02 = 40 :: 
    % S09 = 00 :: Installment months (할부 개월수)
    % S10 = Variable :: Price
    % S23 = POS가 관리하는 번호로, 단말기는 응답전문에 그대로 리턴함. (결과전문 확인용으로 사용할 수 있다)
    % S30 = "1" : 키인거래 홗성화	
*/
	
	/** 결제 금액 (원) */
	private int price;
	
	
	public ReqCharge(int price) {
		super();
		this.price = price;
	}
	

	@Override
	public byte getJobCode() {

		return 4;
	}


	@Override
	public byte[] assembleBody() {
		String bodyMessage = "S01=A1;S02=40;S09=00;S10="+price+";";

		
		
		return bodyMessage.getBytes();
	}


	

}

