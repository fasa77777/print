package co.kr.json;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사이버머니 결제번호(cyberNo) 만들기. 형식은 예전과 같은 yyyyMMddHHmmss (14자리).
 *
 * 예전에는 현재 시각을 그대로 써서, 같은 카드로 1초 안에 두 번 결제하면 두 번째가
 * "같은 결제번호로 이미 처리된 결제"(중복)로 거절됐다.
 * 이제 같은 초에 이미 번호를 냈으면 다음 초 번호를 내서, 이 서버가 내는 번호는 절대 겹치지 않는다.
 * (결제가 1초에 여러 건 몰리면 번호가 실제 시각보다 몇 초 앞설 수 있다)
 */
public final class CyberNo {

	/** 마지막으로 낸 번호의 시각 (초 단위, epoch) */
	private static final AtomicLong LAST_SECOND = new AtomicLong();

	private CyberNo() {
	}

	public static String next() {
		long now = System.currentTimeMillis() / 1000;
		while (true) {
			long last = LAST_SECOND.get();
			long candidate = Math.max(now, last + 1);
			if (LAST_SECOND.compareAndSet(last, candidate)) {
				return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(candidate * 1000));
			}
		}
	}
}
