/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.PaymentKicc;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CyberMoneyPaymentSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = true;

    public String setPayment(PaymentKicc payment) {
        String money = "";
        DBCPConnectionMgr dbcp = ConnectionPool.getInst();
        this.conn = dbcp.getConnection();
        try {
            try {
                this.conn.setAutoCommit(false);
                String stringSQL1 = "select b.fran_name from [dbo].[jw_terminal] a, [dbo].[jw_franchise] b  where a.fran_id = b.fran_id and a.terminal_id = '" + payment.getTerminalID() + "'";
                this.stmt = this.conn.createStatement(1004, 1007);
                this.rs = this.stmt.executeQuery(stringSQL1);
                String fran_name = "";
                while (this.rs.next()) {
                    fran_name = this.rs.getString(1);
                }
                System.out.println("fran_name ==" + fran_name);
                String stringSQL = " SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '" + payment.getUserCard() + "'";
                this.stmt = this.conn.createStatement(1004, 1007);
                this.rs = this.stmt.executeQuery(stringSQL);
                while (this.rs.next()) {
                    money = this.rs.getString("cyberMoney");
                    this.userFlag = false;
                }
                stringSQL = "select * from [dbo].[jw_payment] where [userCard] = '" + payment.getUserCard() + "' and [cyberNo] = '" + payment.getCyberNo() + "'";
                this.stmt = this.conn.createStatement(1004, 1007);
                this.rs = this.stmt.executeQuery(stringSQL);
                boolean ck = false;
                while (this.rs.next()) {
                    ck = true;
                }
                if (ck) {
                    money = "0";
                } else if (this.userFlag) {
                    stringSQL = " SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = '" + payment.getTerminalID() + "'";
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    String franID = "";
                    while (this.rs.next()) {
                        System.out.println(this.rs.getString(3));
                        franID = this.rs.getString(3);
                    }
                    System.out.println(franID);
                    String insertSQL = "INSERT INTO [dbo].[cybermoney] ([cardNumber],[cyberMoney],[fran_id],[reg_date]) VALUES  ('" + payment.getUserCard() + "',0,'" + franID + "',getdate());";
                    this.stmt.executeUpdate(insertSQL);
                    money = "0";
                } else {
                    stringSQL = " SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '" + payment.getUserCard() + "'";
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    while (this.rs.next()) {
                        money = this.rs.getString("cyberMoney");
                    }
                    int moneyK = Integer.parseInt(money);
                    money = String.valueOf(moneyK -= Integer.parseInt(payment.getTotalAmount()));
                    System.out.println("moneyk ==== " + moneyK);
                    if (moneyK > 0) {
                        String updateSQL = "update [dbo].[cybermoney] set  [cyberMoney] = '" + moneyK + "' " + " where [cardNumber] = '" + payment.getUserCard() + "' ";
                        this.stmt = this.conn.createStatement(1004, 1007);
                        System.out.println(updateSQL);
                        this.stmt.executeUpdate(updateSQL);
                        String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[userCard],[cyberNo],[payment_totalCount]) VALUES  ('" + payment.getTerminalID() + "','" + payment.getTotalAmount() + "',N'사이버머니','" + payment.getPaperSize() + "'," + "'" + payment.getColor() + "','" + payment.getCyberNo() + "','" + payment.getJobType() + "',getdate(), N'" + fran_name + "', '" + payment.getUserCard() + "','" + payment.getCyberNo() + "','" + payment.getTotalPages() + "' );";
                        this.stmt = this.conn.createStatement(1004, 1007);
                        System.out.println(insertSQL);
                        this.stmt.executeUpdate(insertSQL);
                    } else {
                        money = "0";
                    }
                    money = String.valueOf(moneyK);
                    System.out.println("money ==== " + moneyK);
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
