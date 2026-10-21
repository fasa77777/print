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
 * 끄면(기본) 아무것도 하지 않고 다음 단계로 넘긴다.
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
		if (!log.isDebugEnabled()) {
			chain.doFilter(request, response);
			return;
		}
		String id = "#" + SEQ.incrementAndGet();
		MDC.put("req", id);
		ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
		long t0 = System.nanoTime();
		log.debug("요청 시작 {} {} from {}", request.getMethod(), uri(request), request.getRemoteAddr());
		try {
			chain.doFilter(req, res);
		} catch (IOException | ServletException | RuntimeException e) {
			log.debug("요청 처리 중 예외: {}", e.toString());
			throw e;
		} finally {
			log.debug("요청 본문: {}", body(req.getContentAsByteArray()));
			log.debug("응답 {} ({}ms): {}", res.getStatus(), (System.nanoTime() - t0) / 1000000, body(res.getContentAsByteArray()));
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
		return b.length > MAX_BODY ? s + " …(" + b.length + "바이트 중 앞부분)" : s;
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
