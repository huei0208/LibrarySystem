package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.BorrowRecord;


public class BookDAO {

    /**
     * 從資料庫抓取所有書籍
     */
    public List<Book> getAllBooks() {
        return searchBooks("全部", "");
    }

    /**
     * 關鍵字搜尋書籍
     * @param category 搜尋類別（書名、作者、主題、出版社、ISBN）
     * @param keyword 關鍵字
     */
    public List<Book> searchBooks(String category, String keyword) {
        List<Book> list = new ArrayList<>();
        
        // 1. 決定要對應資料庫哪個欄位
        String column = switch (category) {
            case "作者" -> "authors";
            case "主題" -> "subjects";
            case "出版社" -> "publisher";
            case "ISBN (識別號)" -> "note"; // 我們的 ISBN 存在 note 欄位裡
            default -> "title";
        };

        // 2. 撰寫 SQL (使用 LIKE 進行模糊查詢)
        String sql = "SELECT * FROM Books WHERE " + column + " LIKE ?";
        if (category.equals("全部")) {
            sql = "SELECT * FROM Books";
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (!category.equals("全部")) {
                pstmt.setString(1, "%" + keyword + "%");
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                // 使用我們剛才在 Book.java 建立的新構造函數
                Book b = new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("authors"),
                    rs.getString("subjects"),
                    rs.getString("publisher"),
                    rs.getString("publish_year"),
                    rs.getString("edition"),
                    rs.getString("format_desc"),
                    rs.getString("source"),
                    rs.getString("note"),
                    rs.getString("status")
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 執行借書動作 (更新資料庫狀態)
     */
    public boolean updateBookStatus(int bookId, String newStatus) {
        String sql = "UPDATE Books SET status = ? WHERE book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, bookId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean borrowBook(int userId, int bookId, int days) {
        String updateBook = "UPDATE Books SET status = 'BORROWED' WHERE book_id = ?";
        String insertRecord = "INSERT INTO Borrow_records (user_id, book_id, due_date) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY))";
        
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // 開啟事務

            // 1. 更新書狀態
            PreparedStatement ps1 = conn.prepareStatement(updateBook);
            ps1.setInt(1, bookId);
            ps1.executeUpdate();

            // 2. 新增紀錄
            PreparedStatement ps2 = conn.prepareStatement(insertRecord);
            ps2.setInt(1, userId);
            ps2.setInt(2, bookId);
            ps2.setInt(3, days);
            ps2.executeUpdate();

            conn.commit(); // 全部成功才存檔
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 獲取特定使用者的「借閱中」清單 (未歸還)
     */
    public List<Object[]> getBorrowedBooks(int userId) {
        List<Object[]> list = new ArrayList<>();
        // SQL 說明：連結 Books 與 Borrow_records 表，找出 return_date 為空的紀錄
        String sql = "SELECT b.book_id, b.title, r.borrow_date, r.due_date, r.record_id " +
                     "FROM Borrow_records r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? AND r.return_date IS NULL";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getTimestamp("borrow_date"),
                    rs.getTimestamp("due_date"),
                    rs.getInt("record_id")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 獲取使用者的完整借閱歷史紀錄 (包含已歸還與未歸還)
     */
    public List<Object[]> getBorrowHistory(int userId) {
        List<Object[]> history = new ArrayList<>();
        // SQL 說明：抓取書名、借出時間、歸還時間、應還時間
        String sql = "SELECT b.title, r.borrow_date, r.return_date, r.due_date " +
                     "FROM Borrow_records r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? " +
                     "ORDER BY r.borrow_date DESC"; // 最近的紀錄排在最上面

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                history.add(new Object[]{
                    rs.getString("title"),
                    rs.getTimestamp("borrow_date"),
                    rs.getTimestamp("return_date"),
                    rs.getTimestamp("due_date")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
    /**
     * 獲取單一書籍的借閱歷史
     */
    public List<Object[]> getBookBorrowHistory(int bookId) {
        List<Object[]> history = new ArrayList<>();
        // SQL 說明：連結 Users 取得借閱人姓名，並按時間排序
        String sql = "SELECT u.real_name, r.borrow_date, r.return_date " +
                     "FROM Borrow_records r " +
                     "JOIN Users u ON r.user_id = u.user_id " +
                     "WHERE r.book_id = ? " +
                     "ORDER BY r.borrow_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                history.add(new Object[]{
                    rs.getString("real_name"),
                    rs.getTimestamp("borrow_date"),
                    rs.getTimestamp("return_date")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }
    /**
     * 檢查該使用者是否有即將到期（3天內）的書籍
     */
    public List<String> getExpiringBooks(int userId, int thresholdDays) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT b.title, DATEDIFF(r.due_date, NOW()) as days_left " +
                     "FROM Borrow_records r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? AND r.return_date IS NULL " +
                     "HAVING days_left <= ? AND days_left >= 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, thresholdDays);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add("《" + rs.getString("title") + "》 剩餘 " + rs.getInt("days_left") + " 天到期");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 預約書籍（增加自我預約檢查）
     */
    public String reserveBook(int userId, int bookId) {
        // SQL 1: 檢查這本書目前是誰借的
        String sqlCheckBorrower = "SELECT user_id FROM Borrow_records WHERE book_id = ? AND return_date IS NULL";
        // SQL 2: 插入預約紀錄
        String sqlInsertReserve = "INSERT INTO Reservations (user_id, book_id) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection()) {
            // 先檢查目前借閱者
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckBorrower)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int currentBorrowerId = rs.getInt("user_id");
                    if (currentBorrowerId == userId) {
                        return "SELF_BORROWED"; // 如果是自己借的，回傳特殊字串
                    }
                }
            }

            // 檢查通過，執行預約
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertReserve)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, bookId);
                if (pstmt.executeUpdate() > 0) {
                    return "SUCCESS";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "FAILED";
    }
    /**
     * 檢查是否有預約的書已經歸還（等著該使用者去借）
     */
    public List<String> getReadyReservations(int userId) {
        List<String> readyBooks = new ArrayList<>();
        // 加上 DISTINCT 關鍵字
        String sql = "SELECT DISTINCT b.title FROM Reservations r " + 
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? AND r.status = 'NOTIFIED'";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                readyBooks.add(rs.getString("title"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return readyBooks;
    }

    /**
     * 當使用者看完通知後，將狀態改為 'CLOSED' 或刪除，避免每次登入都跳出來
     */
    public void clearReservationNotifications(int userId) {
        String sql = "UPDATE Reservations SET status = 'CLOSED' WHERE user_id = ? AND status = 'NOTIFIED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /**
     * 還書、回傳預約者的資訊（如果有人的話）
     */
    public String returnBookAndNotify(int recordId, int bookId, int currentUserId) {
    String sqlUpdateRecord = "UPDATE Borrow_records SET return_date = NOW() WHERE record_id = ?";
    String sqlUpdateBook = "UPDATE Books SET status = 'AVAILABLE' WHERE book_id = ?";
    
    // 關鍵修正 1：先把自己（還書者）對這本書的「等待中」預約直接結案 (CLOSED)
    String sqlCloseSelfReserve = "UPDATE Reservations SET status = 'CLOSED' " +
                                 "WHERE book_id = ? AND user_id = ? AND status = 'WAITING'";
    
    // 關鍵修正 2：找下一個預約者時，要排除掉自己 (AND user_id != ?)
    String sqlCheckReserve = "SELECT u.real_name, r.user_id FROM Reservations r " +
                             "JOIN Users u ON r.user_id = u.user_id " +
                             "WHERE r.book_id = ? AND r.status = 'WAITING' AND r.user_id != ? " +
                             "ORDER BY r.reserve_date LIMIT 1";

    Connection conn = null;
    try {
        conn = DBUtil.getConnection();
        conn.setAutoCommit(false);
        
        try (PreparedStatement ps1 = conn.prepareStatement(sqlUpdateRecord);
             PreparedStatement ps2 = conn.prepareStatement(sqlUpdateBook);
             PreparedStatement psSelf = conn.prepareStatement(sqlCloseSelfReserve);
             PreparedStatement ps3 = conn.prepareStatement(sqlCheckReserve)) {
            
            // 1. 執行還書
            ps1.setInt(1, recordId);
            ps1.executeUpdate();
            
            ps2.setInt(1, bookId);
            ps2.executeUpdate();

            // 2. 自動結案自己的預約（防止自己通知自己）
            psSelf.setInt(1, bookId);
            psSelf.setInt(2, currentUserId);
            psSelf.executeUpdate();
            
            // 3. 檢查是否有「別人」在預約
            String notifyName = null;
            ps3.setInt(1, bookId);
            ps3.setInt(2, currentUserId); // 排除自己
            ResultSet rs = ps3.executeQuery();
            
            if (rs.next()) {
                notifyName = rs.getString("real_name");
                int targetUserId = rs.getInt("user_id");
                
                // 將該預約者的狀態改為已通知
                String sqlUpdateRes = "UPDATE Reservations SET status = 'NOTIFIED' " +
                                     "WHERE book_id = ? AND user_id = ? AND status = 'WAITING'";
                try (PreparedStatement ps4 = conn.prepareStatement(sqlUpdateRes)) {
                    ps4.setInt(1, bookId);
                    ps4.setInt(2, targetUserId);
                    ps4.executeUpdate();
                }
            }
            
            conn.commit();
            return notifyName;
        } catch (SQLException ex) {
            if (conn != null) conn.rollback();
            throw ex;
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return "ERROR";
    }
}
    /**
     * 加入收藏
     */
    public boolean addFavorite(int userId, int bookId) {
        String sql = "INSERT IGNORE INTO Favorites (user_id, book_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    /**
     * 取得使用者的收藏書籍清單
     */
    public List<Book> getFavoriteBooks(int userId) {
        List<Book> list = new ArrayList<>();
        // 確保抓取所有欄位，這樣 refreshTable 才有資料填
        String sql = "SELECT b.* FROM Books b " +
                     "JOIN Favorites f ON b.book_id = f.book_id " +
                     "WHERE f.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Book b = new Book();
                b.setBookId(rs.getInt("book_id"));
                b.setFirstIsbn(rs.getString("isbn")); // 檢查你的 SQL 欄位名是否為 isbn
                b.setTitle(rs.getString("title"));
                b.setAuthors(rs.getString("authors"));
                b.setSubjects(rs.getString("subjects"));
                b.setPublisher(rs.getString("publisher"));
                // 處理 Enum 轉換
                b.setStatus(Book.Status.valueOf(rs.getString("status").toUpperCase()));
                list.add(b);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    /**
     * 移除收藏紀錄
     */
    public boolean deleteFavorite(int userId, int bookId) {
        String sql = "DELETE FROM Favorites WHERE user_id = ? AND book_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            
            // 如果影響的行數 > 0，代表刪除成功
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 取得使用者目前尚未歸還的書籍數量
     */
    public int getCurrentBorrowCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * 取得使用者目前的借閱清單（含逾期計算）
     */
    public List<BorrowRecord> getCurrentBorrowingWithFine(int userId) {
        List<BorrowRecord> list = new ArrayList<>();
        
        // 1. SQL 定義 (DATEDIFF 計算逾期天數)
        String sql = "SELECT br.*, b.title, DATEDIFF(NOW(), br.due_date) as overdue_days " +
                     "FROM Borrow_records br " +
                     "JOIN Books b ON br.book_id = b.book_id " +
                     "WHERE br.user_id = ? AND br.return_date IS NULL";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BorrowRecord r = new BorrowRecord(); 
                    r.setRecordId(rs.getInt("record_id"));
                    r.setTitle(rs.getString("title"));
                    
                    // 處理借閱日期 (確保抓取正確時區)
                    if (rs.getTimestamp("borrow_date") != null) {
                        r.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    }
                    
                    // 處理應還日期
                    if (rs.getTimestamp("due_date") != null) {
                        r.setDueDate(rs.getTimestamp("due_date").toLocalDateTime());
                    }
                    
                    // 處理逾期天數
                    r.setOverdueDays(rs.getInt("overdue_days"));
                    
                    list.add(r);
                }
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return list;
    }
    public boolean addBook(Book book) {
    String sql = "INSERT INTO Books (title, authors, subjects, publisher, status) VALUES (?, ?, ?, ?, 'AVAILABLE')";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, book.getTitle());
        pstmt.setString(2, book.getAuthors());
        pstmt.setString(3, book.getSubjects());
        pstmt.setString(4, book.getPublisher());
        return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
}