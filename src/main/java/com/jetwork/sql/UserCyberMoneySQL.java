/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserCyberMoneySQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = true;

    public String getUser(String terminalID, String userID) {
        String money = "";
        DBCPConnectionMgr dbcp = ConnectionPool.getInst();
        this.conn = dbcp.getConnection();
        try {
            try {
                this.conn.setAutoCommit(false);
                String stringSQL = " SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '" + userID + "'";
                this.stmt = this.conn.createStatement(1004, 1007);
                this.rs = this.stmt.executeQuery(stringSQL);
                while (this.rs.next()) {
                    money = this.rs.getString("cyberMoney");
                    this.userFlag = false;
                }
                if (this.userFlag) {
                    stringSQL = " SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = '" + terminalID + "'";
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    String franID = "";
                    while (this.rs.next()) {
                        System.out.println(this.rs.getString(3));
                        franID = this.rs.getString(3);
                    }
                    System.out.println(franID);
                    String insertSQL = "INSERT INTO [dbo].[cybermoney] ([cardNumber],[cyberMoney],[fran_id],[reg_date]) VALUES  ('" + userID + "',0,'" + franID + "',getdate());";
                    this.stmt.executeUpdate(insertSQL);
                    money = "0";
                }
                this.conn.commit();
            }
            catch (SQLException e) {
                e.printStackTrace();
                dbcp.freeConnection(this.conn, this.stmt, this.rs);
            }
        }
        finally {
            dbcp.freeConnection(this.conn, this.stmt, this.rs);
        }
        return money;
    }

    public void freeConn() {
        try {
            if (this.stmt != null) {
                this.stmt.close();
            }
            if (this.rs != null) {
                this.rs.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
