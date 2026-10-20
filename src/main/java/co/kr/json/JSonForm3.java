package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.json.simple.JSONObject;

import com.jetwork.bin.TerminalState;

/**
 * 티머니 결제 승인(단말기 작업 코드 04, R32 가 FF 가 아님)을 중계 서버의 /printnuri/payment 로 보낸다.
 * 승인번호 대신 현재 시각(yyyyMMddHHmmss)을 쓴다.
 * 단말기 ID 와 금액은 전문의 S03·S10 을 먼저 쓰고, 없으면 그 단말기의 상태(02 전문·Xerox 결제 요청)에서 가져온다.
 */
public class JSonForm3 {

	/**
	 * @param job 결과를 받기 직전에 꺼내 둔 진행 중인 작업 (없으면 null).
	 *            결과를 알리면 Xerox 결제 API 가 작업을 지우므로, 그 전에 꺼낸 값을 쓴다.
	 */
	public void setPaymentJson(TerminalState terminal, TerminalState.Job job, String[] sD) {
		Map<String, String> f = JSonForm.toMap(sD);
		String terminalID = f.containsKey("S03") && !f.get("S03").isEmpty() ? f.get("S03") : terminal.getTerminalID();
		String price = f.containsKey("S10") && !f.get("S10").isEmpty() ? f.get("S10") : String.valueOf(job == null ? 0 : job.getPrice());

		JSONObject data = new JSONObject();
		data.put("terminalID", terminalID);
		data.put("price", price);
		data.put("payOK", new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()));
		data.put("craditName", "");
		data.put("payType", "TM");
		data.put("payTypeTM", "TM");
		data.put("paperSize", "A4");
		data.put("color", job == null ? "" : job.getColor());
		data.put("count", job == null ? "" : job.getCount());

		new PServer().putPost(data);
	}
}
