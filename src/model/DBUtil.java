package model;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    // 改成你正確的資料庫名稱 (library_system)
    private static final String URL = "jdbc:mysql://localhost:3306/library_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    // 換成你的正確密碼
    private static final String PASSWORD = "15791579";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}