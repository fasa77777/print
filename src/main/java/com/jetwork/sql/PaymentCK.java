package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;
import com.jetwork.model.Terminal;

public class PaymentCK {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = true;
	
	public boolean setTerminal(String paymentOK) {
		//String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL ="select * " + 
					"from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) "
					+ "and payment_ok = '"+paymentOK+"'";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			while(rs.next()){
				//money = rs.getString(2);
				//money = rs.getString("cyberMoney");
				userFlag = false;
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	
	public void setTerminal1(String paymentOK) {
		//String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL ="select * " + 
					"from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) "
					+ "and payment_ok = '"+paymentOK+"'";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			conn.commit();
			int i =1;
			String id = "";
			while(rs.next()){
				//money = rs.getString(2);
				//money = rs.getString("cyberMoney");
				id = rs.getString("id");
				System.out.println(id);
				i++;
			}
			
			if(i>1) {
				
				System.out.println("중복삭제  ===="+id);
				stringSQL ="delete   FROM [dbo].[jw_payment] where id = "+id;         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
				System.out.println(stringSQL);
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				stmt.executeUpdate(stringSQL);		
				conn.commit();
			}
			

			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt, rs);
		}
		
		System.out.println("userFlag ==");
	
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
