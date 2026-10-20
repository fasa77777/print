/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DbTest {
    private JdbcTemplate jdbcTemplate;

    public void connection() throws SQLException {
        Connection conn = this.jdbcTemplate.getDataSource().getConnection();
        System.out.println(conn);
    }
}
