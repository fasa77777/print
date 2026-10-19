package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.CardCompanyName;
import com.jetwork.model.PaymentKicc;



public class CreaditPaymentSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public String setPayment(PaymentKicc payment) {
		String money = "";
		
		boolean ab = false;
		 DBCPConnectionMgr dbcp = ConnectionPool.getInst();
       // Connection con = null;


		conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			String payType = "";
			logger.info(payment.getR09());
			logger.info(payment.getR11());
			logger.info(payment.getR30());
			logger.info(payment.getR31());
			logger.info(payment.getR32());
			logger.info(payment.getR33());
			if("Y".equals(payment.getR11())) {
				payType = "체크카드";
				ab = true;
			}else if("N".equals(payment.getR11())) {
				payType = "신용카드";	
				ab = true;
			}
			
			if(ab) {
		    	PaymentCK ck = new PaymentCK();
		    	boolean ck1 = ck.setTerminal(payment.getR09());
		    	if(ck1) {
		    		ck.setTerminal1(payment.getR09());
		    		String stringSQL ="select * " + 
							"from [jw_payment] where  (CONVERT(CHAR(10), reg_date, 120) = CONVERT(CHAR(10), getdate(), 120)) "
							+ "and payment_ok = '"+payment.getR09()+"'";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
				//	System.out.println(stringSQL);
					stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					rs = stmt.executeQuery(stringSQL);
					conn.commit();
					int i =1;
					String id = "";
					
					boolean a   = true;
					while(rs.next()){
						a=false;
						System.out.println("중복");
					}
		    		
		    		if(a) {
						String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
								+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES " +
								" ('"+payment.getTerminalID()+"','"+payment.getTotalAmount()+"',N'"+payType+"','"+payment.getPaperSize()+"',"
										+ "'"+payment.getColor()+"','"+payment.getR09()+"','"+payment.getJobType()+"',getdate(), N'"+CardCompanyName.getName(payment.getR12()) +"','"+payment.getTotalPages()+"' );";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						System.out.println(insertSQL);
						stmt.executeUpdate(insertSQL);		
						conn.commit();
		    		}
		    		
	
					
					
					
					
		    	}

			}else {
				 Calendar cal = Calendar.getInstance();
					if("TM".equals(payment.getR33().trim())) {
						payType = "티머니";
						String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
								+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES " +
								" ('"+payment.getTerminalID()+"','"+payment.getTotalAmount()+"',N'"+payType+"','"+payment.getPaperSize()+"',"
										+ "'"+payment.getColor()+"','"+cal.getTimeInMillis()+"','"+payment.getJobType()+"',getdate(), N'티머니','"+payment.getTotalPages()+"' );";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						System.out.println(insertSQL);
						stmt.executeUpdate(insertSQL);		
						conn.commit();
						
					}else if("EB".equals(payment.getR33())) {
						payType = "캐시비";
						String insertSQL = "INSERT INTO [dbo].[jw_payment] ([terminal_id],[payment_pay],[payment_type],"
								+ "[payment_paperType],[payment_colorYN],[payment_ok],[payment_printType],[reg_date],[payment_cardName],[payment_totalCount]) VALUES " +
								" ('"+payment.getTerminalID()+"','"+payment.getTotalAmount()+"',N'"+payType+"','"+payment.getPaperSize()+"',"
										+ "'"+payment.getColor()+"','"+cal.getTimeInMillis()+"','"+payment.getJobType()+"',getdate(), N'캐시비','"+payment.getTotalPages()+"' );";
						stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						System.out.println(insertSQL);
						stmt.executeUpdate(insertSQL);		
						conn.commit();
					}
			}

			//r09 r11 r12 r15 r16 r33
//			logger.info(payment.getTerminalID());
//			logger.info(payment.getTotalAmount());
//			logger.info(payment.getPaperSize());
//			logger.info(payment.getColor());
//			logger.info(payment.getR09());
//			logger.info(payment.getJobType());  CardCompanyName.getName(payment.getR13())
//			logger.info(payment.getR13());

			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
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
