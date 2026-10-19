package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;
import com.jetwork.model.A4Terminal;
import com.jetwork.model.PriceResult;
import com.jetwork.model.Terminal;

public class PaperPriceSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = false;
	Vector v = new Vector();
	
	public Vector<PriceResult> getPrice(A4Terminal terminal) {
		//String money = "";
		
		 DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL ="select * from jw_paper where terminalID = '"+terminal.getTerminalID()+"'";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			while(rs.next()){
				System.out.println("종이가격");
				PriceResult pr = new PriceResult();
				pr.setA4Color(rs.getString("paper_colorYN"));
				pr.setA4PaperPrice(rs.getString("paper_price"));
				pr.setA4PaperSize(rs.getString("paper_size"));
				v.addElement(pr);
				
			}
			String updateSQL = "update [dbo].[jw_terminal]"
					+ " set [agent_version] = '"+terminal.getAgentVersion()+"' , terminalVersion = '"+terminal.getVersion()+"', [update] = getdate() "
					+ " where [terminal_id]  = '"+terminal.getTerminalID()+"' ";
			
			System.out.println(updateSQL);
			stmt.executeUpdate(updateSQL);		
			
				
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt, rs);
		}
		
		System.out.println("userFlag ==");
		return v;
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
