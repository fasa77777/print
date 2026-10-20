package com.jetwork.bin;


import java.sql.SQLException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.jetwork.sql.DBCPConnectionMgr;
import com.stech.Network.SocketServerDriver;



@SpringBootApplication
public class JetworkServerApplication {
	public static String TerminalIP = "";
	public static String terID = "";
	public static boolean chk = false;
	
	public static boolean chk1 = false;
	
	public static boolean result = false;
	
	public static boolean chk2 = true;
	public static int printPrice = 0;
	public static String printColor="";
	public static String printPaperSize = "";
	public static String printCount = "";
	public static void main(String[] args) {
		SpringApplication.run(JetworkServerApplication.class, args);
		//ConnectionPool cp = new ConnectionPool();
		//cp.connection();
		
	    Runnable runnable1 = new SocketServerDriver();


	    Thread thread3 = new Thread(runnable1);
	    thread3.setName("SocketServerDriver #1");

		
	    thread3.start();
	    
		//DBCPConnectionMgr dm = new DBCPConnectionMgr();
		//System.out.println(dm.getConnection());
	//	ConnectionSQL a = new ConnectionSQL();
	//	try {
	//		a.connection();
	//	} catch (SQLException e) {
	//		e.printStackTrace();
	//	}
	}	
	
	



}
