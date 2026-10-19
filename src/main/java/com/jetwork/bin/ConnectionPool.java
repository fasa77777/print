package com.jetwork.bin;

import java.sql.Connection;
import java.sql.SQLException;

import com.jetwork.sql.DBCPConnectionMgr;

public class ConnectionPool {
	private static DBCPConnectionMgr dm ;  
	
	public static DBCPConnectionMgr getInst(){		
		return dm;
	}
	
	
    public void connection() {
    	dm = DBCPConnectionMgr.getInstance();
    	
    }
}
