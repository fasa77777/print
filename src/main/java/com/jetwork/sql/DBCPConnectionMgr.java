/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

public class DBCPConnectionMgr {
    String jdbcDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    // DB 접속 정보는 소스에 두지 않고 db.properties (또는 환경변수 DB_URL, DB_USERNAME, DB_PASSWORD) 에서 읽는다. (DbConfig)
    String jdbcURL = DbConfig.getUrl();
    String user = DbConfig.getUsername();
    String password = DbConfig.getPassword();
    private static DBCPConnectionMgr instance;

    public static synchronized DBCPConnectionMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        System.out.println(instance);
        instance = new DBCPConnectionMgr();
        System.out.println(instance);
        return instance;
    }

    public DBCPConnectionMgr() {
        try {
            System.out.println("1111111111111");
            this.setupDriver(this.jdbcDriver, this.jdbcURL, this.user, this.password);
            System.out.println("1111111111111");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Connection getConnection() {
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:apache:commons:dbcp:ora10g2");
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
        return con;
    }

    public void setupDriver(String jdbcDriver, String jdbcURL, String user, String password) throws Exception {
        Class.forName(jdbcDriver);
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        connectionPool.setMaxActive(200);
        connectionPool.setMaxIdle(50);
        DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcURL, user, password);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory((ConnectionFactory)connectionFactory, (ObjectPool)connectionPool, null, null, false, true);
        PoolingDriver driver = new PoolingDriver();
        driver.registerPool("ora10g2", (ObjectPool)connectionPool);
        System.out.println("커넥션풀 등록====" + driver);
    }

    public void freeConnection(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            this.freeConnection(con);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(Connection con, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            this.freeConnection(con);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(Connection con, PreparedStatement pstmt) {
        try {
            if (pstmt != null) {
                pstmt.close();
            }
            this.freeConnection(con);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(Connection con, Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
            this.freeConnection(con);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(PreparedStatement pstmt) {
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void freeConnection(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
