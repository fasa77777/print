package com.jetwork.test.fake;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 테스트용 가짜 DB 드라이버. URL 은 jdbc:fakedb: 로 시작한다.
 *
 * 실제 DB 없이 메모리 위의 표(jw_terminal, jw_payment, cybermoney, ...)에 대해
 * 이 서버가 쓰는 SQL 만 흉내 낸다. 네트워크에는 접속하지 않는다.
 * OfflineTest 가 db.properties 의 db.driver 로 이 클래스를 지정해서 쓴다.
 */
public class FakeDbDriver implements Driver {

	static {
		try {
			DriverManager.registerDriver(new FakeDbDriver());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/** 표마다 컬럼 순서 (SELECT * 결과의 컬럼 번호용) */
	private static final Map<String, List<String>> COLS = new LinkedHashMap<String, List<String>>();
	/** 표 내용 */
	private static final Map<String, List<Map<String, String>>> TABLES = new LinkedHashMap<String, List<Map<String, String>>>();
	/** 실행된 SQL (값이 채워진 형태) */
	public static final List<String> LOG = Collections.synchronizedList(new ArrayList<String>());
	/** 쿼리 한 번에 걸리는 시간 (실제 DB 왕복을 흉내). 0 이면 지연 없음 */
	public static volatile long queryDelayMillis = 0;
	/** true 면 DB 장애: 새 접속 실패, 열려 있던 커넥션도 쿼리 실패 */
	public static volatile boolean down = false;
	/** 지금 열려 있는 실제(물리) 커넥션 수와 지금까지의 최대, 만든 총수 */
	public static final java.util.concurrent.atomic.AtomicInteger openConnections = new java.util.concurrent.atomic.AtomicInteger();
	public static final java.util.concurrent.atomic.AtomicInteger maxOpenConnections = new java.util.concurrent.atomic.AtomicInteger();
	public static final java.util.concurrent.atomic.AtomicInteger createdConnections = new java.util.concurrent.atomic.AtomicInteger();
	/** 실행한 쿼리 수 (SELECT 1 확인 포함) */
	public static final java.util.concurrent.atomic.AtomicLong statements = new java.util.concurrent.atomic.AtomicLong();
	private static int seq = 0;
	/** false 면 LOG 에 SQL 을 남기지 않는다 (부하 테스트용) */
	public static volatile boolean logging = true;

	static {
		COLS.put("jw_terminal", Arrays.asList("terminal_id", "serial", "fran_id", "agent_version", "update", "terminalVersion", "last_dt"));
		COLS.put("jw_franchise", Arrays.asList("fran_id", "fran_name"));
		COLS.put("cybermoney", Arrays.asList("cardNumber", "cyberMoney", "fran_id", "reg_date"));
		COLS.put("jw_paper", Arrays.asList("terminalID", "paper_colorYN", "paper_price", "paper_size"));
		COLS.put("printer", Arrays.asList("serialnumber", "devid", "sitename"));
		COLS.put("jw_payment", Arrays.asList("id"));
		reset();
	}

	/** 표를 처음 상태로 되돌린다. 단말기 T1(시리얼 S1, 가맹점 F1 홍대점), 카드 C1 1000원·C2 100원, A4 가격, 프린터 P1 */
	public static synchronized void reset() {
		for (String t : COLS.keySet()) {
			TABLES.put(t, new ArrayList<Map<String, String>>());
		}
		seq = 0;
		LOG.clear();
		logging = true;
		add("jw_terminal", "terminal_id", "T1", "serial", "S1", "fran_id", "F1");
		add("jw_franchise", "fran_id", "F1", "fran_name", "홍대점");
		add("cybermoney", "cardNumber", "C1", "cyberMoney", "1000", "fran_id", "F1");
		add("cybermoney", "cardNumber", "C2", "cyberMoney", "100", "fran_id", "F1");
		add("jw_paper", "terminalID", "T1", "paper_colorYN", "Y", "paper_price", "200", "paper_size", "A4");
		add("jw_paper", "terminalID", "T1", "paper_colorYN", "N", "paper_price", "100", "paper_size", "A4");
		add("printer", "serialnumber", "P1", "devid", "D1", "sitename", "old");
	}

	/** 테스트 중 표에 행을 넣는다 (예: addRow("jw_paper", "terminalID", "T2", "paper_colorYN", "Y", ...)) */
	public static synchronized void addRow(String table, String... kv) {
		add(table, kv);
	}

	private static void add(String t, String... kv) {
		Map<String, String> r = new LinkedHashMap<String, String>();
		for (int i = 0; i < kv.length; i += 2) {
			r.put(kv[i], kv[i + 1]);
		}
		TABLES.get(t).add(r);
	}

	/** 표의 행 복사본 */
	public static synchronized List<Map<String, String>> rows(String table) {
		List<Map<String, String>> out = new ArrayList<Map<String, String>>();
		for (Map<String, String> r : TABLES.get(table)) {
			out.add(new LinkedHashMap<String, String>(r));
		}
		return out;
	}

	/** 표에서 column=value 인 첫 행. 없으면 null */
	public static synchronized Map<String, String> find(String table, String column, String value) {
		for (Map<String, String> r : TABLES.get(table)) {
			if (Objects.equals(r.get(column), value)) {
				return new LinkedHashMap<String, String>(r);
			}
		}
		return null;
	}

	// ---------------- JDBC ----------------

	public boolean acceptsURL(String u) {
		return u != null && u.startsWith("jdbc:fakedb:");
	}

	public Connection connect(String url, Properties p) throws SQLException {
		if (!acceptsURL(url)) {
			return null;
		}
		if (down) {
			throw new SQLException("가짜 DB 장애 (접속 실패)");
		}
		createdConnections.incrementAndGet();
		int n = openConnections.incrementAndGet();
		while (true) {
			int max = maxOpenConnections.get();
			if (n <= max || maxOpenConnections.compareAndSet(max, n)) break;
		}
		final boolean[] closed = { false };
		final boolean[] dead = { false };
		return (Connection) proxy(Connection.class, new InvocationHandler() {
			public Object invoke(Object o, Method m, Object[] a) throws SQLException {
				String nm = m.getName();
				// DB 와 실제로 주고받는 동작만 장애 때 실패한다 (toString·getAutoCommit 같은 것은 실제 드라이버도 실패하지 않음)
				if (down && (nm.equals("prepareStatement") || nm.equals("createStatement") || nm.equals("commit")
						|| nm.equals("rollback") || nm.equals("setAutoCommit"))) {
					dead[0] = true;
					throw new SQLException("가짜 DB 장애 (연결 끊김)");
				}
				if (nm.equals("createStatement")) return statement(null);
				if (nm.equals("prepareStatement")) return statement((String) a[0]);
				if (nm.equals("close")) {
					if (!closed[0]) { closed[0] = true; openConnections.decrementAndGet(); }
					return null;
				}
				if (nm.equals("isClosed")) return closed[0];
				if (nm.equals("isValid")) { if (down) dead[0] = true; return !closed[0] && !dead[0]; }
				if (nm.equals("getAutoCommit")) return true;
				if (nm.equals("getTransactionIsolation")) return Connection.TRANSACTION_READ_COMMITTED;
				return common(o, m, a);
			}
		});
	}

	public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) {
		return new DriverPropertyInfo[0];
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {
		return 0;
	}

	public boolean jdbcCompliant() {
		return false;
	}

	public java.util.logging.Logger getParentLogger() {
		return java.util.logging.Logger.getLogger("fakedb");
	}

	private static Object proxy(Class<?> c, InvocationHandler h) {
		return Proxy.newProxyInstance(FakeDbDriver.class.getClassLoader(), new Class<?>[] { c }, h);
	}

	/** hashCode·equals·toString 과, 나머지 메서드의 기본 반환값 */
	private static Object common(Object o, Method m, Object[] a) {
		String nm = m.getName();
		if (nm.equals("hashCode")) return System.identityHashCode(o);
		if (nm.equals("equals")) return o == a[0];
		if (nm.equals("toString")) return "FakeDb";
		Class<?> r = m.getReturnType();
		if (r == boolean.class) return false;
		if (r == int.class) return 0;
		if (r == long.class) return 0L;
		return null;
	}

	private static Object statement(final String sql0) {
		final Map<Integer, String> params = new HashMap<Integer, String>();
		return proxy(PreparedStatement.class, new InvocationHandler() {
			public Object invoke(Object o, Method m, Object[] a) throws Throwable {
				String nm = m.getName();
				if (nm.startsWith("set") && a != null && a.length == 2 && a[0] instanceof Integer) {
					Object v = a[1];
					params.put((Integer) a[0], v == null ? "NULL"
							: v instanceof Number ? v.toString() : "'" + v.toString().replace("'", "''") + "'");
					return null;
				}
				String sql = a != null && a.length > 0 && a[0] instanceof String ? (String) a[0] : sql0;
				if (nm.equals("executeQuery") || nm.equals("executeUpdate") || nm.equals("execute")) {
					if (down) throw new SQLException("가짜 DB 장애 (쿼리 실패)");
					statements.incrementAndGet();
					long d = queryDelayMillis;
					if (d > 0) Thread.sleep(d);
				}
				if (nm.equals("executeQuery")) return query(render(sql, params));
				if (nm.equals("executeUpdate")) return update(render(sql, params));
				if (nm.equals("execute")) {
					// 커넥션 풀의 확인 쿼리(SELECT 1)는 execute 로 온다
					String r = render(sql, params);
					if (r.trim().toUpperCase().startsWith("SELECT")) {
						query(r);
						return true;
					}
					update(r);
					return false;
				}
				return common(o, m, a);
			}
		});
	}

	/** ? 자리에 값을 채운 SQL */
	private static String render(String sql, Map<Integer, String> params) {
		StringBuilder sb = new StringBuilder();
		int k = 1;
		boolean quoted = false;
		for (char c : sql.toCharArray()) {
			if (c == '\'') quoted = !quoted;
			if (c == '?' && !quoted) sb.append(params.get(k++));
			else sb.append(c);
		}
		return sb.toString().trim();
	}

	private static final Pattern COND = Pattern.compile("\\[?(\\w+)\\]?\\s*=\\s*(N?'(?:[^']|'')*'|-?\\d+)");
	private static final Pattern INS = Pattern.compile("(?is)INSERT INTO \\[dbo\\]\\.\\[(\\w+)\\]\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*)\\)\\s*;?\\s*$");
	private static final Pattern UPD = Pattern.compile("(?is)UPDATE\\s+\\[dbo\\]\\.\\[(\\w+)\\]\\s+SET\\s+(.*?)\\s+WHERE\\s+(.*)$");
	private static final Pattern DEL = Pattern.compile("(?is)DELETE\\s+FROM\\s+\\[dbo\\]\\.\\[(\\w+)\\]\\s+WHERE\\s+(.*)$");
	private static final Pattern TABLE = Pattern.compile("(?i)\\[?(jw_payment|jw_terminal|cybermoney|jw_franchise|jw_paper|printer)\\]?");
	private static final Pattern CAST_GE = Pattern.compile("CAST\\(\\[cyberMoney\\] AS INT\\) >= (\\d+)");
	private static final Pattern CAST_MINUS = Pattern.compile("CAST\\(\\[cyberMoney\\] AS INT\\) - (\\d+)");

	private static Map<String, String> conds(String where) {
		Map<String, String> m = new LinkedHashMap<String, String>();
		Matcher mt = COND.matcher(where);
		while (mt.find()) {
			m.put(mt.group(1), value(mt.group(2)));
		}
		m.remove("120"); // CONVERT(CHAR(10), reg_date, 120) = ... : "오늘" 조건은 무시 (모든 행이 오늘)
		return m;
	}

	private static String value(String v) {
		v = v.trim();
		if (v.equalsIgnoreCase("NULL")) return null;
		if (v.equalsIgnoreCase("getdate()")) return "<now>";
		if (v.startsWith("N'")) v = v.substring(1);
		if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) return v.substring(1, v.length() - 1).replace("''", "'");
		return v;
	}

	private static String where(String sql) {
		int i = sql.toLowerCase().lastIndexOf(" where ");
		return i < 0 ? "" : sql.substring(i + 7);
	}

	private static boolean matches(Map<String, String> row, Map<String, String> c) {
		for (Map.Entry<String, String> e : c.entrySet()) {
			if (!Objects.equals(row.get(e.getKey()), e.getValue())) return false;
		}
		return true;
	}

	private static List<String> splitTop(String s) {
		List<String> r = new ArrayList<String>();
		int depth = 0;
		boolean quoted = false;
		StringBuilder b = new StringBuilder();
		for (char ch : s.toCharArray()) {
			if (ch == '\'') quoted = !quoted;
			if (!quoted && ch == '(') depth++;
			if (!quoted && ch == ')') depth--;
			if (!quoted && depth == 0 && ch == ',') {
				r.add(b.toString());
				b.setLength(0);
			} else {
				b.append(ch);
			}
		}
		r.add(b.toString());
		return r;
	}

	private static synchronized ResultSet query(String sql) {
		if (logging) LOG.add(sql);
		List<Map<String, String>> out = new ArrayList<Map<String, String>>();
		if (sql.equals("SELECT 1")) {
			out.add(Collections.singletonMap("1", "1"));
			return resultSet(Arrays.asList("1"), out);
		}
		if (sql.contains("fran_name")) {
			Map<String, String> c = conds(where(sql));
			for (Map<String, String> t : TABLES.get("jw_terminal")) {
				if (Objects.equals(t.get("terminal_id"), c.get("terminal_id"))) {
					for (Map<String, String> f : TABLES.get("jw_franchise")) {
						if (Objects.equals(f.get("fran_id"), t.get("fran_id"))) {
							out.add(Collections.singletonMap("fran_name", f.get("fran_name")));
						}
					}
				}
			}
			return resultSet(Arrays.asList("fran_name"), out);
		}
		Matcher tm = TABLE.matcher(sql);
		if (!tm.find()) throw new IllegalStateException("가짜 DB 가 모르는 SELECT: " + sql);
		String t = tm.group(1).toLowerCase();
		Map<String, String> c = conds(where(sql));
		for (Map<String, String> r : TABLES.get(t)) {
			if (matches(r, c)) out.add(new LinkedHashMap<String, String>(r));
		}
		if (sql.startsWith("SELECT 1")) {
			List<Map<String, String>> ones = new ArrayList<Map<String, String>>();
			for (int i = 0; i < out.size(); i++) ones.add(Collections.singletonMap("1", "1"));
			return resultSet(Arrays.asList("1"), ones);
		}
		return resultSet(COLS.get(t), out);
	}

	private static synchronized int update(String sql) {
		if (logging) LOG.add(sql);
		Matcher mt = INS.matcher(sql);
		if (mt.find()) {
			String t = mt.group(1).toLowerCase();
			List<String> cs = splitTop(mt.group(2));
			List<String> vs = splitTop(mt.group(3));
			Map<String, String> row = new LinkedHashMap<String, String>();
			if (t.equals("jw_payment")) row.put("id", String.valueOf(++seq));
			for (int i = 0; i < cs.size(); i++) {
				row.put(cs.get(i).trim().replace("[", "").replace("]", ""), value(vs.get(i)));
			}
			TABLES.get(t).add(row);
			return 1;
		}
		mt = UPD.matcher(sql);
		if (mt.find()) {
			String t = mt.group(1).toLowerCase();
			String w = mt.group(3);
			Map<String, String> c = conds(w);
			Matcher ge = CAST_GE.matcher(w);
			Integer min = ge.find() ? Integer.valueOf(ge.group(1)) : null;
			int n = 0;
			for (Map<String, String> r : TABLES.get(t)) {
				if (!matches(r, c)) continue;
				if (min != null && Integer.parseInt(r.get("cyberMoney")) < min) continue;
				for (String as : splitTop(mt.group(2))) {
					int eq = as.indexOf('=');
					String k = as.substring(0, eq).trim().replace("[", "").replace("]", "");
					String v = as.substring(eq + 1).trim();
					Matcher minus = CAST_MINUS.matcher(v);
					r.put(k, minus.find() ? String.valueOf(Integer.parseInt(r.get("cyberMoney")) - Integer.parseInt(minus.group(1))) : value(v));
				}
				n++;
			}
			return n;
		}
		mt = DEL.matcher(sql);
		if (mt.find()) {
			String t = mt.group(1).toLowerCase();
			Map<String, String> c = conds(mt.group(2));
			int n = 0;
			for (Iterator<Map<String, String>> it = TABLES.get(t).iterator(); it.hasNext();) {
				if (matches(it.next(), c)) {
					it.remove();
					n++;
				}
			}
			return n;
		}
		throw new IllegalStateException("가짜 DB 가 모르는 SQL: " + sql);
	}

	private static ResultSet resultSet(final List<String> cols, final List<Map<String, String>> rows) {
		final int[] i = { -1 };
		return (ResultSet) proxy(ResultSet.class, new InvocationHandler() {
			public Object invoke(Object o, Method m, Object[] a) {
				String nm = m.getName();
				if (nm.equals("next")) return ++i[0] < rows.size();
				if (nm.equals("getString")) {
					return a[0] instanceof Integer ? rows.get(i[0]).get(cols.get((Integer) a[0] - 1)) : rows.get(i[0]).get((String) a[0]);
				}
				if (nm.equals("getMetaData")) {
					return proxy(ResultSetMetaData.class, new InvocationHandler() {
						public Object invoke(Object o2, Method m2, Object[] a2) {
							if (m2.getName().equals("getColumnCount")) return cols.size();
							if (m2.getName().equals("getColumnName")) return cols.get((Integer) a2[0] - 1);
							return common(o2, m2, a2);
						}
					});
				}
				return common(o, m, a);
			}
		});
	}
}
