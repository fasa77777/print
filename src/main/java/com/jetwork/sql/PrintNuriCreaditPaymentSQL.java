/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PrintNuriPay;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintNuriCreaditPaymentSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String setPayment(PrintNuriPay payment) {
        String money = "";
        DBCPConnectionMgr dbcp = ConnectionPool.getInst();
        this.conn = dbcp.getConnection();
        try {
            try {
                this.conn.setAutoCommit(false);
                String payType = "";
                payType = "Y".equals(payment.getPayType()) ? "체크카드" : "신용카드";
                if ("TM".equals(payment.getPayTypeTM())) {
                    payType = "티머니";
                } else if ("EB".equals(payment.getPayTypeTM())) {
                    payType = "캐시비";
                }
                System.out.println("111111111111");
                String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getPrice() + "',N'" + payType + "','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + payment.getPayOK() + "','print',getdate(), N'" + CardCompanyName.getName(payment.getCraditName().trim()) + "','" + payment.getCount() + "' );";
                this.stmt = this.conn.createStatement(1004, 1007);
                System.out.println(insertSQL);
                this.stmt.executeUpdate(insertSQL);
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
