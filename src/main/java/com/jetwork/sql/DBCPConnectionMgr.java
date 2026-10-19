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
import org.apache.commons.pool.impl.GenericObjectPool;
public class DBCPConnectionMgr {
    String jdbcDriver="com.microsoft.sqlserver.jdbc.SQLServerDriver";
    // 접속 정보는 DbConfig 가 db.properties 또는 환경변수에서 읽어 온다.
    String jdbcURL = DbConfig.getUrl();
    String user = DbConfig.getUsername();
    String password = DbConfig.getPassword();
    
    private static DBCPConnectionMgr instance;
    synchronized public static DBCPConnectionMgr getInstance() {
		if (instance != null) {
			return instance;
		}
		System.out.println(instance);
	
			instance = new DBCPConnectionMgr();
			System.out.println(instance);
		return instance;
    }
   
    /**
     * 생성자
     *
     */
    public DBCPConnectionMgr() {
        super();
        try{
        	System.out.println("1111111111111");
            setupDriver(jdbcDriver, jdbcURL, user, password);
            System.out.println("1111111111111");
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    /**
     * Connection을 리턴함
     * @return
     */
    public Connection getConnection(){
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:apache:commons:dbcp:ora10g2");
        } catch(SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        return con;
    }
   
    /**
     * Connection 설정
     * @param jdbcDriver
     * @param jdbcURL
     * @param user
     * @param password
     * @throws Exception
     */
    public void setupDriver(String jdbcDriver,
                                   String jdbcURL,
                                   String user,
                                   String password) throws Exception {
        // JDBC 드라이버 로딩
        Class.forName(jdbcDriver);
       
        // Connection Pool 생성
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        connectionPool.setMaxActive(20000); //최대 20개 접속 지원
        connectionPool.setMaxIdle(500);    //5개 대기중 설정
       
        // 실제 DB와의 커넥션을 연결해주는 팩토리 생성
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            jdbcURL, // JDBC URL
            user, // 사용자
            password);
       
        // Connection Pool이 PoolableConnection 객체를 생성할 때 사용할
        // PoolableConnectionFactory 생성
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
            connectionFactory,
            connectionPool,
            null, // statement pool
            null, // 커넥션 테스트 쿼리: 커넥션이 유효한지 테스트할 때 사용되는 쿼리.
            false, // read only 여부
            true); // auto commit 여부
       
        // Pooling을 위한 JDBC 드라이버 생성 및 등록
        PoolingDriver driver = new PoolingDriver();
       
        // JDBC 드라이버에 커넥션 풀 등록
        driver.registerPool("ora10g2", connectionPool);
        
        System.out.println("커넥션풀 등록===="+driver);
    }
   
    public void freeConnection(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void freeConnection(Connection con, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Connection con, PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }   
    public void freeConnection(Connection con, Statement stmt) {
        try {
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Connection con) {
        try {
            if (con != null) con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public void freeConnection(PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void freeConnection(ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }   
   
}
