package com.jetwork.sql;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * ���ϸ� : DBConnectionManager.java
 * �ۼ��� : 2013. 07. 23
 * �ۼ��� : son
 * ���ϼ��� : DB Connection Pool
 */
@Component
public class DBConnectionManager {
	static private DBConnectionManager instance; // The single instance
	static private int clients;
	
	String resource ;
	//String ip;
	
	 @Value("${ip}")
	 private String ip;

	@SuppressWarnings("unchecked")
	private Vector drivers = new Vector();
	// private PrintWriter log;
	@SuppressWarnings("unchecked")
	private Hashtable pools = new Hashtable();
	// Log
	//private Logger log = Logger.getLogger("DEBUG");
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	/**
	 * Returns the single instance, creating one if it's the first time this
	 * method is called.
	 * 
	 * @return DBConnectionManager The single instance.
	 */
	public static DBConnectionManager getInstance() {
		if (instance != null) {
			return instance;
		}
		synchronized (DBConnectionManager.class) {
			if (instance != null) {
				return instance;
			}
			instance = new DBConnectionManager();
		}
		return instance;
	}

	/**
	 * A private constructor since this is a Singleton
	 */
	private DBConnectionManager() {

		init();
	}

	/**
	 * Loads properties and initializes the instance with its values.
	 */
	private void init() {

//		InputStream is = getClass().getResourceAsStream("db.properties");
//		// InputStream is = new FileInputStream("db.properties");
//		Properties dbProps = new Properties();
//		try {
//			dbProps.load(is);
//		} catch (Exception e) {
//			System.err.println("Can't read the properties file. "
//					+ "Make sure db.properties is in the CLASSPATH");
//			return;
//		}
//		// String logFile = dbProps.getProperty("logfile",
//		// "/home/httpd/jsp/logs/DBConnectionManager.log");

		loadDrivers();
		createPools();
	}

	@SuppressWarnings("unchecked")
	private void loadDrivers() {
	//	String db = props.getProperty("db", "mssql");
		String driverClasses = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		StringTokenizer st = new StringTokenizer(driverClasses);
		// log.info("load");
		while (st.hasMoreElements()) {
			String driverClassName = st.nextToken().trim();
			try {
				Driver driver = (Driver) Class.forName(driverClassName)
						.newInstance();
				DriverManager.registerDriver(driver);
				drivers.addElement(driver);
				logger.info("Registered JDBC driver " + driverClassName);
			} catch (Exception e) {
				e.printStackTrace();
				logger.info("Can't register JDBC driver: " + driverClassName
						+ ", Exception: " + e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void createPools() {
		 resource = "db.properties";
		 
		 Properties properties = new Properties();
		 logger.info("dir == "+System.getProperty("user.dir"));
		 
		 String dir = System.getProperty("user.dir");

		 try {
			 logger.info(dir+"\\"+resource);
			// InputStream reader = getClass().getResourceAsStream(dir+"\\"+resource);
			// logger.info(""+reader);
			 
			 
			 InputStream is = getClass().getResourceAsStream(resource);
				// InputStream is = new FileInputStream("db.properties");
				Properties dbProps = new Properties();
			 
			 
			 
				dbProps.load(is);
				logger.info(dbProps.toString());
				logger.info("	"+dbProps.isEmpty());
				logger.info("	"+dbProps.size());
	         ip = dbProps.getProperty("ip");
	         System.out.println("ip ==="+ip);
		 }catch (Exception e) {
			// TODO: handle exception
			 logger.info(""+e);
		}
			String poolName = "mssql";
			String url = "jdbc:sqlserver://"+ip+":25433;DatabaseName=GWS";
		//	String url = "jdbc:sqlserver://"+ip+":25433;DatabaseName=SONTECH";
			String user = "sa";
			String password ="!fasa7721@";
			String maxconn ="2000";
			int max;
			try {
				max = Integer.valueOf(maxconn).intValue();
			} catch (NumberFormatException e) {
				e.printStackTrace();
				logger.info("Invalid maxconn value " + maxconn + " for "
						+ poolName);
				max = 0;
			}
			DBConnectionPool pool = new DBConnectionPool(poolName, url,
					user, password, max);
			pools.put(poolName, pool);
			logger.info("Initialized pool " + poolName);
		

	}

	/**
	 * Returns an open connection. If no one is available, and the max number of
	 * connections has not been reached, a new connection is created.
	 * 
	 * @param name
	 *            The pool name as defined in the properties file
	 * @return Connection The connection or null
	 */
	public Connection getConnection(String name) {
		clients++;
		DBConnectionPool pool = (DBConnectionPool) pools.get(name);
		if (pool != null) {
			// long time = 100000;
			return pool.getConnection();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public synchronized void release() {
		// Wait until called by the last client
		if (--clients != 0) {
			return;
		}

		Enumeration allPools = pools.elements();
		while (allPools.hasMoreElements()) {
			DBConnectionPool pool = (DBConnectionPool) allPools.nextElement();
			pool.release();
		}
		Enumeration allDrivers = drivers.elements();
		while (allDrivers.hasMoreElements()) {
			Driver driver = (Driver) allDrivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
				// log.info("Deregistered JDBC driver " +
				// driver.getClass().getName());
			} catch (SQLException e) {
				e.printStackTrace();
				// log.info( "Can't deregister JDBC driver: " +
				// driver.getClass().getName());
			}
		}
	}

	/**
	 * Returns a connection to the named pool.
	 * 
	 * @param name
	 *            The pool name as defined in the properties file
	 * @param con
	 *            The Connection
	 */
	public void freeConnection(String name, Connection con) {
		// /****DB POOL���***********///////////
		/*
		 * DBConnectionPool pool = (DBConnectionPool) pools.get(name); try { //
		 * new Connection if(con == null) con = pool.newConnection(); if (pool
		 * != null) { pool.freeConnection(con); } } catch (Exception e) {
		 * pool.decreaseMaxConn(); }
		 */
		// /****DB POOL��� ����***********///////////
		try {
			if ( con != null ) {
				con.close();
				//logger.info("Connection Free");
			}
			// log.info("Closed connection for pool " + name);
		} catch (SQLException e) {
			e.printStackTrace();
			// log.info( "Can't close connection for pool " + name);
		}

	}

	/**
	 * Writes a message to the log file.
	 */
	private void log(String msg) {
		// log.println(new Date() + ": " + msg);
	}

	/**
	 * Writes a message with an Exception to the log file.
	 */
	@SuppressWarnings("unused")
	private void log(Throwable e, String msg) {
		// log.println(new Date() + ": " + msg);
		// e.printStackTrace(log);
	}

	/**
	 * This inner class represents a connection pool. It creates new connections
	 * on demand, up to a max number if specified. It also makes sure a
	 * connection is still open before it is returned to a client.
	 */
	class DBConnectionPool {
		private int checkedOut;
		@SuppressWarnings("unchecked")
		private Vector freeConnections = new Vector();
		private int maxConn;
		private String name;
		private String password;
		private String URL;
		private String user;

		/**
		 * Creates new connection pool.
		 * 
		 * @param name
		 *            The pool name
		 * @param URL
		 *            The JDBC URL for the database
		 * @param user
		 *            The database user, or null
		 * @param password
		 *            The database user password, or null
		 * @param maxConn
		 *            The maximal number of connections, or 0 for no limit
		 */
		public DBConnectionPool(String name, String URL, String user,
				String password, int maxConn) {
			this.name = name;
			this.URL = URL;
			this.user = user;
			this.password = password;
			this.maxConn = maxConn;
		}

		/**
		 * Checks in a connection to the pool. Notify other Threads that may be
		 * waiting for a connection.
		 * 
		 * @param con
		 *            The connection to check in
		 */
		@SuppressWarnings("unchecked")
		public synchronized void freeConnection(Connection con) {
			// Put the connection at the end of the Vector
			freeConnections.addElement(con);
			checkedOut--;
			notifyAll();
		}

		/**
		 * Checks out a connection from the pool. If no free connection is
		 * available, a new connection is created unless the max number of
		 * connections has been reached. If a free connection has been closed by
		 * the database, it's removed from the pool and this method is called
		 * again recursively.
		 */
		public synchronized Connection getConnection() {
			Connection con = null;
			con = newConnection();
			return con;
		}

		/**
		 * Checks out a connection from the pool. If no free connection is
		 * available, a new connection is created unless the max number of
		 * connections has been reached. If a free connection has been closed by
		 * the database, it's removed from the pool and this method is called
		 * again recursively.
		 * <P>
		 * If no connection is available and the max number has been reached,
		 * this method waits the specified time for one to be checked in.
		 * 
		 * @param timeout
		 *            The timeout value in milliseconds
		 */

		/**
		 * Closes all available connections.
		 */
		@SuppressWarnings("unchecked")
		public synchronized void release() {
			Enumeration allConnections = freeConnections.elements();
			while (allConnections.hasMoreElements()) {
				Connection con = (Connection) allConnections.nextElement();
				try {
					con.close();
					logger.info("Closed connection for pool " + name);
				} catch (SQLException e) {
					e.printStackTrace();
					// log.info("Can't close connection for pool " + name);
				}
			}
			freeConnections.removeAllElements();
		}

		/**
		 * Creates a new connection, using a userid and password if specified.
		 */
		private Connection newConnection() {
			Connection con = null;

			try {
				if (user == null) {
					con = DriverManager.getConnection(URL);
				} else {
					// log.info("qqqquser = "+user);
					con = DriverManager.getConnection(URL, user, password);
				}
				// log.info("Created a new connection in pool " + name);
			} catch (SQLException e) {
				e.printStackTrace();
				logger.info("Can't create a new connection for " + URL);
				return null;
			}
			return con;
		}

		@SuppressWarnings("unused")
		private void decreaseMaxConn() {
			maxConn--;
		}
	}
}

