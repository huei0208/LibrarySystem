package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public List<Book> getAllBooks() {
        return searchBooks("全部", "");
    }

    public List<Book> searchBooks(String category, String keyword) {
        List<Book> list = new ArrayList<>();

        String column = switch (category) {
            case "作者" -> "authors";
            case "主題" -> "subjects";
            case "出版社" -> "publisher";
            case "ISBN (識別號)" -> "note";
            default -> "title";
        };

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
        String updateBook = "UPDATE Books SET status = 'BORROWED' WHERE book_id = ? AND status = 'AVAILABLE'";
        String insertRecord = "INSERT INTO Borrow_records (user_id, book_id, borrow_date, due_date, borrow_days) " +
                              "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), ?)";

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(updateBook)) {
                ps1.setInt(1, bookId);

                int updatedRows = ps1.executeUpdate();
                if (updatedRows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement(insertRecord)) {
                ps2.setInt(1, userId);
                ps2.setInt(2, bookId);
                ps2.setInt(3, days);
                ps2.setInt(4, days);
                ps2.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> getBorrowedBooks(int userId) {
        List<Object[]> list = new ArrayList<>();

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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Object[]> getBorrowHistory(int userId) {
        List<Object[]> history = new ArrayList<>();

        String sql = "SELECT b.title, r.borrow_date, r.return_date, r.due_date " +
                     "FROM Borrow_records r " +
                     "JOIN Books b ON r.book_id = b.book_id " +
                     "WHERE r.user_id = ? " +
                     "ORDER BY r.borrow_date DESC";

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

    public List<Object[]> getBookBorrowHistory(int bookId) {
        List<Object[]> history = new ArrayList<>();

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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return history;
    }

    public List<String> getExpiringBooks(int userId, int thresholdDays) {
        List<String> list = new ArrayList<>();

        String sql = "SELECT b.title, DATEDIFF(r.due_date, NOW()) AS days_left " +
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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public String reserveBook(int userId, int bookId) {
        String sqlCheckBorrower = "SELECT user_id FROM Borrow_records WHERE book_id = ? AND return_date IS NULL";
        String sqlCheckDuplicate = "SELECT reservation_id FROM Reservations WHERE user_id = ? AND book_id = ? AND status = 'WAITING'";
        String sqlInsertReserve = "INSERT INTO Reservations (user_id, book_id) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection()) {

            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckBorrower)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int currentBorrowerId = rs.getInt("user_id");
                    if (currentBorrowerId == userId) {
                        return "SELF_BORROWED";
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckDuplicate)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, bookId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return "ALREADY_RESERVED";
                }
            }

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

    public List<String> getReadyReservations(int userId) {
        List<String> readyBooks = new ArrayList<>();

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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return readyBooks;
    }

    public void clearReservationNotifications(int userId) {
        String sql = "UPDATE Reservations SET status = 'CLOSED' WHERE user_id = ? AND status = 'NOTIFIED'";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String returnBookAndNotify(int recordId, int bookId, int currentUserId) {
        String sqlUpdateRecord = "UPDATE Borrow_records SET return_date = NOW() WHERE record_id = ?";
        String sqlUpdateBook = "UPDATE Books SET status = 'AVAILABLE' WHERE book_id = ?";

        String sqlCloseSelfReserve = "UPDATE Reservations SET status = 'CLOSED' " +
                                     "WHERE book_id = ? AND user_id = ? AND status = 'WAITING'";

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

                ps1.setInt(1, recordId);
                ps1.executeUpdate();

                ps2.setInt(1, bookId);
                ps2.executeUpdate();

                psSelf.setInt(1, bookId);
                psSelf.setInt(2, currentUserId);
                psSelf.executeUpdate();

                String notifyName = null;

                ps3.setInt(1, bookId);
                ps3.setInt(2, currentUserId);
                ResultSet rs = ps3.executeQuery();

                if (rs.next()) {
                    notifyName = rs.getString("real_name");
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
                if (conn != null) {
                    conn.rollback();
                }
                throw ex;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public boolean addFavorite(int userId, int bookId) {
        String sql = "INSERT IGNORE INTO Favorites (user_id, book_id) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Book> getFavoriteBooks(int userId) {
        List<Book> list = new ArrayList<>();

        String sql = "SELECT b.* FROM Books b " +
                     "JOIN Favorites f ON b.book_id = f.book_id " +
                     "WHERE f.user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
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

    public boolean deleteFavorite(int userId, int bookId) {
        String sql = "DELETE FROM Favorites WHERE user_id = ? AND book_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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

    public List<BorrowRecord> getCurrentBorrowingWithFine(int userId) {
        List<BorrowRecord> list = new ArrayList<>();

        String sql = "SELECT br.*, b.title, DATEDIFF(NOW(), br.due_date) AS overdue_days " +
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

                    if (rs.getTimestamp("borrow_date") != null) {
                        r.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    }

                    if (rs.getTimestamp("due_date") != null) {
                        r.setDueDate(rs.getTimestamp("due_date").toLocalDateTime());
                    }

                    r.setOverdueDays(rs.getInt("overdue_days"));

                    list.add(r);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}