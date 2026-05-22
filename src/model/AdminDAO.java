package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {
    // 檢查登入者是否為管理員
    public boolean isAdmin(String username) {
        String sql = "SELECT role FROM Users WHERE username = ? AND role = 'ADMIN'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // 有找到資料就回傳 true
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 這裡之後我們會補上刪除書籍、會員管理等 SQL 指令
}
