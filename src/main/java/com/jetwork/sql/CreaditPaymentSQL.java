/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentKicc;
import com.jetwork.sql.DBCPConnectionMgr;
import com.jetwork.sql.PaymentCK;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreaditPaymentSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String setPayment(PaymentKicc payment) {
        String money = "";
        boolean ab = false;
        DBCPConnectionMgr dbcp = ConnectionPool.getInst();
        this.conn = dbcp.getConnection();
        try {
            try {
                this.conn.setAutoCommit(false);
                String payType = "";
                this.logger.info(payment.getR09());
                this.logger.info(payment.getR11());
                this.logger.info(payment.getR30());
                this.logger.info(payment.getR31());
                this.logger.info(payment.getR32());
                this.logger.info(payment.getR33());
                if ("Y".equals(payment.getR11())) {
                    payType = "체크카드";
                    ab = true;
                } else if ("N".equals(payment.getR11())) {
                    payType = "신용카드";
                    ab = true;
                }
                if (ab) {
                    PaymentCK ck = new PaymentCK();
                    boolean ck1 = ck.setTerminal(payment.getR09());
                    if (ck1) {
                        ck.setTerminal1(payment.getR09());
                        String stringSQL = "select * from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) and payment_ok = '" + payment.getR09() + "'";
                        this.stmt = this.conn.createStatement(1004, 1007);
                        this.rs = this.stmt.executeQuery(stringSQL);
                        this.conn.commit();
                        boolean i = true;
                        String id = "";
                        boolean a = true;
                        while (this.rs.next()) {
                            a = false;
                            System.out.println("중복");
                        }
                        if (a) {
                            String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getTotalAmount() + "',N'" + payType + "','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + payment.getR09() + "','" + payment.getJobType() + "',getdate(), N'" + CardCompanyName.getName(payment.getR12()) + "','" + payment.getTotalPages() + "' );";
                            this.stmt = this.conn.createStatement(1004, 1007);
                            System.out.println(insertSQL);
                            this.stmt.executeUpdate(insertSQL);
                            this.conn.commit();
                        }
                    }
                } else {
                    Calendar cal = Calendar.getInstance();
                    if ("TM".equals(payment.getR33().trim())) {
                        payType = "티머니";
                        String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getTotalAmount() + "',N'" + payType + "','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + cal.getTimeInMillis() + "','" + payment.getJobType() + "',getdate(), N'티머니','" + payment.getTotalPages() + "' );";
                        this.stmt = this.conn.createStatement(1004, 1007);
                        System.out.println(insertSQL);
                        this.stmt.executeUpdate(insertSQL);
                        this.conn.commit();
                    } else if ("EB".equals(payment.getR33())) {
                        payType = "캐시비";
                        String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getTotalAmount() + "',N'" + payType + "','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + cal.getTimeInMillis() + "','" + payment.getJobType() + "',getdate(), N'캐시비','" + payment.getTotalPages() + "' );";
                        this.stmt = this.conn.createStatement(1004, 1007);
                        System.out.println(insertSQL);
                        this.stmt.executeUpdate(insertSQL);
                        this.conn.commit();
                    }
                }
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