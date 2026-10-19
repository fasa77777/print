package co.kr.up.util;

/** 로그에 개인정보가 그대로 남지 않도록 가리는 도구. */
public final class Mask {

	private Mask() {
	}

	/** 카드번호는 앞 4자리와 뒤 4자리만 남기고 가린다. 예) 1234********5678 */
	public static String card(String card) {
		if (card == null) {
			return null;
		}
		String c = card.trim();
		if (c.length() <= 8) {
			return c.length() <= 4 ? "****" : c.substring(0, 2) + "****";
		}
		StringBuilder sb = new StringBuilder(c.substring(0, 4));
		for (int i = 4; i < c.length() - 4; i++) {
			sb.append('*');
		}
		return sb.append(c.substring(c.length() - 4)).toString();
	}
}
