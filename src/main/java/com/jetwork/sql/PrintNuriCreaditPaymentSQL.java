package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.bin.ConnectionSQL;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentKicc;
import com.jetwork.model.PrintNuriPay;

public class PrintNuriCreaditPaymentSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public String setPayment(PrintNuriPay payment) {
		String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			String payType = "";

			if("Y".equals(payment.getPayType())) {
				payType = "체크카드";	
			}else  {
				payType = "신용카드";				
			}
			
			if("TM".equals(payment.getPayTypeTM())) {
				payType = "티머니";	
			}else if("EB".equals(payment.getPayTypeTM())) {
				payType = "캐시비";	
			}
			
			System.out.println("111111111111");
			//r09 r11 r12 r15 r16 r33
//			logger.info(payment.getTerminalID());
//			logger.info(payment.getTotalAmount());
//			logger.info(payment.getPaperSize());
//			logger.info(payment.getColor());
//			logger.info(payment.getR09());
//			logger.info(payment.getJobType());  CardCompanyName.getName(payment.getR13())
//			logger.info(payment.getR13());
			String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
					+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES " +
					" ('"+payment.getTerminalID()+"','"+payment.getPrice()+"',N'"+payType+"','"+payment.getPaperSize()+"',"
							+ "'"+payment.getColor()+"','"+payment.getPayOK()+"','print',getdate(), N'"+CardCompanyName.getName(payment.getCraditName().trim()) +"','"+payment.getCount()+"' );";
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			System.out.println(insertSQL);
			stmt.executeUpdate(insertSQL);		
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
