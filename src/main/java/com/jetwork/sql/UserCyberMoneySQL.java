package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;

public class UserCyberMoneySQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = true;
	
	public String getUser(String terminalID,String userID) {
		String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL =" SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '"+userID+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			int i = 1;
			while(rs.next()){
				//money = rs.getString(2);
				money = rs.getString("cyberMoney");
				System.out.println("money ====="+money);
				System.out.println("getString ====="+rs.getString(i));
				userFlag = false;
				i++;
			}
			
			if(userFlag){
				
				stringSQL =" SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = '"+terminalID+"'";
				System.out.println(stringSQL);
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery(stringSQL);
				String franID = "";
				while(rs.next()){
					
					System.out.println(rs.getString(3));
					franID =rs.getString(3);
				}
				
				System.out.println(franID);
				
				String insertSQL = "INSERT INTO [dbo].[cybermoney] ([cardNumber],[cyberMoney],[fran_id],[reg_date]) VALUES " +
						" ('"+userID+"',0,'"+franID+"',getdate());";
				
				//System.out.println(insertSQL);
				stmt.executeUpdate(insertSQL);		
				
				money = "0";
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt, rs);
		}
		
		
		return money;
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
