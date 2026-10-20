package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;
import com.jetwork.model.Terminal;

public class TerminalInfoSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = false;
	
	public boolean setTerminal(Terminal terminal) {
		//String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL ="select * from [dbo].[jw_terminal] where [terminal_id]  = '"+terminal.getTerminalID()+"'"
					+ " and [serial] = '"+terminal.getSerial()+"' ";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			while(rs.next()){
				//money = rs.getString(2);
				//money = rs.getString("cyberMoney");
				
				System.out.println("사용중인 복사기");
				userFlag = true;
			}
			
			if(userFlag){
				System.out.println("사용중인 복사기   111");
					
				String updateSQL = "update [dbo].[jw_terminal]"
						+ " set [agent_version] = '"+terminal.getAgentVersion()+"' , [update] = getdate() "
						+ " where [terminal_id]  = '"+terminal.getTerminalID()+"' ";
				
				System.out.println(updateSQL);
				stmt.executeUpdate(updateSQL);		
				
		
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt, rs);
		}
		
		System.out.println("userFlag ==");
		return userFlag;
	}
	
	
	public void freeConn(){
		try {
			if(stmt != null){
				stmt.close();
			}
			if(rs != null){
				rs.close();
			}
		
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//dm.freeConnection("mssql", conn);
	}
}
