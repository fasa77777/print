package com.jetwork.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.kr.up.util.Mask;

/**
 * SQL 전체 로그 (로거 이름 jetwork.trace.sql, DEBUG 일 때만).
 *
 * 켜면 DB 에 보내는 모든 SQL 을 한 줄씩 남긴다: SQL, 넣은 값, 결과(조회 행 수·바뀐 행 수), 걸린 시간, commit/rollback.
 * 카드번호(cardNumber, userCard 컬럼 값)는 가린다.
 * 끄면(기본) 커넥션을 감싸지 않으므로 성능에 영향이 없다.
 *
 * 켜는 방법: application.properties 의 logging.level.jetwork-all=debug (또는 logging.level.jetwork.trace.sql=debug)
 */
public final class SqlTrace {

	static final Logger log = LoggerFactory.getLogger("jetwork.trace.sql");

	/** 값을 가릴 컬럼 */
	private static final Set<String> MASKED = new HashSet<String>(Arrays.asList("cardnumber", "usercard"));
	private static final Pattern COLUMN_BEFORE = Pattern.compile("\\[?(\\w+)\\]?\\s*(=|>=|<=|<>|>|<)\\s*$");
	private static final Pattern INSERT = Pattern.compile("(?is)INSERT\\s+INTO\\s+\\S+\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*)\\)");

	private SqlTrace() {
	}

	public static boolean enabled() {
		return log.isDebugEnabled();
	}

	/** 커넥션을 감싼다 (enabled() 일 때만 부른다) */
	static Connection wrap(final Connection target) {
		return (Connection) Proxy.newProxyInstance(SqlTrace.class.getClassLoader(), new Class<?>[] { Connection.class },
				new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
						String name = m.getName();
						long t0 = System.nanoTime();
						Object r = call(target, m, args, null);
						if (name.equals("prepareStatement") && r instanceof PreparedStatement) {
							return statement((PreparedStatement) r, (String) args[0]);
						}
						if (name.equals("commit") || name.equals("rollback")) {
							log.debug("{} ({})", name, ms(t0));
						}
						return r;
					}
				});
	}

	private static PreparedStatement statement(final PreparedStatement target, final String sql) {
		final TreeMap<Integer, Object> params = new TreeMap<Integer, Object>();
		return (PreparedStatement) Proxy.newProxyInstance(SqlTrace.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
				new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
						String name = m.getName();
						if (name.startsWith("set") && args != null && args.length == 2 && args[0] instanceof Integer) {
							params.put((Integer) args[0], args[1]);
							return call(target, m, args, null);
						}
						if (name.equals("executeQuery") && (args == null || args.length == 0)) {
							long t0 = System.nanoTime();
							ResultSet rs = (ResultSet) call(target, m, args, sql + params(sql, params));
							return resultSet(rs, sql + params(sql, params), ms(t0));
						}
						if (name.equals("executeUpdate") && (args == null || args.length == 0)) {
							long t0 = System.nanoTime();
							Object n = call(target, m, args, sql + params(sql, params));
							log.debug("{}{} → {}행 바뀜 ({})", oneLine(sql), params(sql, params), n, ms(t0));
							return n;
						}
						return call(target, m, args, null);
					}
				});
	}

	private static ResultSet resultSet(final ResultSet target, final String what, final String elapsed) {
		final int[] rows = { 0 };
		final boolean[] logged = { false };
		return (ResultSet) Proxy.newProxyInstance(SqlTrace.class.getClassLoader(), new Class<?>[] { ResultSet.class },
				new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
						Object r = call(target, m, args, null);
						if (m.getName().equals("next") && Boolean.TRUE.equals(r)) {
							rows[0]++;
						}
						if (m.getName().equals("close") && !logged[0]) {
							logged[0] = true;
							log.debug("{} → {}행 ({})", oneLine(what), rows[0], elapsed);
						}
						return r;
					}
				});
	}

	/** 실제 메서드를 부르고, 실패하면 SQL 과 함께 로그를 남긴 뒤 원래 예외를 던진다 */
	private static Object call(Object target, Method m, Object[] args, String what) throws Throwable {
		try {
			return m.invoke(target, args);
		} catch (InvocationTargetException e) {
			if (what != null) {
				log.debug("{} → 실패: {}", oneLine(what), e.getCause().toString());
			}
			throw e.getCause();
		}
	}

	/** " | 값: [1]=T1, [2]=8E****" */
	static String params(String sql, TreeMap<Integer, Object> params) {
		if (params.isEmpty()) {
			return "";
		}
		List<String> cols = paramColumns(sql);
		StringBuilder sb = new StringBuilder(" | 값: ");
		boolean first = true;
		for (java.util.Map.Entry<Integer, Object> e : params.entrySet()) {
			int i = e.getKey();
			Object v = e.getValue();
			String col = i - 1 < cols.size() ? cols.get(i - 1) : "";
			String text = v == null ? "NULL" : MASKED.contains(col.toLowerCase()) ? Mask.card(v.toString()) : "'" + v + "'";
			if (!first) sb.append(", ");
			sb.append('[').append(i).append("]=").append(text);
			first = false;
		}
		return sb.toString();
	}

	/** ? 마다 어느 컬럼의 값인지 (모르면 빈 문자열) */
	static List<String> paramColumns(String sql) {
		List<String> cols = new ArrayList<String>();
		Matcher ins = INSERT.matcher(sql);
		if (ins.find()) {
			String[] names = ins.group(1).split(",");
			String[] values = ins.group(2).replaceAll("N?'[^']*'", "''").split(","); // 문자열 안의 쉼표는 무시
			for (int i = 0; i < values.length && i < names.length; i++) {
				if (values[i].trim().equals("?")) {
					cols.add(names[i].trim().replace("[", "").replace("]", ""));
				}
			}
			return cols;
		}
		for (int i = 0; i < sql.length(); i++) {
			if (sql.charAt(i) == '?') {
				Matcher mt = COLUMN_BEFORE.matcher(sql.substring(Math.max(0, i - 60), i));
				cols.add(mt.find() ? mt.group(1) : "");
			}
		}
		return cols;
	}

	private static String oneLine(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}

	private static String ms(long t0) {
		return String.format("%.1fms", (System.nanoTime() - t0) / 1e6);
	}
}
