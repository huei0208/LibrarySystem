package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    /**
     * 取得使用者的通知數量 (顯示在右上角的紅色數字)
     */
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return 0;
    }

    /**
     * 新增系統通知 (具有防呆機制，避免重複塞入一模一樣的訊息)
     */
    public void addNotification(int userId, String message) {
        String checkSql = "SELECT * FROM Notifications WHERE user_id = ? AND message = ?";
        String insertSql = "INSERT INTO Notifications (user_id, message) VALUES (?, ?)";
        
        try (Connection conn = DBUtil.getConnection()) {
            // 先檢查是不是已經有同一條訊息了 (避免每次登入都狂塞同樣的逾期警告)
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, userId);
                psCheck.setString(2, message);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) return; // 已經有了就直接跳出
            }
            
            // 如果沒有，就新增進去
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setInt(1, userId);
                psInsert.setString(2, message);
                psInsert.executeUpdate();
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    /**
     * 撈取使用者的所有信件
     */
    public List<Object[]> getUserNotifications(int userId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT notification_id, message, created_at FROM Notifications WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            // 定義日期格式
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            while (rs.next()) {
                list.add(new Object[]{
                    false, // 第一欄位留給 GUI 的 Checkbox 使用 (預設未勾選)
                    rs.getInt("notification_id"), // 隱藏的 ID，刪除時需要用到
                    rs.getString("message"),
                    rs.getTimestamp("created_at").toLocalDateTime().format(formatter) // 美化時間
                });
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return list;
    }

    /**
     * 刪除指定的一條訊息
     */
    public void deleteNotification(int notiId) {
        String sql = "DELETE FROM Notifications WHERE notification_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notiId);
            pstmt.executeUpdate();
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }
    /**
     * 一鍵刪除特定使用者的所有通知
     */
    public boolean deleteAllNotifications(int userId) {
        // 請確認你的資料庫中，通知表的名稱是否為 notifications (如果是其他名稱請替換)
        String sql = "DELETE FROM notifications WHERE user_id = ?"; 
        
        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // 只要有刪除到資料就回傳 true
            
        } catch (java.sql.SQLException e) {
            System.err.println("刪除全部通知失敗: " + e.getMessage());
            return false;
        }
    }
}