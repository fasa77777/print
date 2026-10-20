/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Printer;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PrinterSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = false;

    public boolean setPrinter(Printer printer) {
        block8: {
            DBCPConnectionMgr dbcp = ConnectionPool.getInst();
            this.conn = dbcp.getConnection();
            try {
                try {
                    this.conn.setAutoCommit(false);
                    String stringSQL = "select * from [SONTECH].[dbo].[printer] where [serialnumber]  = '" + printer.getSerialnumber() + "'";
                    System.out.println(stringSQL);
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    while (this.rs.next()) {
                        System.out.println("등록된 복사기");
                        this.userFlag = true;
                    }
                    if (this.userFlag) {
                        String updateSQL = "UPDATE [dbo].[printer]   SET [devid] = '" + printer.getDevid() + "' " + "     ,[sitename] = N'" + printer.getSitename() + "'  " + "     ,[desc1] = N'" + printer.getDesc() + "'  " + "     ,[errmsg] = N'" + printer.getErrmsg() + "'  " + "     ,[modelname] = N'" + printer.getModelname() + "' " + "     ,[total] = " + printer.getTotal() + " " + "     ,[mono] = " + printer.getMono() + "  " + "     ,[color] = " + printer.getColor() + "  " + "     ,[black] = " + printer.getBlack() + "  " + "     ,[cyan] = " + printer.getCyan() + "  " + "     ,[magenta] = " + printer.getMagenta() + "  " + "     ,[yellow] = " + printer.getYellow() + "  " + "     ,[devicestatus] = N'" + printer.getDevicestatus() + "' " + "     ,[printerstatus] = N'" + printer.getPrinterstatus() + "' " + "     ,[coverstatus] = N'" + printer.getCoverstatus() + "' " + "     ,[macaddr] = '" + printer.getMacaddr() + "' " + "     ,[regdate] = getdate() " + " WHERE [serialnumber] = '" + printer.getSerialnumber() + "'  ";
                        System.out.println(updateSQL);
                        this.stmt.executeUpdate(updateSQL);
                    } else {
                        String insertSQL = "INSERT INTO [dbo].[printer] ([devid] ,[sitename] ,[desc1] ,[errmsg] ,[modelname] ,[total] ,[mono] ,[color] ,[black] ,[cyan] ,[magenta] ,[yellow] ,[devicestatus] ,[printerstatus] ,[coverstatus] ,[serialnumber] ,[macaddr] ,[regdate]) VALUES ( '" + printer.getDevid() + "'  ,N'" + printer.getSitename() + "' ,N'" + printer.getDesc() + "'  ,N'" + printer.getErrmsg() + "' ,N'" + printer.getModelname() + "'  ," + printer.getTotal() + "  ," + printer.getMono() + " ," + printer.getColor() + " ," + printer.getBlack() + "  ," + printer.getCyan() + " ," + printer.getMagenta() + " ," + printer.getYellow() + " ,N'" + printer.getDevicestatus() + "'  ,N'" + printer.getPrinterstatus() + "'  ,N'" + printer.getCoverstatus() + "' ,'" + printer.getSerialnumber() + "' ,'" + printer.getMacaddr() + "' ,getdate() )";
                        System.out.println(insertSQL);
                        this.stmt.executeUpdate(insertSQL);
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
        System.out.println("userFlag ==" + this.userFlag);
        return this.userFlag;
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
