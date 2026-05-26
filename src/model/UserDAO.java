package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * 登入驗證
     */
    public User login(String inputAccount, String inputPassword) {
        // ✨ 符合簡報 Spec：student_no
        String sql = "SELECT * FROM Users WHERE student_no = ? AND password = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, inputAccount);
            pstmt.setString(2, inputPassword);
            
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                
                // ✨ 符合簡報 Spec：撈取 student_no 與 name
                user.setStudentNo(rs.getString("student_no")); 
                user.setName(rs.getString("name"));     
                user.setPassword(rs.getString("password"));
                
                // ✨ 符合簡報 Spec：role_level
                String roleStr = rs.getString("role_level");
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
        // ✨ 符合簡報 Spec：使用 student_no, name, role_level
        String sql = "INSERT INTO Users (student_no, name, password, role_level, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentNo);
            pstmt.setString(2, name);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL 執行錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 管理者功能：更新學生帳號狀態（停權或復權）
     */
    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET status = ? WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.toUpperCase());
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("更新使用者狀態失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 管理者功能：更新學生權限層級（升級為 VIP 或降級為 NORMAL）
     */
    public boolean updateUserRole(int userId, String role) {
        // ✨ 符合簡報 Spec：將 role 改為 role_level
        String sql = "UPDATE Users SET role_level = ? WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role.toUpperCase());
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("更新使用者權限失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 管理者功能：抓取所有學生（使用者）清單
     */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                
                // ✨ 符合簡報 Spec：撈取 student_no 與 name
                user.setStudentNo(rs.getString("student_no"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));
                
                // ✨ 符合簡報 Spec：role_level
                String roleStr = rs.getString("role_level");
                String statusStr = rs.getString("status");
                
                if (roleStr != null) {
                    user.setRoleLevel(User.RoleLevel.valueOf(roleStr.toUpperCase()));
                }
                if (statusStr != null) {
                    user.setStatus(User.Status.valueOf(statusStr.toUpperCase()));
                }
                
                list.add(user);
            }
        } catch (SQLException e) {
            System.err.println("獲取使用者清單失敗: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}