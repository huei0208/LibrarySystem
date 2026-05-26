package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {
    
    /**
     * 驗證管理者帳號與密碼是否正確
     */
    public boolean verifyAdmin(String username, String password) {
        String sql = "SELECT * FROM Admins WHERE username = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // 如果有找到資料，代表帳號密碼正確，回傳 true
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}