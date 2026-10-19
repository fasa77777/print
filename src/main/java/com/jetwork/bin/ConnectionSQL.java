package com.jetwork.bin;

import java.sql.Connection;
import java.sql.SQLException;

import com.jetwork.sql.DBConnectionManager;

public class ConnectionSQL {
	private static Connection conn;
	private static DBConnectionManager dm ;  
	
	public static Connection getConnection(){		
		return conn;
	}
	
	
    public void connection() throws SQLException {
    	dm = DBConnectionManager.getInstance();
		conn = dm.getConnection("mssql");
    }
}
