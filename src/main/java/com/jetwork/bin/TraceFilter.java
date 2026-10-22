package com.jetwork.bin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import co.kr.up.util.Mask;

/**
 * HTTP 전체 로그 (로거 이름 jetwork.trace.http, DEBUG 일 때만).
 *
 * 요청마다 번호(#n)를 붙여 시작 줄과 끝 줄을 남긴다:
 *   시작: 메서드, 주소, 보낸 쪽 IP, 요청 본문
 *   끝:   응답 코드, 걸린 시간, 응답 본문
 * 같은 요청에서 나온 다른 로그(SQL 등)에도 같은 번호가 붙는다 (로그 패턴의 %X{req}).
 * 카드번호(userCard, cardNumber, cardNo 값)는 가린다.
 * 디버깅 페이지가 켜져 있으면 로그와 별도로 최근 요청 기록(RequestRecorder)에도 남긴다.
 * 둘 다 꺼져 있으면(기본) 아무것도 하지 않고 다음 단계로 넘긴다.
 */
@Component
public class TraceFilter extends OncePerRequestFilter {

	static final Logger log = LoggerFactory.getLogger("jetwork.trace.http");

	/** 본문은 이 길이까지만 남긴다 */
	static final int MAX_BODY = 8000;
	private static final Pattern CARD = Pattern.compile("(\"(?:userCard|cardNumber|cardNo)\"\\s*:\\s*\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);
	private static final AtomicLong SEQ = new AtomicLong();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		boolean logOn = log.isDebugEnabled();
		boolean record = RequestRecorder.enabled() && !request.getRequestURI().startsWith(DebugController.PATH);
		if (!logOn && !record) {
			chain.doFilter(request, response);
			return;
		}
		long seq = SEQ.incrementAndGet();
		MDC.put("req", "#" + seq);
		ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, MAX_BODY); // 본문은 MAX_BODY 까지만 메모리에 담는다
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
		RequestRecorder.Record rec = record ? RequestRecorder.start(seq, request.getMethod(), uri(request), request.getRemoteAddr()) : null;
		long t0 = System.nanoTime();
		if (logOn) {
			log.debug("요청 시작 {} {} from {}", request.getMethod(), uri(request), request.getRemoteAddr());
		}
		try {
			chain.doFilter(req, res);
		} catch (IOException | ServletException | RuntimeException e) {
			if (logOn) {
				log.debug("요청 처리 중 예외: {}", e.toString());
			}
			RequestRecorder.sql("요청 처리 중 예외: " + e);
			throw e;
		} finally {
			long ms = (System.nanoTime() - t0) / 1000000;
			String reqBody = body(req.getContentAsByteArray());
			String resBody = body(res.getContentAsByteArray());
			if (logOn) {
				log.debug("요청 본문: {}", reqBody);
				log.debug("응답 {} ({}ms): {}", res.getStatus(), ms, resBody);
			}
			if (rec != null) {
				RequestRecorder.finish(rec, res.getStatus(), ms, reqBody, resBody);
			}
			res.copyBodyToResponse();
			MDC.remove("req");
		}
	}

	private static String uri(HttpServletRequest r) {
		return r.getQueryString() == null ? r.getRequestURI() : r.getRequestURI() + "?" + r.getQueryString();
	}

	static String body(byte[] b) {
		if (b == null || b.length == 0) {
			return "(없음)";
		}
		String s = new String(b, 0, Math.min(b.length, MAX_BODY), StandardCharsets.UTF_8).replaceAll("[\\r\\n]+", " ");
		s = mask(s);
		return b.length >= MAX_BODY ? s + " …(앞 " + MAX_BODY + "바이트만)" : s;
	}

	static String mask(String json) {
		Matcher m = CARD.matcher(json);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + Mask.card(m.group(2)) + m.group(3)));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
