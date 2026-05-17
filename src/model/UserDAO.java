package model;

import java.sql.*;

public class UserDAO {

    /**
     * 登入驗證
     */
    public User login(String inputAccount, String inputPassword) {
        // 關鍵修正：將 users 改為 Users
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, inputAccount);
            pstmt.setString(2, inputPassword);
            
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setStudentNo(rs.getString("username")); 
                user.setName(rs.getString("real_name"));     
                user.setPassword(rs.getString("password"));
                
                String roleStr = rs.getString("role");
                String statusStr = rs.getString("status");
                
                if (roleStr != null) {
                    user.setRoleLevel(User.RoleLevel.valueOf(roleStr.toUpperCase()));
                }
                if (statusStr != null) {
                    user.setStatus(User.Status.valueOf(statusStr.toUpperCase()));
                }
                
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 註冊新使用者
     */
    public boolean register(String studentNo, String name, String password, String role) {
        // 關鍵修正：將 users 改為 Users
        String sql = "INSERT INTO Users (username, real_name, password, role, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentNo);
            pstmt.setString(2, name);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // 在 Console 印出具體錯誤原因，方便我們除錯
            System.err.println("SQL 執行錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}