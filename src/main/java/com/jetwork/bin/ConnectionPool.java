/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.sql.DBCPConnectionMgr;

public class ConnectionPool {
    private static DBCPConnectionMgr dm;

    public static DBCPConnectionMgr getInst() {
        return dm;
    }

    public void connection() {
        dm = DBCPConnectionMgr.getInstance();
    }
}
