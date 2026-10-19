package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentCreaditKicc;
import com.jetwork.model.PaymentKicc;

public class KiccCreaditPaymentSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public String setPayment(PaymentCreaditKicc payment) {
		String money = "";
		
		 DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			String payType = "";
			logger.info(payment.getR10());
	
			logger.info(payment.getR12());
			logger.info(payment.getR13());

			if("Y".equals(payment.getR10())) {
				payType = "체크카드";	
			}else  {
				payType = "신용카드";				
			}
			
			if("TM".equals(payment.getR33())) {
				payType = "티머니";	
			}else if("EB".equals(payment.getR33())) {
				payType = "캐시비";	
			}
			//r09 r11 r12 r15 r16 r33
//			logger.info(payment.getTerminalID());
//			logger.info(payment.getTotalAmount());
//			logger.info(payment.getPaperSize());
//			logger.info(payment.getColor());
//			logger.info(payment.getR09());
//			logger.info(payment.getJobType());  CardCompanyName.getName(payment.getR13())
//			logger.info(payment.getR13());
			String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
					+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName]) VALUES " +
					" ('"+payment.getTerminalID()+"','"+payment.getTotalAmount()+"',N'"+payType+"','"+payment.getPaperSize()+"',"
							+ "'"+payment.getColor()+"','"+payment.getR09()+"','"+payment.getJobType()+"',getdate(), N'"+CardCompanyName.getName(payment.getR12().trim()) +"' );";
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			//System.out.println(insertSQL);
			stmt.executeUpdate(insertSQL);		
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt);
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
