/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.sql;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Terminal;
import com.jetwork.sql.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TerminalInfoSQL {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private boolean userFlag = false;

    public boolean setTerminal(Terminal terminal) {
        block7: {
            DBCPConnectionMgr dbcp = ConnectionPool.getInst();
            this.conn = dbcp.getConnection();
            try {
                try {
                    this.conn.setAutoCommit(false);
                    String stringSQL = "select * from [dbo].[jw_terminal] where [terminal_id]  = '" + terminal.getTerminalID() + "'" + " and [serial] = '" + terminal.getSerial() + "' ";
                    System.out.println(stringSQL);
                    this.stmt = this.conn.createStatement(1004, 1007);
                    this.rs = this.stmt.executeQuery(stringSQL);
                    while (this.rs.next()) {
                        System.out.println("사용중인 복사기");
                        this.userFlag = true;
                    }
                    if (this.userFlag) {
                        System.out.println("사용중인 복사기   111");
                        String updateSQL = "update [dbo].[jw_terminal] set [agent_version] = '" + terminal.getAgentVersion() + "' , [update] = getdate() , terminalVersion = '" + terminal.getTerminalVersion() + "' " + " where [terminal_id]  = '" + terminal.getTerminalID() + "' ";
                        System.out.println(updateSQL);
                        this.stmt.executeUpdate(updateSQL);
                    }
                    this.conn.commit();
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
