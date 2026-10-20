/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.A4Terminal;
import com.jetwork.model.PriceResult;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class PaperPriceSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = false;
    Vector v = new Vector();

    public Vector<PriceResult> getPrice(A4Terminal terminal) {
        block6: {
            DBCPConnectionMgr dbcp = ConnectionPool.getInst();
            this.conn = dbcp.getConnection();
            try {
                try {
                    this.conn.setAutoCommit(false);
                    String stringSQL = "select * from jw_paper where terminalID = '" + terminal.getTerminalID() + "'";
                    System.out.println(stringSQL);
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    while (this.rs.next()) {
                        System.out.println("종이가격");
                        PriceResult pr = new PriceResult();
                        pr.setA4Color(this.rs.getString("paper_colorYN"));
                        pr.setA4PaperPrice(this.rs.getString("paper_price"));
                        pr.setA4PaperSize(this.rs.getString("paper_size"));
                        this.v.addElement(pr);
                    }
                    String updateSQL = "update [dbo].[jw_terminal] set [agent_version] = '" + terminal.getAgentVersion() + "' , terminalVersion = '" + terminal.getVersion() + "', [update] = getdate() " + " where [terminal_id]  = '" + terminal.getTerminalID() + "' ";
                    System.out.println(updateSQL);
                    this.stmt.executeUpdate(updateSQL);
                    this.conn.commit();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                    dbcp.freeConnection(this.conn, this.stmt, this.rs);
                    break block6;
                }
            }
            catch (Throwable throwable) {
                dbcp.freeConnection(this.conn, this.stmt, this.rs);
                throw throwable;
            }
            dbcp.freeConnection(this.conn, this.stmt, this.rs);
        }
        System.out.println("userFlag ==");
        return this.v;
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
