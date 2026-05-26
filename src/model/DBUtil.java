package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    // 1. 資料庫連線網址（library_system 是你剛才建的資料庫名稱）
	private static final String URL = "jdbc:mysql://localhost:3306/library_system?serverTimezone=Asia/Taipei&useSSL=false&characterEncoding=UTF-8";
    
    // 2. MySQL 的帳號
    private static final String USER = "root"; 
    
    // 3. MySQL 的密碼（改成你安裝 .pkg 時設定的那組密碼）
    private static final String PASSWORD = "15791579"; 

    public static Connection getConnection() throws SQLException {
    	
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}