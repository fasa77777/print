/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.sql.DBConnectionManager;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionSQL {
    private static Connection conn;
    private static DBConnectionManager dm;

    public static Connection getConnection() {
        return conn;
    }

    public void connection() throws SQLException {
        dm = DBConnectionManager.getInstance();
        conn = dm.getConnection("mssql");
    }
}
