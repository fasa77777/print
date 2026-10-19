package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;
import com.jetwork.model.PaymentKicc;
import com.jetwork.model.PaymentKyocera;

public class CyberMoneyKyoceraPaymentSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = true;
	
	public String setPayment(PaymentKyocera payment) {
		String money = "";
		 DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			System.out.println("1");
			
			
			////////////////////////////////////

			String stringSQL =" SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '"+payment.getUserCard()+"'";
		//	System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			while(rs.next()){
				//money = rs.getString(2);
				money = rs.getString("cyberMoney");
				userFlag = false;
			}
			System.out.println("2");
			
			//////////
			stringSQL ="select * from [dbo].[jw_payment] where [userCard] = '"+payment.getUserCard()+"' and [cyberNo] = '"+payment.getCyberNo()+"'";
			//	System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			boolean ck = false;
			while(rs.next()){
				ck = true;
			}
			
			if(ck) {
				money = "0";
			}else {
		////////////
				if(userFlag){
					
					stringSQL =" SELECT * FROM [dbo].[jw_terminal] WHERE [terminal_id] = '"+payment.getTerminalID()+"'";
					//System.out.println(stringSQL);
					stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					rs = stmt.executeQuery(stringSQL);
					String franID = "";
					while(rs.next()){
						
						System.out.println(rs.getString(3));
						franID =rs.getString(3);
					}
					
					System.out.println(franID);
					
					String insertSQL = "INSERT INTO [dbo].[cybermoney] ([cardNumber],[cyberMoney],[fran_id],[reg_date]) VALUES " +
							" ('"+payment.getUserCard()+"',0,'"+franID+"',getdate());";
					
					//System.out.println(insertSQL);
					stmt.executeUpdate(insertSQL);		
					
					money = "0";
				}else {
					//cyber money 계산
					 stringSQL =" SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '"+payment.getUserCard().trim()+"'";
						System.out.println(stringSQL);
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						rs = stmt.executeQuery(stringSQL);
						while(rs.next()){
							//money = rs.getString(2);
							money = rs.getString("cyberMoney");
							//userFlag = false;
						}
					int moneyK = Integer.parseInt(money);
					
					System.out.println("payment.getTotalAmount() == ["+payment.getTotalAmount().replaceAll("\"", "").trim()+"]");
					System.out.println("moneyK == ["+moneyK+"]");
					moneyK = moneyK - Integer.parseInt(payment.getTotalAmount().replaceAll("\"", "").trim());
					money = moneyK+"";
					System.out.println("moneyk ==== "+moneyK);
					
					if(moneyK > 0) {
						//cyber money 업데이트
						String updateSQL = "update [dbo].[cybermoney]"
								+ " set  [cyberMoney] = '"+moneyK+"' "
								+ " where [cardNumber] = '"+payment.getUserCard()+"' ";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						System.out.println(updateSQL);
						stmt.executeUpdate(updateSQL);	
						//
						
						stringSQL =  "select b.fran_name from [dbo].[jw_terminal] a, [dbo].[jw_franchise] b  where a.fran_id = b.fran_id and a.terminal_id = '"+payment.getTerminalID()+"'";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						rs = stmt.executeQuery(stringSQL);
						String fran_name = "";
						while(rs.next()){
							//money = rs.getString(2);
							fran_name = rs.getString(1);
							//userFlag = false;
						}
						
						System.out.println("fran_name =="+fran_name);
						
						if("1".equals(payment.getColor())) {
							payment.setColor("true");
						}else if("2".equals(payment.getColor())) {
							payment.setColor("false");
						}
						
						String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
								+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[userCard],[cyberNo],[payment_totalCount]) VALUES " +
								" ('"+payment.getTerminalID()+"','"+payment.getTotalAmount()+"',N'사이버머니','"+payment.getPaperSize()+"',"
										+ "'"+payment.getColor()+"','"+payment.getCyberNo()+"','"+payment.getJobType()+"',getdate(), N'"+fran_name+"', '"+payment.getUserCard()+"','"+payment.getCyberNo()+"','"+payment.getTotalPages()+"' );";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						
						System.out.println(insertSQL);
						stmt.executeUpdate(insertSQL);	
					}else {
						money = "0";
					}
		
					money = moneyK+"";
					System.out.println("money ==== "+money);
					
				}
				////////////////////////////////////
			}	
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			dbcp.freeConnection(conn, stmt, rs);
			//this.freeConn();
		}
		
		
		return money;
	}
	
	public String reMoney(String card) {
		 DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		conn  = dbcp.getConnection();
		String money = "";
		try {
			conn.setAutoCommit(false);
			System.out.println("1");	
			//cyber money 계산
			
			 String stringSQL =" SELECT * FROM [dbo].[cybermoney] WHERE [cardNumber] = '"+card+"'";
			//	System.out.println(stringSQL);
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery(stringSQL);
				while(rs.next()){
					//money = rs.getString(2);
					money = rs.getString("cyberMoney");
					//userFlag = false;
				}
			int moneyK = Integer.parseInt(money);
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
