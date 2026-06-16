package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.BorrowRecord;


public class BookDAO {

    /**
     * 管理者：新增書籍
     */
    public boolean addBook(Book book) {
        String sql = "INSERT INTO Books (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthors());
            pstmt.setString(3, book.getSubjects());
            pstmt.setString(4, book.getPublisher());
            pstmt.setString(5, book.getPublishYear());
            pstmt.setString(6, book.getEdition());
            pstmt.setString(7, book.getFormatDesc());
            pstmt.setString(8, book.getSource());
            pstmt.setString(9, book.getNote());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 管理者：徹底下架 (刪除) 書籍
     */
    public boolean deleteBook(int bookId) {
        String sql = "DELETE FROM Books WHERE book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
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
    // 💡 核心借書邏輯：檢查上限、逾期，並使用 Transaction (資料庫交易) 確保安全
    public String borrowBook(String studentNo, int bookId) {
        try (java.sql.Connection conn = DBUtil.getConnection()) {
            // 🛡️ 開啟「資料庫交易機制」，確保所有動作同進同退
            conn.setAutoCommit(false); 
            
            try {
                // 1️⃣ 第一關：抓取使用者 ID 與身分天數/上限
                String userSql = "SELECT user_id, role_level FROM Users WHERE student_no = ?";
                int userId = -1;
                int borrowLimit = 3; // 一般會員預設最多借 3 本
                int borrowDays = 7;  // 一般會員預設借 7 天

                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(userSql)) {
                    pstmt.setString(1, studentNo);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        userId = rs.getInt("user_id");
                        String role = rs.getString("role_level");
                        if (role != null && role.toUpperCase().contains("VIP")) {
                            borrowLimit = 5; // VIP 可借 5 本
                            borrowDays = 14; // VIP 借 14 天
                        }
                    } else {
                        return "找不到該使用者帳號！";
                    }
                }

                // 2️⃣ 第二關：檢查是否有「逾期未還」的書
                String overdueSql = "SELECT COUNT(*) AS overdue_count FROM Borrow_records WHERE user_id = ? AND return_date IS NULL AND due_date < NOW()";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(overdueSql)) {
                    pstmt.setInt(1, userId);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next() && rs.getInt("overdue_count") > 0) {
                        return "您目前有逾期未還的書籍，必須先歸還才能再借喔！";
                    }
                }

                // 3️⃣ 第三關：檢查借閱數量是否「已達上限」
                String countSql = "SELECT COUNT(*) AS current_borrows FROM Borrow_records WHERE user_id = ? AND return_date IS NULL";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                    pstmt.setInt(1, userId);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next() && rs.getInt("current_borrows") >= borrowLimit) {
                        return "您的借閱數量已達上限 (" + borrowLimit + "本) 囉！";
                    }
                }

                // 4️⃣ 第四關：檢查書籍狀態是否真的是「可借」
                String bookSql = "SELECT status FROM Books WHERE book_id = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                    pstmt.setInt(1, bookId);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        // 取得資料庫真實狀態，轉大寫並去除空白方便比對
                        String status = rs.getString("status");
                        String checkStatus = (status != null) ? status.trim().toUpperCase() : "";
                        
                        // 🛡️ 終極寬鬆防呆：只要包含這些關鍵字，通通准許借！
                        boolean canBorrow = checkStatus.contains("可借") || 
                                            checkStatus.contains("在館") || 
                                            checkStatus.contains("在庫") || 
                                            checkStatus.contains("AVAIL");
                                            
                        if (!canBorrow) {
                            return "這本書目前無法借閱 (狀態: " + status + ")！";
                        }
                    } else {
                        return "館藏中找不到這本書！";
                    }
                }
                
                // 📝 最終執行 A：將書籍狀態改為英文的「BORROWED」
                // 🚨 修正：絕對不能寫中文的 '借出'，要配合資料庫的格式寫 'BORROWED'
                String updateBookSql = "UPDATE Books SET status = 'BORROWED' WHERE book_id = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                }


                // 📝 最終執行 B：寫入借閱紀錄
                String insertRecordSql = "INSERT INTO Borrow_records (user_id, book_id, borrow_date, due_date, borrow_days) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), ?)";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(insertRecordSql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, bookId);
                    pstmt.setInt(3, borrowDays);
                    pstmt.setInt(4, borrowDays);
                    pstmt.executeUpdate();
                }

                // 🎉 全部成功！提交資料庫交易
                conn.commit(); 
                return "成功：" + borrowDays; // 回傳成功與天數

            } catch (Exception e) {
                conn.rollback(); // 發生任何錯誤就倒轉
                return "系統錯誤：" + e.getMessage();
            } finally {
                conn.setAutoCommit(true); // 恢復預設
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "資料庫連線異常：" + e.getMessage();
        }
    }
    // =========================================================
    // 🌟 舊版：給桌面端 SearchFrame 用的 (保留相容性，回傳 boolean)
    // =========================================================
    public boolean borrowBook(int userId, int bookId, int days) {
        String updateBook = "UPDATE Books SET status = 'BORROWED' WHERE book_id = ?";
        String insertRecord = "INSERT INTO Borrow_records (user_id, book_id, due_date) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY))";
        
        java.sql.Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // 開啟事務

            // 1. 更新書狀態
            java.sql.PreparedStatement ps1 = conn.prepareStatement(updateBook);
            ps1.setInt(1, bookId);
            ps1.executeUpdate();

            // 2. 新增紀錄
            java.sql.PreparedStatement ps2 = conn.prepareStatement(insertRecord);
            ps2.setInt(1, userId);
            ps2.setInt(2, bookId);
            ps2.setInt(3, days);
            ps2.executeUpdate();

            conn.commit(); // 全部成功才存檔
            return true;
        } catch (java.sql.SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
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
                     "ORDER BY r.return_date IS NULL DESC, r.return_date DESC, r.borrow_date DESC"; // 最新還的書排在最上面

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
     * 獲取單一書籍的歷史借閱紀錄
     * 回傳格式：List<Object[]> -> {借閱人姓名, 借出時間, 歸還時間}
     */
    public List<Object[]> getBookBorrowHistory(int bookId) {
        List<Object[]> history = new java.util.ArrayList<>();
        
        // 💡 抓蟲關鍵：確保使用有底線的 borrow_records，並 JOIN users 表抓取學生姓名
        // 💡 抓蟲關鍵：確保使用有底線的 borrow_records，並 JOIN users 表抓取學生姓名
        String sql = "SELECT u.name, r.borrow_date, r.return_date " +
                     "FROM borrow_records r " +
                     "JOIN users u ON r.user_id = u.user_id " +
                     "WHERE r.book_id = ? " +
                     // ✨ 真正的魔法：未歸還置頂，剩下的依照「歸還日期」由新到舊往下排
                     "ORDER BY return_date IS NULL DESC, return_date DESC";

        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String studentName = rs.getString("name");
                    String borrowDate = rs.getString("borrow_date");
                    String returnDate = rs.getString("return_date");
                    
                    history.add(new Object[]{studentName, borrowDate, returnDate});
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("讀取書籍歷史紀錄失敗: " + e.getMessage());
            e.printStackTrace();
        }
        return history;
    }
    /**
     * 檢查該使用者是否有即將到期（指定天數內）或【已經逾期】的書籍
     */
    public List<String> getExpiringBooks(int userId, int thresholdDays) {
        List<String> list = new ArrayList<>();
        
        // 💡 關鍵修正：移除了 days_left >= 0 的限制，讓負數（逾期）也能被撈出來！
        // 並且加上 ORDER BY days_left ASC，讓逾期最嚴重的書排在最上面
        String sql = "SELECT b.title, DATEDIFF(r.due_date, NOW()) as days_left " +
                     "FROM Borrow_records r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? AND r.return_date IS NULL " +
                     "HAVING days_left <= ? " +
                     "ORDER BY days_left ASC"; 

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, thresholdDays); // 例如傳入 3，代表只提醒 3 天內到期或已逾期的書
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int daysLeft = rs.getInt("days_left");
                String title = rs.getString("title");
                
                // ✨ 智慧狀態分類：根據剩餘天數給予不同的強烈警告
                if (daysLeft < 0) {
                    // 取絕對值 Math.abs 把 -5 變成 5 天
                    list.add("❌ 《" + title + "》 已逾期 " + Math.abs(daysLeft) + " 天！請盡速歸還！");
                } else if (daysLeft == 0) {
                    list.add("🚨 《" + title + "》 就是今天到期！");
                } else {
                    list.add("⚠️ 《" + title + "》 剩餘 " + daysLeft + " 天到期");
                }
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
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
        // ✨ 修改這裡：將 u.real_name 改為 u.name
        String sqlCheckReserve = "SELECT u.name, r.user_id FROM Reservations r " +
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

                // 2. 自動結案自己的預約
                psSelf.setInt(1, bookId);
                psSelf.setInt(2, currentUserId);
                psSelf.executeUpdate();
                
                // 3. 檢查是否有「別人」在預約
                String notifyName = null;
                ps3.setInt(1, bookId);
                ps3.setInt(2, currentUserId); 
                ResultSet rs = ps3.executeQuery();
                
                if (rs.next()) {
                    // ✨ 修改這裡：從 ResultSet 撈取 name
                    notifyName = rs.getString("name"); 
                    int targetUserId = rs.getInt("user_id");
                    
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
        
        // ✨ 確保這裡的 SELECT 欄位名稱與你 MySQL 裡的 Books 表完全一致
        // 請確認你的 Books 表是否有 isbn 欄位，或者是不是存在 note 裡？
        // 這裡假設你有 isbn 欄位，如果沒有，請把 rs.getString("isbn") 改成 rs.getString("note")
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
                b.setTitle(rs.getString("title"));
                b.setAuthors(rs.getString("authors"));
                b.setSubjects(rs.getString("subjects"));
                b.setPublisher(rs.getString("publisher"));
                b.setPublishYear(rs.getString("publish_year")); // 確認欄位名
                
                // ✨ 重要：如果 SQL 報錯說找不到欄位，請檢查這裡的字串是否與 Workbench 的欄位完全一樣
                // 例如：檢查是 rs.getString("status") 還是 rs.getString("book_status")
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    b.setStatus(Book.Status.valueOf(statusStr.toUpperCase()));
                }
                
                list.add(b);
            }
        } catch (SQLException e) { 
            System.err.println("取得收藏清單失敗: " + e.getMessage());
            e.printStackTrace(); 
        }
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

    /**
     * ==========================================
     * ✨ 管理者專用功能：獲取全校所有借還紀錄 (支援關鍵字搜尋)
     * ==========================================
     * @param keyword 搜尋關鍵字 (學號、姓名或書名)，若為空白則搜尋全部
     */
    public List<Object[]> getAllSystemBorrowRecords(String keyword) {
        List<Object[]> list = new ArrayList<>();
        // SQL 說明：將 borrow_records, users, books 三張表 JOIN 起來，這樣才能同時顯示學號、姓名跟書名！
        String sql = "SELECT u.student_no, u.name, b.title, r.borrow_date, r.due_date, r.return_date " +
                     "FROM borrow_records r " +
                     "JOIN users u ON r.user_id = u.user_id " +
                     "JOIN books b ON r.book_id = b.book_id ";
        
        // 如果有輸入關鍵字，就加上條件過濾
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += "WHERE u.student_no LIKE ? OR u.name LIKE ? OR b.title LIKE ? ";
        }
        
        // ✨ 這裡套用我們的「魔法排序」！未歸還的強制置頂，其餘照借出時間排
        sql += "ORDER BY r.return_date IS NULL DESC, r.borrow_date DESC";

        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 設定搜尋關鍵字 (前後加上 % 代表模糊搜尋)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchStr = "%" + keyword.trim() + "%";
                pstmt.setString(1, searchStr);
                pstmt.setString(2, searchStr);
                pstmt.setString(3, searchStr);
            }

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // 💡 將 Timestamp 改用 getString 取出，確保傳遞給 UI 時絕對安全且不會報錯
                    list.add(new Object[]{
                        rs.getString("student_no"),
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date"),
                        rs.getString("return_date")
                    });
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("查詢全校借閱紀錄失敗：" + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
    /**
     * ✨ 新增功能：獲取特定使用者的「排隊中」與「已到館」預約清單
     */
    public List<Object[]> getUserReservations(int userId) {
        List<Object[]> list = new ArrayList<>();
        // SQL 說明：將 Reservations 與 Books 表格 JOIN 起來，撈出書名、預約時間與狀態
        String sql = "SELECT b.title, r.reserve_date, r.status " +
                     "FROM Reservations r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? AND r.status IN ('WAITING', 'NOTIFIED') " +
                     "ORDER BY r.reserve_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            while (rs.next()) {
                String statusStr = rs.getString("status");
                // 轉成親切的中文狀態
                String displayStatus = statusStr.equalsIgnoreCase("NOTIFIED") ? "🟢 書籍已到館 (請盡速借閱)" : "🟡 排隊等待中";
                
                // 處理預約時間
                String dateStr = "---";
                if (rs.getTimestamp("reserve_date") != null) {
                    dateStr = rs.getTimestamp("reserve_date").toLocalDateTime().format(formatter);
                }

                list.add(new Object[]{
                    rs.getString("title"),
                    dateStr,
                    displayStatus
                });
            }
        } catch (SQLException e) {
            System.err.println("獲取個人預約紀錄失敗：" + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
    // 💡 放在你的 DAO 檔案裡
    public boolean deleteAllNotifications(int userId) {
        // 假設你的通知資料表叫做 notifications
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