package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
    /**
     * 統計各書籍主題的借閱熱度
     */
    public java.util.Map<String, Integer> getCategoryBorrowStats() {
    java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
    
    // 💡 精確對齊：使用 borrow_records (底線) 與 subjects (複數)
    String sql = "SELECT b.subjects, COUNT(r.record_id) as borrow_count " +
                 "FROM borrow_records r " +
                 "JOIN books b ON r.book_id = b.book_id " +
                 "GROUP BY b.subjects " +
                 "ORDER BY borrow_count DESC";

    try (java.sql.Connection conn = DBUtil.getConnection();
         java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
         java.sql.ResultSet rs = pstmt.executeQuery()) {
        
        while (rs.next()) {
            String subject = rs.getString("subjects"); // 對應到 b.subjects
            int count = rs.getInt("borrow_count");
            stats.put(subject != null ? subject : "未分類", count);
        }
    } catch (java.sql.SQLException e) {
        System.err.println("SQL 錯誤: " + e.getMessage());
        e.printStackTrace();
    }
    return stats;
    }
    
    public Map<String, Integer> getSubjectStatsFromAllRecords() {
    Map<String, Integer> stats = new HashMap<>();
    // 直接用 JOIN，SQL 會幫你把「借閱紀錄」和「書籍主題」直接對應起來算好
    String sql = "SELECT b.subjects, COUNT(*) as count " +
                 "FROM borrow_records r " +
                 "JOIN books b ON r.book_id = b.book_id " +
                 "GROUP BY b.subjects";
                 
    // ... 執行 SQL 並把結果填入 stats Map ...
    return stats;
}
}