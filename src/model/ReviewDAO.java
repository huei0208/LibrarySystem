package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    /**
     * 新增一筆書評
     */
    public boolean addReview(int bookId, int userId, int rating, String comment) {
        String sql = "INSERT INTO book_reviews (book_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, rating);
            pstmt.setString(4, comment);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("新增書評失敗: " + e.getMessage());
            return false;
        }
    }

    /**
     * 讀取某本書的所有書評
     * 回傳格式：{留言者姓名, 星星數, 評論內容, 留言時間}
     */
    public List<Object[]> getReviewsByBook(int bookId) {
        List<Object[]> list = new ArrayList<>();
        // 關聯 users 表，這樣才能顯示是「誰」留的言
        String sql = "SELECT u.name, r.rating, r.comment, r.created_at " +
                     "FROM book_reviews r " +
                     "JOIN users u ON r.user_id = u.user_id " +
                     "WHERE r.book_id = ? " +
                     "ORDER BY r.created_at DESC";
                     
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("created_at");
                    if (date != null && date.length() >= 16) {
                        date = date.substring(0, 16); // 截斷秒數讓畫面乾淨點
                    }
                    
                    list.add(new Object[]{
                        rs.getString("name"),
                        "⭐ ".repeat(rs.getInt("rating")), // 把數字轉換成星星符號
                        rs.getString("comment"),
                        date
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("讀取書評失敗: " + e.getMessage());
        }
        return list;
    }
    /**
     * [管理員專用] 獲取全校所有書評，支援關鍵字搜尋(書名或學生姓名)
     * 回傳格式：{評論ID, 書名, 留言者姓名, 星星數, 評論內容, 留言時間}
     */
    public List<Object[]> getAllSystemReviews(String keyword) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT r.review_id, b.title, u.name, r.rating, r.comment, r.created_at " +
                     "FROM book_reviews r " +
                     "JOIN books b ON r.book_id = b.book_id " +
                     "JOIN users u ON r.user_id = u.user_id " +
                     "WHERE b.title LIKE ? OR u.name LIKE ? " +
                     "ORDER BY r.created_at DESC";
                     
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("created_at");
                    if (date != null && date.length() >= 16) date = date.substring(0, 16);
                    
                    list.add(new Object[]{
                        rs.getInt("review_id"),
                        rs.getString("title"),
                        rs.getString("name"),
                        "⭐ ".repeat(rs.getInt("rating")),
                        rs.getString("comment"),
                        date
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("讀取全校書評失敗: " + e.getMessage());
        }
        return list;
    }

    /**
     * [管理員專用] 刪除不當書評
     */
    public boolean deleteReview(int reviewId) {
        String sql = "DELETE FROM book_reviews WHERE review_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("刪除書評失敗: " + e.getMessage());
            return false;
        }
    }
}