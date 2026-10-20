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

public class PaymentCK {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = true;

    public boolean setTerminal(String paymentOK) {
        block8: {
            DBCPConnectionMgr dbcp = ConnectionPool.getInst();
            this.conn = dbcp.getConnection();
            try {
                try {
                    this.conn.setAutoCommit(false);
                    String stringSQL = "select * from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) and payment_ok = '" + paymentOK + "'";
                    System.out.println(stringSQL);
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    while (this.rs.next()) {
                        this.userFlag = false;
                    }
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.conn.commit();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                    dbcp.freeConnection(this.conn, this.stmt, this.rs);
                    break block8;
                }
            }
            catch (Throwable throwable) {
                dbcp.freeConnection(this.conn, this.stmt, this.rs);
                throw throwable;
            }
            dbcp.freeConnection(this.conn, this.stmt, this.rs);
        }
        System.out.println("userFlag ==");
        return this.userFlag;
    }

    public void setTerminal1(String paymentOK) {
        block7: {
            DBCPConnectionMgr dbcp = ConnectionPool.getInst();
            this.conn = dbcp.getConnection();
            try {
                try {
                    this.conn.setAutoCommit(false);
                    String stringSQL = "select * from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) and payment_ok = '" + paymentOK + "'";
                    System.out.println(stringSQL);
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    this.conn.commit();
                    int i = 1;
                    String id = "";
                    while (this.rs.next()) {
                        id = this.rs.getString("id");
                        System.out.println(id);
                        ++i;
                    }
                    if (i > 1) {
                        System.out.println("중복삭제  ====" + id);
                        stringSQL = "delete   FROM [dbo].[jw_payment] where id = " + id;
                        System.out.println(stringSQL);
                        this.stmt = this.conn.createStatement(1004, 1007);
                        this.stmt.executeUpdate(stringSQL);
                        this.conn.commit();
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                    dbcp.freeConnection(this.conn, this.stmt, this.rs);
                    break block7;
                }
            }
            catch (Throwable throwable) {
                dbcp.freeConnection(this.conn, this.stmt, this.rs);
                throw throwable;
            }
            dbcp.freeConnection(this.conn, this.stmt, this.rs);
        }
        System.out.println("userFlag ==");
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
