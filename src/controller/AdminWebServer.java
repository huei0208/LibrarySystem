package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.DBUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AdminWebServer {

    private static final int PORT = 8081;

    /*
     * 如果你的資料表名稱是小寫，就改成：
     * books
     * users
     * borrow_records
     * book_reviews
     */
    private static final String TABLE_BOOKS = "Books";
    private static final String TABLE_USERS = "Users";
    private static final String TABLE_RECORDS = "Borrow_records";
    private static final String TABLE_REVIEWS = "book_reviews";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/admin/dashboard", new DashboardHandler());
        server.createContext("/api/admin/books", new BooksHandler());
        server.createContext("/api/admin/users", new UsersHandler());
        server.createContext("/api/admin/records", new RecordsHandler());
        server.createContext("/api/admin/reviews", new ReviewsHandler());
        server.createContext("/api/admin/stats", new StatsHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("管理者後台 API 已啟動：http://localhost:" + PORT);
        System.out.println("測試網址：http://localhost:" + PORT + "/api/admin/dashboard");
    }

    static abstract class BaseHandler implements HttpHandler {
        @Override
        public final void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                doHandle(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500,
                        "{\"success\":false,\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        }

        protected abstract void doHandle(HttpExchange exchange) throws Exception;
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String jsonEscape(Object value) {
        if (value == null) return "";

        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Map<String, String> getQueryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getRawQuery();

        if (query == null || query.isBlank()) {
            return map;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            map.put(key, value);
        }

        return map;
    }

    private static Map<String, String> getFormParams(HttpExchange exchange) throws IOException {
        String body;

        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Map<String, String> map = new HashMap<>();

        if (body == null || body.isBlank()) {
            return map;
        }

        String[] pairs = body.split("&");

        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            map.put(key, value);
        }

        return map;
    }

    // =====================================================
    // 首頁總覽
    // =====================================================
    static class DashboardHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            String json = "{"
                    + "\"bookCount\":" + count("SELECT COUNT(*) FROM " + TABLE_BOOKS) + ","
                    + "\"userCount\":" + count("SELECT COUNT(*) FROM " + TABLE_USERS) + ","
                    + "\"borrowingCount\":" + count("SELECT COUNT(*) FROM " + TABLE_RECORDS + " WHERE return_date IS NULL") + ","
                    + "\"overdueCount\":" + count("SELECT COUNT(*) FROM " + TABLE_RECORDS + " WHERE return_date IS NULL AND due_date < NOW()") + ","
                    + "\"reviewCount\":" + count("SELECT COUNT(*) FROM " + TABLE_REVIEWS)
                    + "}";

            sendJson(exchange, 200, json);
        }

        private int count(String sql) {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }

            } catch (SQLException e) {
                System.err.println("統計失敗：" + e.getMessage());
            }

            return 0;
        }
    }

    // =====================================================
    // 書籍管理
    // GET    取得書籍
    // POST   新增書籍
    // DELETE 下架書籍
    // =====================================================
    static class BooksHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                getBooks(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                addBook(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                deleteBook(exchange);
            } else {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"不支援的方法\"}");
            }
        }

        private void getBooks(HttpExchange exchange) throws Exception {
            Map<String, String> params = getQueryParams(exchange.getRequestURI());
            String keyword = params.getOrDefault("keyword", "").trim();

            String sql = "SELECT book_id, title, authors, subjects, publisher, publish_year, status "
                    + "FROM " + TABLE_BOOKS + " ";

            boolean hasKeyword = !keyword.isEmpty();

            if (hasKeyword) {
                sql += "WHERE title LIKE ? OR authors LIKE ? OR subjects LIKE ? OR publisher LIKE ? ";
            }

            sql += "ORDER BY book_id DESC";

            StringBuilder json = new StringBuilder("[");

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (hasKeyword) {
                    String search = "%" + keyword + "%";
                    ps.setString(1, search);
                    ps.setString(2, search);
                    ps.setString(3, search);
                    ps.setString(4, search);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;

                    while (rs.next()) {
                        if (!first) json.append(",");
                        first = false;

                        json.append("{")
                                .append("\"book_id\":").append(rs.getInt("book_id")).append(",")
                                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                                .append("\"authors\":\"").append(jsonEscape(rs.getString("authors"))).append("\",")
                                .append("\"subjects\":\"").append(jsonEscape(rs.getString("subjects"))).append("\",")
                                .append("\"publisher\":\"").append(jsonEscape(rs.getString("publisher"))).append("\",")
                                .append("\"publish_year\":\"").append(jsonEscape(rs.getString("publish_year"))).append("\",")
                                .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\"")
                                .append("}");
                    }
                }
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        }

        private void addBook(HttpExchange exchange) throws Exception {
            Map<String, String> params = getFormParams(exchange);

            String title = params.getOrDefault("title", "").trim();
            String authors = params.getOrDefault("authors", "").trim();
            String subjects = params.getOrDefault("subjects", "").trim();
            String publisher = params.getOrDefault("publisher", "").trim();
            String publishYear = params.getOrDefault("publish_year", "").trim();

            if (title.isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"書名不可空白\"}");
                return;
            }

            /*
             * 這裡不要塞 status。
             * 因為你原本 BookDAO.addBook() 也是只新增下面這 9 個欄位。
             */
            String sql = "INSERT INTO " + TABLE_BOOKS
                    + " (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            boolean success;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, title);
                ps.setString(2, authors);
                ps.setString(3, subjects);
                ps.setString(4, publisher);
                ps.setString(5, publishYear);
                ps.setString(6, "");
                ps.setString(7, "");
                ps.setString(8, "");
                ps.setString(9, "");

                success = ps.executeUpdate() > 0;
            }

            sendJson(exchange, 200, "{\"success\":" + success + "}");
        }

        private void deleteBook(HttpExchange exchange) throws Exception {
            Map<String, String> params = getQueryParams(exchange.getRequestURI());
            int bookId = parseInt(params.get("id"), -1);

            if (bookId <= 0) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"缺少書籍 ID\"}");
                return;
            }

            String sql = "DELETE FROM " + TABLE_BOOKS + " WHERE book_id = ?";
            boolean success;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, bookId);
                success = ps.executeUpdate() > 0;
            }

            sendJson(exchange, 200, "{\"success\":" + success + "}");
        }
    }

    // =====================================================
    // 學生帳號管理
    // =====================================================
    static class UsersHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                getUsers(exchange);
            } else if ("PATCH".equalsIgnoreCase(method)) {
                updateUser(exchange);
            } else {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"不支援的方法\"}");
            }
        }

        private void getUsers(HttpExchange exchange) throws Exception {
            String sql = "SELECT user_id, student_no, name, role_level, status "
                    + "FROM " + TABLE_USERS + " ORDER BY user_id DESC";

            StringBuilder json = new StringBuilder("[");

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean first = true;

                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                            .append("\"user_id\":").append(rs.getInt("user_id")).append(",")
                            .append("\"student_no\":\"").append(jsonEscape(rs.getString("student_no"))).append("\",")
                            .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                            .append("\"role_level\":\"").append(jsonEscape(rs.getString("role_level"))).append("\",")
                            .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\"")
                            .append("}");
                }
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        }

        private void updateUser(HttpExchange exchange) throws Exception {
            Map<String, String> params = getQueryParams(exchange.getRequestURI());

            int userId = parseInt(params.get("id"), -1);
            String status = params.getOrDefault("status", "").trim();
            String role = params.getOrDefault("role", "").trim();

            if (userId <= 0) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"缺少使用者 ID\"}");
                return;
            }

            boolean success = false;

            try (Connection conn = DBUtil.getConnection()) {
                if (!status.isEmpty()) {
                    String sql = "UPDATE " + TABLE_USERS + " SET status = ? WHERE user_id = ?";

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, status.toUpperCase());
                        ps.setInt(2, userId);
                        success = ps.executeUpdate() > 0;
                    }
                }

                if (!role.isEmpty()) {
                    String sql = "UPDATE " + TABLE_USERS + " SET role_level = ? WHERE user_id = ?";

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, role.toUpperCase());
                        ps.setInt(2, userId);
                        success = ps.executeUpdate() > 0;
                    }
                }
            }

            sendJson(exchange, 200, "{\"success\":" + success + "}");
        }
    }

    // =====================================================
    // 全校借還紀錄
    // =====================================================
    static class RecordsHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            Map<String, String> params = getQueryParams(exchange.getRequestURI());
            String keyword = params.getOrDefault("keyword", "").trim();

            String sql = "SELECT u.student_no, u.name, b.title, r.borrow_date, r.due_date, r.return_date "
                    + "FROM " + TABLE_RECORDS + " r "
                    + "JOIN " + TABLE_USERS + " u ON r.user_id = u.user_id "
                    + "JOIN " + TABLE_BOOKS + " b ON r.book_id = b.book_id ";

            boolean hasKeyword = !keyword.isEmpty();

            if (hasKeyword) {
                sql += "WHERE u.student_no LIKE ? OR u.name LIKE ? OR b.title LIKE ? ";
            }

            sql += "ORDER BY r.return_date IS NULL DESC, r.borrow_date DESC";

            StringBuilder json = new StringBuilder("[");

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (hasKeyword) {
                    String search = "%" + keyword + "%";
                    ps.setString(1, search);
                    ps.setString(2, search);
                    ps.setString(3, search);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;

                    while (rs.next()) {
                        if (!first) json.append(",");
                        first = false;

                        String returnDate = rs.getString("return_date");

                        if (returnDate == null) {
                            returnDate = "尚未歸還";
                        }

                        json.append("{")
                                .append("\"student_no\":\"").append(jsonEscape(rs.getString("student_no"))).append("\",")
                                .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                                .append("\"borrow_date\":\"").append(jsonEscape(rs.getString("borrow_date"))).append("\",")
                                .append("\"due_date\":\"").append(jsonEscape(rs.getString("due_date"))).append("\",")
                                .append("\"return_date\":\"").append(jsonEscape(returnDate)).append("\"")
                                .append("}");
                    }
                }
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        }
    }

    // =====================================================
    // 書評管理
    // =====================================================
    static class ReviewsHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                getReviews(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                deleteReview(exchange);
            } else {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"不支援的方法\"}");
            }
        }

        private void getReviews(HttpExchange exchange) throws Exception {
            String sql = "SELECT r.review_id, b.title, u.name, r.rating, r.comment, r.created_at "
                    + "FROM " + TABLE_REVIEWS + " r "
                    + "JOIN " + TABLE_BOOKS + " b ON r.book_id = b.book_id "
                    + "JOIN " + TABLE_USERS + " u ON r.user_id = u.user_id "
                    + "ORDER BY r.created_at DESC";

            StringBuilder json = new StringBuilder("[");

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean first = true;

                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                            .append("\"review_id\":").append(rs.getInt("review_id")).append(",")
                            .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                            .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                            .append("\"rating\":").append(rs.getInt("rating")).append(",")
                            .append("\"comment\":\"").append(jsonEscape(rs.getString("comment"))).append("\",")
                            .append("\"created_at\":\"").append(jsonEscape(rs.getString("created_at"))).append("\"")
                            .append("}");
                }
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        }

        private void deleteReview(HttpExchange exchange) throws Exception {
            Map<String, String> params = getQueryParams(exchange.getRequestURI());
            int reviewId = parseInt(params.get("id"), -1);

            if (reviewId <= 0) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"缺少書評 ID\"}");
                return;
            }

            String sql = "DELETE FROM " + TABLE_REVIEWS + " WHERE review_id = ?";
            boolean success;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, reviewId);
                success = ps.executeUpdate() > 0;
            }

            sendJson(exchange, 200, "{\"success\":" + success + "}");
        }
    }

    // =====================================================
    // 借閱統計
    // =====================================================
    static class StatsHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws Exception {
            String sql = "SELECT COALESCE(NULLIF(b.subjects, ''), '未分類') AS subject, "
                    + "COUNT(r.record_id) AS borrow_count "
                    + "FROM " + TABLE_RECORDS + " r "
                    + "JOIN " + TABLE_BOOKS + " b ON r.book_id = b.book_id "
                    + "GROUP BY COALESCE(NULLIF(b.subjects, ''), '未分類') "
                    + "ORDER BY borrow_count DESC";

            StringBuilder json = new StringBuilder("[");

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean first = true;

                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                            .append("\"subject\":\"").append(jsonEscape(rs.getString("subject"))).append("\",")
                            .append("\"count\":").append(rs.getInt("borrow_count"))
                            .append("}");
                }
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        }
    }
}