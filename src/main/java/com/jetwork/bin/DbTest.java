package com.jetwork.bin;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;

public class DbTest {

	private  JdbcTemplate jdbcTemplate;

	public void connection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		System.out.println(conn);
	}
}