package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.TerminalState;

/**
 * 사이버머니 카드 태그(단말기 작업 코드 06)를 받으면, 그 단말기에서 진행 중인 출력 작업 금액으로
 * 중계 서버의 /kyocera/postCyberMoneyPayment 에 결제를 요청한다. 결제번호는 현재 시각(yyyyMMddHHmmss)이다.
 */
public class JSonForm2 {

	private static final Logger logger = LoggerFactory.getLogger(JSonForm2.class);

	public void setPaymentJson(TerminalState terminal, String card) {
		// 진행 중인 출력 작업(Xerox 결제 요청)이 없는데 카드를 태그하면 금액이 0원이다.
		// 예전에는 0원 사이버머니 결제가 그대로 기록되었다.
		if (terminal.getPrice() <= 0) {
			logger.info("진행 중인 출력 작업이 없어 사이버머니 결제 요청을 보내지 않음 {}", terminal);
			return;
		}
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
