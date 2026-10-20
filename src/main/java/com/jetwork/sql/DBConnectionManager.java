/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import java.io.InputStream;
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

@Component
public class DBConnectionManager {
    private static DBConnectionManager instance;
    private static int clients;
    String resource;
    @Value(value="${ip}")
    private String ip;
    private Vector drivers = new Vector();
    private Hashtable pools = new Hashtable();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static DBConnectionManager getInstance() {
        if (instance != null) {
            return instance;
        }
        Class<DBConnectionManager> clazz = DBConnectionManager.class;
        synchronized (DBConnectionManager.class) {
            if (instance != null) {
                // ** MonitorExit[var0] (shouldn't be in output)
                return instance;
            }
            instance = new DBConnectionManager();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    private DBConnectionManager() {
        this.init();
    }

    private void init() {
        this.loadDrivers();
        this.createPools();
    }

    private void loadDrivers() {
        String driverClasses = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        StringTokenizer st = new StringTokenizer(driverClasses);
        while (st.hasMoreElements()) {
            String driverClassName = st.nextToken().trim();
            try {
                Driver driver = (Driver)Class.forName(driverClassName).newInstance();
                DriverManager.registerDriver(driver);
                this.drivers.addElement(driver);
                this.logger.info("Registered JDBC driver " + driverClassName);
            }
            catch (Exception e) {
                e.printStackTrace();
                this.logger.info("Can't register JDBC driver: " + driverClassName + ", Exception: " + e);
            }
        }
    }

    private void createPools() {
        int max;
        this.resource = "db.properties";
        Properties properties = new Properties();
        this.logger.info("dir == " + System.getProperty("user.dir"));
        String dir = System.getProperty("user.dir");
        try {
            this.logger.info(String.valueOf(dir) + "\\" + this.resource);
            InputStream is = this.getClass().getResourceAsStream(this.resource);
            Properties dbProps = new Properties();
            dbProps.load(is);
            this.logger.info(dbProps.toString());
            this.logger.info("\t" + dbProps.isEmpty());
            this.logger.info("\t" + dbProps.size());
            this.ip = dbProps.getProperty("ip");
            System.out.println("ip ===" + this.ip);
        }
        catch (Exception e) {
            this.logger.info("" + e);
        }
        String poolName = "mssql";
        String url = "jdbc:sqlserver://" + this.ip + ":25433;DatabaseName=SONTECH";
        // DB 계정은 소스에 두지 않고 db.properties (또는 환경변수) 에서 읽는다. (DbConfig)
        String user = DbConfig.getUsername();
        String password = DbConfig.getPassword();
        String maxconn = "2000";
        try {
            max = Integer.valueOf(maxconn);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            this.logger.info("Invalid maxconn value " + maxconn + " for " + poolName);
            max = 0;
        }
        DBConnectionPool pool = new DBConnectionPool(poolName, url, user, password, max);
        this.pools.put(poolName, pool);
        this.logger.info("Initialized pool " + poolName);
    }

    public Connection getConnection(String name) {
        ++clients;
        DBConnectionPool pool = (DBConnectionPool)this.pools.get(name);
        if (pool != null) {
            return pool.getConnection();
        }
        return null;
    }

    public synchronized void release() {
        if (--clients != 0) {
            return;
        }
        Enumeration allPools = this.pools.elements();
        while (allPools.hasMoreElements()) {
            DBConnectionPool pool = (DBConnectionPool)allPools.nextElement();
            pool.release();
        }
        Enumeration allDrivers = this.drivers.elements();
        while (allDrivers.hasMoreElements()) {
            Driver driver = (Driver)allDrivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void freeConnection(String name, Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void log(String msg) {
    }

    private void log(Throwable e, String msg) {
    }

    class DBConnectionPool {
        private int checkedOut;
        private Vector freeConnections = new Vector();
        private int maxConn;
        private String name;
        private String password;
        private String URL;
        private String user;

        public DBConnectionPool(String name, String URL, String user, String password, int maxConn) {
            this.name = name;
            this.URL = URL;
            this.user = user;
            this.password = password;
            this.maxConn = maxConn;
        }

        public synchronized void freeConnection(Connection con) {
            this.freeConnections.addElement(con);
            --this.checkedOut;
            this.notifyAll();
        }

        public synchronized Connection getConnection() {
            Connection con = null;
            con = this.newConnection();
            return con;
        }

        public synchronized void release() {
            Enumeration allConnections = this.freeConnections.elements();
            while (allConnections.hasMoreElements()) {
                Connection con = (Connection)allConnections.nextElement();
                try {
                    con.close();
                    DBConnectionManager.this.logger.info("Closed connection for pool " + this.name);
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            this.freeConnections.removeAllElements();
        }

        private Connection newConnection() {
            Connection con = null;
            try {
                con = this.user == null ? DriverManager.getConnection(this.URL) : DriverManager.getConnection(this.URL, this.user, this.password);
            }
            catch (SQLException e) {
                e.printStackTrace();
                DBConnectionManager.this.logger.info("Can't create a new connection for " + this.URL);
                return null;
            }
            return con;
        }

        private void decreaseMaxConn() {
            --this.maxConn;
        }
    }
}
