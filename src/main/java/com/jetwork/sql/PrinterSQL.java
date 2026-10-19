package com.jetwork.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jetwork.bin.ConnectionPool;
import com.jetwork.model.Printer;
/**

SELECT TOP (1000) [devid]
      ,[sitename]
      ,[desc]
      ,[errmsg]
      ,[modelname]
      ,[total]
      ,[mono]
      ,[color]
      ,[black]
      ,[cyan]
      ,[magenta]
      ,[yellow]
      ,[devicestatus]
      ,[printerstatus]
      ,[coverstatus]
      ,[serialnumber]
      ,[macaddr]
  FROM [SONTECH].[dbo].[printer]
  
  
  
  
INSERT INTO [dbo].[printer] ([devid] ,[sitename] ,[desc] ,[errmsg] ,[modelname] ,[total] ,[mono] ,[color] ,[black] ,[cyan] ,[magenta] ,[yellow] ,[devicestatus] ,[printerstatus] ,[coverstatus] ,[serialnumber] ,[macaddr] ,[regdate])
     VALUES
           (<devid, nvarchar(50),>  ,<sitename, nvarchar(50),> ,<desc, nvarchar(50),>  ,<errmsg, nvarchar(50),> ,<modelname, nvarchar(50),>  ,<total, int,>  ,<mono, int,> ,<color, int,> ,<black, int,>
           ,<cyan, int,> ,<magenta, int,> ,<yellow, int,> ,<devicestatus, nvarchar(50),>  ,<printerstatus, nvarchar(50),>  ,<coverstatus, nvarchar(50),> ,<serialnumber, nvarchar(50),> ,<macaddr, nvarchar(50),> ,<regdate, datetime,>)

 "UPDATE [dbo].[printer] "
 "  SET [devid] = <devid, nvarchar(50),> " +
 "     ,[sitename] = <sitename, nvarchar(50),>  " +
 "     ,[desc] = <desc, nvarchar(50),>  " +
 "     ,[errmsg] = <errmsg, nvarchar(50),>  " +
 "     ,[modelname] = <modelname, nvarchar(50),>  " +
 "     ,[total] = <total, int,>  " +
 "     ,[mono] = <mono, int,>  " +
 "     ,[color] = <color, int,>  " +
 "     ,[black] = <black, int,>  " +
 "     ,[cyan] = <cyan, int,>  " +
 "     ,[magenta] = <magenta, int,>  " +
 "     ,[yellow] = <yellow, int,>  " +
 "     ,[devicestatus] = <devicestatus, nvarchar(50),> " +
 "     ,[printerstatus] = <printerstatus, nvarchar(50),> " +
 "     ,[coverstatus] = <coverstatus, nvarchar(50),> " +
 "     ,[serialnumber] = <serialnumber, nvarchar(50),> " +
 "     ,[macaddr] = <macaddr, nvarchar(50),> " +
 "     ,[regdate] = <regdate, datetime,> " +
 " WHERE [serialnumber] = <serialnumber, nvarchar(50),> " ;
 * 
 * 
 * 
 INSERT INTO [dbo].[printer] ([devid] ,[sitename] ,[desc1] ,[errmsg] ,[modelname] ,[total] ,[mono] ,[color] ,[black] ,[cyan] ,[magenta] ,[yellow] ,[devicestatus] ,[printerstatus] ,[coverstatus] ,[serialnumber] ,[macaddr] ,[regdate])"
						+ " VALUES ( '"+printer.getDevid()+"'  ,'"+printer.getSitename()+"' ,'"+printer.getDesc()+"'  ,'"+printer.getErrmsg()+"' ,'"+printer.getModelname()+"'  ,"+printer.getTotal()+"  ,"+printer.getMono()+" ,"+printer.getColor()+" ,"+printer.getBlack()+""
						+ "  ,"+printer.getCyan()+" ,"+printer.getMagenta()+" ,"+printer.getYellow()+" ,'"+printer.getDevicestatus()+"'  ,'"+printer.getPrinterstatus()+"'  ,'"+printer.getCoverstatus()+"' ,'"+printer.getSerialnumber()+"' ,'"+printer.getMacaddr()+"' ,getdate() )";
				
 * **/


public class PrinterSQL {
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	private boolean userFlag = false;
	
	public boolean setPrinter(Printer printer) {
		//String money = "";
		
		DBCPConnectionMgr dbcp = ConnectionPool.getInst();
		 conn  = dbcp.getConnection();
		
		try {
			conn.setAutoCommit(false);
			
			
			String stringSQL ="select * from [SONTECH].[dbo].[printer] where [serialnumber]  = '"+printer.getSerialnumber()+"'";
			//		+ " and [serial] = '"+terminal.getSerial()+"' ";         //and [TerminalSerial] = '"+terminal.getTerminalSerial()+"'";
			System.out.println(stringSQL);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(stringSQL);
			while(rs.next()){
				//money = rs.getString(2);
				//money = rs.getString("cyberMoney");
				
				System.out.println("등록된 복사기");
				userFlag = true;
			}
			
			if(userFlag){
				//System.out.println("사용중인 복사기   111");
					
				String updateSQL =  "UPDATE [dbo].[printer] " +
						 "  SET [devid] = '"+printer.getDevid()+"' " +
						 "     ,[sitename] = N'"+printer.getSitename()+"'  " +
						 "     ,[desc1] = N'"+printer.getDesc()+"'  " +
						 "     ,[errmsg] = N'"+printer.getErrmsg()+"'  " +
						 "     ,[modelname] = N'"+printer.getModelname()+"' " +
						 "     ,[total] = "+printer.getTotal()+" " +
						 "     ,[mono] = "+printer.getMono()+"  " +
						 "     ,[color] = "+printer.getColor()+"  " +
						 "     ,[black] = "+printer.getBlack()+"  " +
						 "     ,[cyan] = "+printer.getCyan()+"  " +
						 "     ,[magenta] = "+printer.getMagenta()+"  " +
						 "     ,[yellow] = "+printer.getYellow()+"  " +
						 "     ,[devicestatus] = N'"+printer.getDevicestatus()+"' " +
						 "     ,[printerstatus] = N'"+printer.getPrinterstatus()+"' " +
						 "     ,[coverstatus] = N'"+printer.getCoverstatus()+"' " +
						 "     ,[macaddr] = '"+printer.getMacaddr()+"' " +
						 "     ,[regdate] = getdate() " +
						 " WHERE [serialnumber] = '"+printer.getSerialnumber()+"'  " ;
				
				System.out.println(updateSQL);
				stmt.executeUpdate(updateSQL);		
				
		
			}else {
				String insertSQL = "INSERT INTO [dbo].[printer] ([devid] ,[sitename] ,[desc1] ,[errmsg] ,[modelname] ,[total] ,[mono] ,[color] ,[black] ,[cyan] ,[magenta] ,[yellow] ,[devicestatus] ,[printerstatus] ,[coverstatus] ,[serialnumber] ,[macaddr] ,[regdate])"
						+ " VALUES ( '"+printer.getDevid()+"'  ,N'"+printer.getSitename()+"' ,N'"+printer.getDesc()+"'  ,N'"+printer.getErrmsg()+"' ,N'"+printer.getModelname()+"'  ,"+printer.getTotal()+"  ,"+printer.getMono()+" ,"+printer.getColor()+" ,"+printer.getBlack()+""
						+ "  ,"+printer.getCyan()+" ,"+printer.getMagenta()+" ,"+printer.getYellow()+" ,N'"+printer.getDevicestatus()+"'  ,N'"+printer.getPrinterstatus()+"'  ,N'"+printer.getCoverstatus()+"' ,'"+printer.getSerialnumber()+"' ,'"+printer.getMacaddr()+"' ,getdate() )";
				
				System.out.println(insertSQL);
				stmt.executeUpdate(insertSQL);		
				
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}finally{
			//this.freeConn();
			dbcp.freeConnection(conn, stmt, rs);
		}
		
		System.out.println("userFlag =="+userFlag);
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
