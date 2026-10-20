package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;

import com.jetwork.bin.TerminalState;

/**
 * 사이버머니 카드 태그(단말기 작업 코드 06)를 받으면, 그 단말기에서 진행 중인 출력 작업 금액으로
 * 중계 서버의 /kyocera/postCyberMoneyPayment 에 결제를 요청한다. 결제번호는 현재 시각(yyyyMMddHHmmss)이다.
 */
public class JSonForm2 {

	public void setPaymentJson(TerminalState terminal, String card) {
		JSONObject data = new JSONObject();
		data.put("terminalID", terminal.getTerminalID());
		data.put("totalAmount", terminal.getPrice());
		data.put("cyberNo", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		data.put("userCard", card);
		data.put("jobType", "print");
		data.put("paperSize", "A4");
		data.put("color", terminal.getColor());
		data.put("totalPages", terminal.getCount());

		new PrintCyberPOST().putPost(data, terminal);
	}
}
