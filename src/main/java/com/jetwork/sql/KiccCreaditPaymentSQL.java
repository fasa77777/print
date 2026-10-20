/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentCreaditKicc;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KiccCreaditPaymentSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String setPayment(PaymentCreaditKicc payment) {
        String money = "";
        DBCPConnectionMgr dbcp = ConnectionPool.getInst();
        this.conn = dbcp.getConnection();
        try {
            try {
                this.conn.setAutoCommit(false);
                String payType = "";
                this.logger.info(payment.getR10());
                this.logger.info(payment.getR12());
                this.logger.info(payment.getR13());
                payType = "Y".equals(payment.getR10()) ? "체크카드" : "신용카드";
                if ("TM".equals(payment.getR33())) {
                    payType = "티머니";
                } else if ("EB".equals(payment.getR33())) {
                    payType = "캐시비";
                }
                String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getTotalAmount() + "',N'" + payType + "','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + payment.getR09() + "','" + payment.getJobType() + "',getdate(), N'" + CardCompanyName.getName(payment.getR12().trim()) + "' );";
                this.stmt = this.conn.createStatement(1004, 1007);
                this.stmt.executeUpdate(insertSQL);
                this.conn.commit();
            }
            catch (SQLException e) {
                e.printStackTrace();
                dbcp.freeConnection(this.conn, this.stmt);
            }
        }
        finally {
            dbcp.freeConnection(this.conn, this.stmt);
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
