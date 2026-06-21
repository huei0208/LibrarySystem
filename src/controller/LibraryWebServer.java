package controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.DBUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class LibraryWebServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBUtil.getConnection()) {
            ensureTables(conn);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Auth
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/ping", new PingHandler());

        // Student web APIs
        server.createContext("/api/books", new BooksHandler());
        server.createContext("/api/recommendations", new RecommendationsHandler());
        server.createContext("/api/book", new BookDetailHandler());
        server.createContext("/api/borrow", new BorrowHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/renew", new RenewHandler());
        server.createContext("/api/my/current", new MyCurrentHandler());
        server.createContext("/api/my/history", new MyHistoryHandler());
        server.createContext("/api/my/badges", new MyBadgesHandler());
        server.createContext("/api/my/reservations", new MyReservationsHandler());
        server.createContext("/api/reserve", new ReserveHandler());
        server.createContext("/api/reservation/status", new ReservationStatusHandler());
        server.createContext("/api/reservation/delete", new ReservationDeleteHandler());
        server.createContext("/api/my/favorites", new MyFavoritesHandler());
        server.createContext("/api/favorite/add", new FavoriteAddHandler());
        server.createContext("/api/favorite/delete", new FavoriteDeleteHandler());
        server.createContext("/api/reviews", new ReviewsByBookHandler());
        server.createContext("/api/review/add", new ReviewAddHandler());
        server.createContext("/api/notifications", new NotificationsHandler());
        server.createContext("/api/notification/read", new NotificationReadHandler());
        server.createContext("/api/vip/request", new VipRequestHandler());

        // Admin web APIs
        server.createContext("/api/admin/dashboard", new AdminDashboardHandler());
        server.createContext("/api/admin/books", new AdminBooksHandler());
        server.createContext("/api/admin/book/add", new AdminBookAddHandler());
        server.createContext("/api/admin/book/status", new AdminBookStatusHandler());
        server.createContext("/api/admin/users", new AdminUsersHandler());
        server.createContext("/api/admin/user/status", new AdminUserStatusHandler());
        server.createContext("/api/admin/user/role", new AdminUserRoleHandler());
        server.createContext("/api/admin/records", new AdminRecordsHandler());
        server.createContext("/api/admin/overdue", new AdminOverdueHandler());
        server.createContext("/api/admin/overdue/notify", new AdminOverdueNotifyHandler());
        server.createContext("/api/admin/export/records", new AdminRecordsExportHandler());
        server.createContext("/api/admin/reservations", new AdminReservationsHandler());
        server.createContext("/api/admin/reviews", new AdminReviewsHandler());
        server.createContext("/api/admin/review/delete", new AdminReviewDeleteHandler());
        server.createContext("/api/admin/stats", new AdminStatsHandler());
        server.createContext("/api/admin/risk-users", new AdminRiskUsersHandler());
        server.createContext("/api/admin/inventory-alerts", new AdminInventoryAlertsHandler());
        server.createContext("/api/admin/vip-requests", new AdminVipRequestsHandler());
        server.createContext("/api/admin/vip/approve", new AdminVipApproveHandler());
        server.createContext("/api/admin/vip/reject", new AdminVipRejectHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("圖書館 Web API 已啟動：http://localhost:" + PORT);
        System.out.println("登入頁請開 web/login.html，主頁請開 web/app.html");
        System.out.println("測試 API：http://localhost:" + PORT + "/api/ping");
    }

    // =========================================================
    // Base helpers
    // =========================================================
    static abstract class BaseHandler implements HttpHandler {
        @Override
        public final void handle(HttpExchange exchange) {
            try {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendText(exchange, 204, "");
                    return;
                }
                doHandle(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, "{\"success\":false,\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
                } catch (Exception ignored) {
                }
            }
        }

        protected abstract void doHandle(HttpExchange exchange) throws Exception;
    }

    private static void addCors(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws Exception {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        sendText(exchange, status, json);
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendCsv(HttpExchange exchange, String filename, String csv) throws Exception {
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private static Map<String, String> params(HttpExchange exchange) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        parseQueryInto(exchange.getRequestURI().getRawQuery(), map);

        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    body.append(line);
                }
            }
            parseQueryInto(body.toString(), map);
        }
        return map;
    }

    private static void parseQueryInto(String raw, Map<String, String> map) throws Exception {
        if (raw == null || raw.isBlank()) return;
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf('=');
            String k = idx >= 0 ? pair.substring(0, idx) : pair;
            String v = idx >= 0 ? pair.substring(idx + 1) : "";
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
    }

    private static int intParam(Map<String, String> p, String key, int def) {
        try {
            return Integer.parseInt(p.getOrDefault(key, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String s(Map<String, String> p, String key) {
        return p.getOrDefault(key, "").trim();
    }

    private static String jsonEscape(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String quote(Object value) {
        return "\"" + jsonEscape(value) + "\"";
    }

    private static String boolJson(boolean success, String message) {
        return "{\"success\":" + success + ",\"message\":" + quote(message) + "}";
    }

    private static int count(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static void ensureTables(Connection conn) throws SQLException {
        exec(conn, "CREATE TABLE IF NOT EXISTS users (" +
                "user_id INT AUTO_INCREMENT PRIMARY KEY," +
                "student_no VARCHAR(50) NOT NULL UNIQUE," +
                "name VARCHAR(100) NOT NULL," +
                "password VARCHAR(100) NOT NULL," +
                "role VARCHAR(20) DEFAULT 'student'," +
                "role_level VARCHAR(20) DEFAULT 'NORMAL'," +
                "status VARCHAR(20) DEFAULT 'ACTIVE'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS books (" +
                "book_id INT AUTO_INCREMENT PRIMARY KEY," +
                "title VARCHAR(255) NOT NULL," +
                "authors TEXT," +
                "subjects TEXT," +
                "publisher VARCHAR(255)," +
                "publish_year VARCHAR(20)," +
                "edition VARCHAR(100)," +
                "format_desc VARCHAR(255)," +
                "source VARCHAR(255)," +
                "note TEXT," +
                "status VARCHAR(20) DEFAULT 'AVAILABLE'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS borrow_records (" +
                "record_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "book_id INT NOT NULL," +
                "borrow_date DATETIME," +
                "due_date DATETIME," +
                "return_date DATETIME NULL," +
                "borrow_days INT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS book_reviews (" +
                "review_id INT AUTO_INCREMENT PRIMARY KEY," +
                "book_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "rating INT DEFAULT 5," +
                "comment TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS reservations (" +
                "reservation_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "book_id INT NOT NULL," +
                "reserve_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status VARCHAR(20) DEFAULT 'WAITING'" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS favorites (" +
                "user_id INT NOT NULL," +
                "book_id INT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (user_id, book_id)" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS notifications (" +
                "notification_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "message TEXT NOT NULL," +
                "is_read BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS admins (" +
                "admin_id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(50) NOT NULL UNIQUE," +
                "password VARCHAR(100) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        exec(conn, "CREATE TABLE IF NOT EXISTS vip_requests (" +
                "request_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "status VARCHAR(20) DEFAULT 'PENDING'," +
                "message TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "handled_at DATETIME NULL" +
                ")");

        ensureColumn(conn, "users", "role", "role VARCHAR(20) DEFAULT 'student' AFTER password");
        ensureColumn(conn, "users", "role_level", "role_level VARCHAR(20) DEFAULT 'NORMAL'");
        ensureColumn(conn, "users", "status", "status VARCHAR(20) DEFAULT 'ACTIVE'");
        ensureColumn(conn, "books", "authors", "authors TEXT");
        ensureColumn(conn, "books", "subjects", "subjects TEXT");
        ensureColumn(conn, "books", "publisher", "publisher VARCHAR(255)");
        ensureColumn(conn, "books", "publish_year", "publish_year VARCHAR(20)");
        ensureColumn(conn, "books", "status", "status VARCHAR(20) DEFAULT 'AVAILABLE'");
        ensureColumn(conn, "borrow_records", "borrow_days", "borrow_days INT AFTER return_date");
        ensureColumn(conn, "borrow_records", "renewed_count", "renewed_count INT DEFAULT 0 AFTER borrow_days");
        ensureColumn(conn, "borrow_records", "created_at", "created_at DATETIME DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(conn, "reservations", "status", "status VARCHAR(20) DEFAULT 'WAITING'");
        ensureColumn(conn, "favorites", "created_at", "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        exec(conn, "INSERT IGNORE INTO admins (username, password) VALUES ('admin', '1234')");
    }

    private static void ensureColumn(Connection conn, String table, String column, String definition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    exec(conn, "ALTER TABLE " + table + " ADD COLUMN " + definition);
                }
            }
        }
    }

    private static int borrowLimit(String roleLevel) {
        return "VIP".equalsIgnoreCase(roleLevel) ? 5 : 3;
    }

    private static int borrowDays(String roleLevel) {
        return "VIP".equalsIgnoreCase(roleLevel) ? 14 : 7;
    }

    private static String normalizeRoleLevel(String role) {
        if (role == null) return "NORMAL";
        String r = role.trim().toUpperCase();
        if (r.contains("VIP")) return "VIP";
        if (r.contains("ADMIN") || r.contains("管理")) return "ADMIN";
        return "NORMAL";
    }

    // =========================================================
    // Auth
    // =========================================================
    static class PingHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            sendJson(e, 200, "{\"success\":true,\"message\":\"API OK\"}");
        }
    }

    static class LoginHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String username = s(p, "username");
            String password = s(p, "password");

            if (username.isEmpty() || password.isEmpty()) {
                sendJson(e, 400, boolJson(false, "請輸入帳號與密碼"));
                return;
            }

            try (Connection conn = DBUtil.getConnection()) {
                String adminSql = "SELECT admin_id, username FROM admins WHERE username = ? AND password = ?";
                try (PreparedStatement ps = conn.prepareStatement(adminSql)) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String json = "{\"success\":true,\"role\":\"ADMIN\",\"user_id\":0," +
                                    "\"name\":" + quote(rs.getString("username")) + ",\"student_no\":" + quote(username) + "," +
                                    "\"role_level\":\"ADMIN\",\"status\":\"ACTIVE\",\"borrow_limit\":999}";
                            sendJson(e, 200, json);
                            return;
                        }
                    }
                }

                String sql = "SELECT user_id, student_no, name, role, role_level, status FROM users WHERE student_no = ? AND password = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if (status != null && (status.equalsIgnoreCase("SUSPENDED") || status.equalsIgnoreCase("DISABLED"))) {
                                sendJson(e, 403, boolJson(false, "此帳號已被停權，請聯絡管理者"));
                                return;
                            }
                            String roleLevel = normalizeRoleLevel(rs.getString("role_level"));
                            String role = "ADMIN".equals(roleLevel) ? "ADMIN" : "STUDENT";
                            String json = "{\"success\":true," +
                                    "\"user_id\":" + rs.getInt("user_id") + "," +
                                    "\"student_no\":" + quote(rs.getString("student_no")) + "," +
                                    "\"name\":" + quote(rs.getString("name")) + "," +
                                    "\"role\":" + quote(role) + "," +
                                    "\"role_level\":" + quote(roleLevel) + "," +
                                    "\"status\":" + quote(status) + "," +
                                    "\"borrow_limit\":" + borrowLimit(roleLevel) + "}";
                            sendJson(e, 200, json);
                            return;
                        }
                    }
                }
            }
            sendJson(e, 200, boolJson(false, "帳號或密碼錯誤"));
        }
    }

    static class RegisterHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String studentNo = s(p, "student_no");
            String name = s(p, "name");
            String password = s(p, "password");
            if (studentNo.isEmpty() || name.isEmpty() || password.isEmpty()) {
                sendJson(e, 400, boolJson(false, "請完整輸入學號、姓名、密碼"));
                return;
            }
            String sql = "INSERT INTO users (student_no, name, password, role, role_level, status) VALUES (?, ?, ?, 'student', 'NORMAL', 'ACTIVE')";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, studentNo);
                ps.setString(2, name);
                ps.setString(3, password);
                ps.executeUpdate();
                sendJson(e, 200, boolJson(true, "註冊成功"));
            } catch (SQLIntegrityConstraintViolationException dup) {
                sendJson(e, 200, boolJson(false, "此學號已經註冊過"));
            }
        }
    }

    // =========================================================
    // Student APIs
    // =========================================================
    static class BooksHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String keyword = s(p, "keyword");
            String onlyAvailable = s(p, "available");
            int userId = intParam(p, "user_id", -1);

            StringBuilder sql = new StringBuilder(
                    "SELECT b.book_id, b.title, b.authors, b.subjects, b.publisher, b.publish_year, b.status, " +
                    "(SELECT COUNT(*) FROM reservations r WHERE r.book_id = b.book_id AND r.status IN ('WAITING','READY')) AS reservation_count ");

            if (userId > 0) {
                sql.append(", EXISTS(SELECT 1 FROM reservations r2 WHERE r2.book_id = b.book_id AND r2.user_id = ? AND r2.status IN ('WAITING','READY')) AS is_reserved ");
                sql.append(", EXISTS(SELECT 1 FROM favorites f WHERE f.book_id = b.book_id AND f.user_id = ?) AS is_favorite ");
            } else {
                sql.append(", FALSE AS is_reserved, FALSE AS is_favorite ");
            }

            sql.append("FROM books b WHERE 1=1 ");
            List<String> bind = new ArrayList<>();
            if (!keyword.isEmpty()) {
                sql.append("AND (b.title LIKE ? OR b.authors LIKE ? OR b.subjects LIKE ? OR b.publisher LIKE ?) ");
                String k = "%" + keyword + "%";
                bind.add(k); bind.add(k); bind.add(k); bind.add(k);
            }
            if ("true".equalsIgnoreCase(onlyAvailable)) sql.append("AND b.status = 'AVAILABLE' ");
            sql.append("ORDER BY b.book_id DESC LIMIT 300");

            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (userId > 0) {
                    ps.setInt(idx++, userId);
                    ps.setInt(idx++, userId);
                }
                for (String value : bind) ps.setString(idx++, value);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"book_id\":").append(rs.getInt("book_id"))
                                .append(",\"title\":").append(quote(rs.getString("title")))
                                .append(",\"authors\":").append(quote(rs.getString("authors")))
                                .append(",\"subjects\":").append(quote(rs.getString("subjects")))
                                .append(",\"publisher\":").append(quote(rs.getString("publisher")))
                                .append(",\"publish_year\":").append(quote(rs.getString("publish_year")))
                                .append(",\"status\":").append(quote(rs.getString("status")))
                                .append(",\"reservation_count\":").append(rs.getInt("reservation_count"))
                                .append(",\"is_reserved\":").append(rs.getBoolean("is_reserved"))
                                .append(",\"is_favorite\":").append(rs.getBoolean("is_favorite"))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }


    private static String bookObjectWithStatus(ResultSet rs) throws SQLException {
        StringBuilder obj = new StringBuilder();
        obj.append("{\"book_id\":").append(rs.getInt("book_id"))
                .append(",\"title\":").append(quote(rs.getString("title")))
                .append(",\"authors\":").append(quote(rs.getString("authors")))
                .append(",\"subjects\":").append(quote(rs.getString("subjects")))
                .append(",\"publisher\":").append(quote(rs.getString("publisher")))
                .append(",\"publish_year\":").append(quote(rs.getString("publish_year")))
                .append(",\"status\":").append(quote(rs.getString("status")))
                .append(",\"reservation_count\":").append(rs.getInt("reservation_count"))
                .append(",\"is_reserved\":").append(rs.getBoolean("is_reserved"))
                .append(",\"is_favorite\":").append(rs.getBoolean("is_favorite"));
        try {
            obj.append(",\"reason\":").append(quote(rs.getString("reason")));
        } catch (SQLException ignored) {
        }
        obj.append('}');
        return obj.toString();
    }

    private static List<String> preferenceKeywords(Connection conn, int userId) throws SQLException {
        List<String> keywords = new ArrayList<>();
        String sql = "SELECT b.subjects FROM books b JOIN borrow_records r ON b.book_id = r.book_id WHERE r.user_id = ? AND b.subjects IS NOT NULL AND b.subjects <> '' " +
                "UNION ALL SELECT b.subjects FROM books b JOIN favorites f ON b.book_id = f.book_id WHERE f.user_id = ? AND b.subjects IS NOT NULL AND b.subjects <> '' LIMIT 12";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subjects = rs.getString(1);
                    if (subjects == null) continue;
                    for (String part : subjects.split("[、,，;；/\\s]+")) {
                        String kw = part.trim();
                        if (kw.length() >= 2 && !keywords.contains(kw)) keywords.add(kw);
                        if (keywords.size() >= 5) return keywords;
                    }
                }
            }
        }
        return keywords;
    }

    static class RecommendationsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            if (userId <= 0) {
                sendJson(e, 400, boolJson(false, "缺少使用者 ID"));
                return;
            }
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection()) {
                List<String> keys = preferenceKeywords(conn, userId);
                String baseSelect = "SELECT b.book_id, b.title, b.authors, b.subjects, b.publisher, b.publish_year, b.status, " +
                        "(SELECT COUNT(*) FROM reservations r WHERE r.book_id = b.book_id AND r.status IN ('WAITING','READY')) AS reservation_count, " +
                        "EXISTS(SELECT 1 FROM reservations r2 WHERE r2.book_id = b.book_id AND r2.user_id = ? AND r2.status IN ('WAITING','READY')) AS is_reserved, " +
                        "EXISTS(SELECT 1 FROM favorites f WHERE f.book_id = b.book_id AND f.user_id = ?) AS is_favorite, ";
                PreparedStatement ps;
                if (!keys.isEmpty()) {
                    StringBuilder where = new StringBuilder();
                    for (int i = 0; i < keys.size(); i++) {
                        if (i > 0) where.append(" OR ");
                        where.append("b.subjects LIKE ? OR b.title LIKE ?");
                    }
                    String sql = baseSelect + "? AS reason FROM books b WHERE b.status = 'AVAILABLE' " +
                            "AND NOT EXISTS(SELECT 1 FROM borrow_records br WHERE br.user_id = ? AND br.book_id = b.book_id AND br.return_date IS NULL) " +
                            "AND (" + where + ") ORDER BY b.book_id DESC LIMIT 10";
                    ps = conn.prepareStatement(sql);
                    int idx = 1;
                    ps.setInt(idx++, userId);
                    ps.setInt(idx++, userId);
                    ps.setString(idx++, "根據你的借閱與收藏主題推薦：" + String.join("、", keys));
                    ps.setInt(idx++, userId);
                    for (String key : keys) {
                        ps.setString(idx++, "%" + key + "%");
                        ps.setString(idx++, "%" + key + "%");
                    }
                } else {
                    String sql = baseSelect + "'熱門書籍推薦' AS reason FROM books b WHERE b.status = 'AVAILABLE' " +
                            "ORDER BY (SELECT COUNT(*) FROM borrow_records br WHERE br.book_id = b.book_id) DESC, b.book_id DESC LIMIT 10";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, userId);
                    ps.setInt(2, userId);
                }
                try (ps; ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append(bookObjectWithStatus(rs));
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class BookDetailHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int bookId = intParam(params(e), "id", -1);
            String sql = "SELECT * FROM books WHERE book_id = ?";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sendJson(e, 404, boolJson(false, "找不到書籍"));
                        return;
                    }
                    String json = "{\"book_id\":" + rs.getInt("book_id") +
                            ",\"title\":" + quote(rs.getString("title")) +
                            ",\"authors\":" + quote(rs.getString("authors")) +
                            ",\"subjects\":" + quote(rs.getString("subjects")) +
                            ",\"publisher\":" + quote(rs.getString("publisher")) +
                            ",\"publish_year\":" + quote(rs.getString("publish_year")) +
                            ",\"edition\":" + quote(rs.getString("edition")) +
                            ",\"format_desc\":" + quote(rs.getString("format_desc")) +
                            ",\"source\":" + quote(rs.getString("source")) +
                            ",\"note\":" + quote(rs.getString("note")) +
                            ",\"status\":" + quote(rs.getString("status")) + "}";
                    sendJson(e, 200, json);
                }
            }
        }
    }

    static class BorrowHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            int bookId = intParam(p, "book_id", -1);
            if (userId <= 0 || bookId <= 0) {
                sendJson(e, 400, boolJson(false, "缺少使用者或書籍 ID"));
                return;
            }
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    String userSql = "SELECT role_level, status FROM users WHERE user_id = ?";
                    String roleLevel;
                    String status;
                    try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                        ps.setInt(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("找不到使用者");
                            roleLevel = normalizeRoleLevel(rs.getString("role_level"));
                            status = rs.getString("status");
                        }
                    }
                    if (status != null && (status.equalsIgnoreCase("SUSPENDED") || status.equalsIgnoreCase("DISABLED"))) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "帳號已停權，不能借書"));
                        return;
                    }

                    int overdueCount = countUserOverdue(conn, userId);
                    if (overdueCount > 0) {
                        addNotification(conn, userId,
                                "借閱失敗提醒：你目前有 " + overdueCount + " 本逾期未還書籍，請先完成還書後再借閱新書。系統已暫停你的借書功能。");
                        conn.commit();
                        sendJson(e, 200, boolJson(false, "你目前有 " + overdueCount + " 本逾期未還，請先還書後才能借閱新書。通知已寄到通知中心。"));
                        return;
                    }

                    int current = countUserBorrowing(conn, userId);
                    int limit = borrowLimit(roleLevel);
                    if (current >= limit) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "已達借閱上限：" + limit + " 本"));
                        return;
                    }

                    String bookStatus = null;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM books WHERE book_id = ? FOR UPDATE")) {
                        ps.setInt(1, bookId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("找不到書籍");
                            bookStatus = rs.getString("status");
                        }
                    }
                    if (!"AVAILABLE".equalsIgnoreCase(bookStatus)) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "此書已被借出，請改用預約"));
                        return;
                    }

                    int readyOwner = firstReadyReservationOwner(conn, bookId);
                    if (readyOwner > 0 && readyOwner != userId) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "此書已通知預約者優先借閱，暫時不能被其他人借走"));
                        return;
                    }

                    int days = borrowDays(roleLevel);
                    String insert = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), NULL, ?, NOW())";
                    try (PreparedStatement ps = conn.prepareStatement(insert)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, bookId);
                        ps.setInt(3, days);
                        ps.setInt(4, days);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE books SET status = 'BORROWED' WHERE book_id = ?")) {
                        ps.setInt(1, bookId);
                        ps.executeUpdate();
                    }
                    completeUserReservationIfAny(conn, userId, bookId);
                    addNotification(conn, userId, "借閱成功：你已成功借閱此書，借閱天數為 " + days + " 天。請記得準時歸還。");
                    conn.commit();
                    sendJson(e, 200, boolJson(true, "借閱成功，借閱天數：" + days + " 天"));
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        }
    }

    private static int countUserBorrowing(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int countUserOverdue(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL AND due_date < NOW()")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int firstReadyReservationOwner(Connection conn, int bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM reservations WHERE book_id = ? AND status = 'READY' ORDER BY reserve_date ASC LIMIT 1")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    private static void completeUserReservationIfAny(Connection conn, int userId, int bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE reservations SET status = 'COMPLETED' WHERE user_id = ? AND book_id = ? AND status IN ('WAITING','READY')")) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }

    static class ReturnHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            int recordId = intParam(p, "record_id", -1);
            int rating = intParam(p, "rating", 0);
            String comment = s(p, "comment");
            if (userId <= 0 || recordId <= 0) {
                sendJson(e, 400, boolJson(false, "缺少使用者或紀錄 ID"));
                return;
            }
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int bookId = -1;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM borrow_records WHERE record_id = ? AND user_id = ? AND return_date IS NULL")) {
                        ps.setInt(1, recordId);
                        ps.setInt(2, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                sendJson(e, 200, boolJson(false, "找不到可歸還的借閱紀錄"));
                                return;
                            }
                            bookId = rs.getInt("book_id");
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE borrow_records SET return_date = NOW() WHERE record_id = ?")) {
                        ps.setInt(1, recordId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE books SET status = 'AVAILABLE' WHERE book_id = ?")) {
                        ps.setInt(1, bookId);
                        ps.executeUpdate();
                    }
                    if (rating >= 1 && rating <= 5 && !comment.isBlank()) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO book_reviews (book_id, user_id, rating, comment) VALUES (?, ?, ?, ?)")) {
                            ps.setInt(1, bookId);
                            ps.setInt(2, userId);
                            ps.setInt(3, rating);
                            ps.setString(4, comment);
                            ps.executeUpdate();
                        }
                    }
                    notifyFirstReservation(conn, bookId);
                    conn.commit();
                    sendJson(e, 200, boolJson(true, "還書成功"));
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        }
    }

    private static void notifyFirstReservation(Connection conn, int bookId) throws SQLException {
        String sql = "SELECT r.reservation_id, r.user_id, b.title FROM reservations r JOIN books b ON r.book_id = b.book_id WHERE r.book_id = ? AND r.status = 'WAITING' ORDER BY r.reserve_date ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int rid = rs.getInt("reservation_id");
                    int uid = rs.getInt("user_id");
                    String title = rs.getString("title");
                    try (PreparedStatement ups = conn.prepareStatement("UPDATE reservations SET status = 'READY' WHERE reservation_id = ?")) {
                        ups.setInt(1, rid);
                        ups.executeUpdate();
                    }
                    addNotification(conn, uid, "你預約的《" + title + "》已可借閱，請盡快處理。");
                }
            }
        }
    }

    private static void addNotification(Connection conn, int userId, String message) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO notifications (user_id, message) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }


    static class RenewHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            int recordId = intParam(p, "record_id", -1);
            if (userId <= 0 || recordId <= 0) {
                sendJson(e, 400, boolJson(false, "缺少使用者或借閱紀錄 ID"));
                return;
            }
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int bookId = -1;
                    int renewed = 0;
                    String title = "";
                    int overdueDays = 0;
                    String sql = "SELECT r.book_id, r.renewed_count, b.title, DATEDIFF(NOW(), r.due_date) AS overdue_days " +
                            "FROM borrow_records r JOIN books b ON r.book_id = b.book_id " +
                            "WHERE r.record_id = ? AND r.user_id = ? AND r.return_date IS NULL";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, recordId);
                        ps.setInt(2, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                sendJson(e, 200, boolJson(false, "找不到可續借的借閱紀錄"));
                                return;
                            }
                            bookId = rs.getInt("book_id");
                            renewed = rs.getInt("renewed_count");
                            title = rs.getString("title");
                            overdueDays = rs.getInt("overdue_days");
                        }
                    }
                    if (overdueDays > 0) {
                        addNotification(conn, userId, "續借失敗提醒：你借閱的《" + title + "》已逾期 " + overdueDays + " 天，請先歸還後再借閱。");
                        conn.commit();
                        sendJson(e, 200, boolJson(false, "這本書已逾期，不能續借。通知已寄到通知中心。"));
                        return;
                    }
                    if (renewed >= 1) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "每筆借閱最多只能續借 1 次"));
                        return;
                    }
                    int waiting = 0;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reservations WHERE book_id = ? AND user_id <> ? AND status IN ('WAITING','READY')")) {
                        ps.setInt(1, bookId);
                        ps.setInt(2, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            waiting = rs.next() ? rs.getInt(1) : 0;
                        }
                    }
                    if (waiting > 0) {
                        conn.rollback();
                        sendJson(e, 200, boolJson(false, "這本書已有其他使用者預約，不能續借"));
                        return;
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE borrow_records SET due_date = DATE_ADD(due_date, INTERVAL 7 DAY), renewed_count = renewed_count + 1 WHERE record_id = ?")) {
                        ps.setInt(1, recordId);
                        ps.executeUpdate();
                    }
                    addNotification(conn, userId, "續借成功：你借閱的《" + title + "》已延長 7 天，請記得準時歸還。");
                    conn.commit();
                    sendJson(e, 200, boolJson(true, "續借成功，已延長 7 天"));
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        }
    }

    static class MyCurrentHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            String sql = "SELECT r.record_id, b.book_id, b.title, r.borrow_date, r.due_date, r.renewed_count, DATEDIFF(NOW(), r.due_date) AS overdue_days " +
                    "FROM borrow_records r JOIN books b ON r.book_id = b.book_id WHERE r.user_id = ? AND r.return_date IS NULL ORDER BY r.due_date ASC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        int overdue = Math.max(0, rs.getInt("overdue_days"));
                        json.append("{\"record_id\":").append(rs.getInt("record_id"))
                                .append(",\"book_id\":").append(rs.getInt("book_id"))
                                .append(",\"title\":").append(quote(rs.getString("title")))
                                .append(",\"borrow_date\":").append(quote(rs.getString("borrow_date")))
                                .append(",\"due_date\":").append(quote(rs.getString("due_date")))
                                .append(",\"overdue_days\":").append(overdue)
                                .append(",\"fine\":").append(overdue * 20)
                                .append(",\"renewed_count\":").append(rs.getInt("renewed_count"))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class MyHistoryHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            String sql = "SELECT r.record_id, b.title, r.borrow_date, r.due_date, r.return_date FROM borrow_records r JOIN books b ON r.book_id = b.book_id WHERE r.user_id = ? ORDER BY r.borrow_date DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"record_id\":").append(rs.getInt("record_id"))
                                .append(",\"title\":").append(quote(rs.getString("title")))
                                .append(",\"borrow_date\":").append(quote(rs.getString("borrow_date")))
                                .append(",\"due_date\":").append(quote(rs.getString("due_date")))
                                .append(",\"return_date\":").append(quote(rs.getString("return_date")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class ReserveHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            int bookId = intParam(p, "book_id", -1);
            if (userId <= 0 || bookId <= 0) {
                sendJson(e, 400, boolJson(false, "缺少使用者或書籍 ID"));
                return;
            }
            try (Connection conn = DBUtil.getConnection()) {
                String title = "";
                String status = "";
                try (PreparedStatement ps = conn.prepareStatement("SELECT title, status FROM books WHERE book_id = ?")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            sendJson(e, 200, boolJson(false, "找不到書籍"));
                            return;
                        }
                        title = rs.getString("title");
                        status = rs.getString("status");
                    }
                }
                if ("AVAILABLE".equalsIgnoreCase(status)) {
                    sendJson(e, 200, boolJson(false, "這本書目前可直接借閱，不需要預約"));
                    return;
                }
                String check = "SELECT COUNT(*) FROM reservations WHERE user_id = ? AND book_id = ? AND status IN ('WAITING','READY')";
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            sendJson(e, 200, boolJson(false, "你已經預約過這本書"));
                            return;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO reservations (user_id, book_id, status) VALUES (?, ?, 'WAITING')")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, bookId);
                    ps.executeUpdate();
                }
                addNotification(conn, userId, "預約成功：你已預約《" + title + "》，書籍歸還後系統會通知你。");
                sendJson(e, 200, boolJson(true, "預約成功，已加入我的預約並寄到通知中心"));
            }
        }
    }

    static class MyReservationsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            sendJson(e, 200, reservationListJson("WHERE r.user_id = ?", userId));
        }
    }

    static class ReservationStatusHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int id = intParam(p, "id", -1);
            String status = s(p, "status").toUpperCase();
            if (!List.of("WAITING", "READY", "COMPLETED", "CANCELLED").contains(status)) status = "WAITING";
            try (Connection conn = DBUtil.getConnection()) {
                int userId = -1;
                String title = "";
                try (PreparedStatement q = conn.prepareStatement("SELECT r.user_id, b.title FROM reservations r JOIN books b ON r.book_id = b.book_id WHERE r.reservation_id = ?")) {
                    q.setInt(1, id);
                    try (ResultSet rs = q.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                            title = rs.getString("title");
                        }
                    }
                }
                boolean ok;
                try (PreparedStatement ps = conn.prepareStatement("UPDATE reservations SET status = ? WHERE reservation_id = ?")) {
                    ps.setString(1, status);
                    ps.setInt(2, id);
                    ok = ps.executeUpdate() > 0;
                }
                if (ok && userId > 0 && "READY".equals(status)) {
                    addNotification(conn, userId, "預約通知：你預約的《" + title + "》目前可借閱，請盡快前往借書。");
                }
                sendJson(e, 200, boolJson(ok, "更新完成"));
            }
        }
    }

    static class ReservationDeleteHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int id = intParam(params(e), "id", -1);
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM reservations WHERE reservation_id = ?")) {
                ps.setInt(1, id);
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "刪除完成"));
            }
        }
    }

    private static String reservationListJson(String where, int value) throws SQLException {
        String sql = "SELECT r.reservation_id, r.user_id, u.student_no, u.name, r.book_id, b.title, r.reserve_date, r.status " +
                "FROM reservations r JOIN users u ON r.user_id = u.user_id JOIN books b ON r.book_id = b.book_id " + where + " ORDER BY r.reserve_date DESC";
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append(reservationObject(rs));
                }
            }
        }
        json.append(']');
        return json.toString();
    }

    private static String reservationObject(ResultSet rs) throws SQLException {
        return "{\"reservation_id\":" + rs.getInt("reservation_id") +
                ",\"user_id\":" + rs.getInt("user_id") +
                ",\"student_no\":" + quote(rs.getString("student_no")) +
                ",\"name\":" + quote(rs.getString("name")) +
                ",\"book_id\":" + rs.getInt("book_id") +
                ",\"title\":" + quote(rs.getString("title")) +
                ",\"reserve_date\":" + quote(rs.getString("reserve_date")) +
                ",\"status\":" + quote(rs.getString("status")) + "}";
    }

    static class MyFavoritesHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            String sql = "SELECT b.book_id, b.title, b.authors, b.subjects, b.status, f.created_at, " +
                    "(SELECT COUNT(*) FROM reservations r WHERE r.book_id = b.book_id AND r.status IN ('WAITING','READY')) AS reservation_count, " +
                    "EXISTS(SELECT 1 FROM reservations r2 WHERE r2.book_id = b.book_id AND r2.user_id = ? AND r2.status IN ('WAITING','READY')) AS is_reserved " +
                    "FROM favorites f JOIN books b ON f.book_id = b.book_id WHERE f.user_id = ? ORDER BY f.created_at DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"book_id\":").append(rs.getInt("book_id"))
                                .append(",\"title\":").append(quote(rs.getString("title")))
                                .append(",\"authors\":").append(quote(rs.getString("authors")))
                                .append(",\"subjects\":").append(quote(rs.getString("subjects")))
                                .append(",\"status\":").append(quote(rs.getString("status")))
                                .append(",\"reservation_count\":").append(rs.getInt("reservation_count"))
                                .append(",\"is_reserved\":").append(rs.getBoolean("is_reserved"))
                                .append(",\"is_favorite\":true")
                                .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class FavoriteAddHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO favorites (user_id, book_id) VALUES (?, ?)")) {
                ps.setInt(1, intParam(p, "user_id", -1));
                ps.setInt(2, intParam(p, "book_id", -1));
                int affected = ps.executeUpdate();
                sendJson(e, 200, boolJson(true, affected > 0 ? "已加入收藏" : "這本書已經在收藏清單中"));
            }
        }
    }

    static class FavoriteDeleteHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM favorites WHERE user_id = ? AND book_id = ?")) {
                ps.setInt(1, intParam(p, "user_id", -1));
                ps.setInt(2, intParam(p, "book_id", -1));
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "已移除收藏"));
            }
        }
    }

    static class ReviewsByBookHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int bookId = intParam(params(e), "book_id", -1);
            String sql = "SELECT rv.review_id, rv.rating, rv.comment, rv.created_at, u.name FROM book_reviews rv JOIN users u ON rv.user_id = u.user_id WHERE rv.book_id = ? ORDER BY rv.created_at DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"review_id\":").append(rs.getInt("review_id"))
                                .append(",\"rating\":").append(rs.getInt("rating"))
                                .append(",\"comment\":").append(quote(rs.getString("comment")))
                                .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                                .append(",\"name\":").append(quote(rs.getString("name")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class ReviewAddHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            int bookId = intParam(p, "book_id", -1);
            int rating = intParam(p, "rating", 5);
            String comment = s(p, "comment");
            if (comment.isBlank()) {
                sendJson(e, 200, boolJson(false, "請輸入書評內容"));
                return;
            }
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO book_reviews (book_id, user_id, rating, comment) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, bookId);
                ps.setInt(2, userId);
                ps.setInt(3, Math.max(1, Math.min(5, rating)));
                ps.setString(4, comment);
                ps.executeUpdate();
                sendJson(e, 200, boolJson(true, "書評已送出"));
            }
        }
    }

    static class NotificationsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            String sql = "SELECT notification_id, message, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"notification_id\":").append(rs.getInt("notification_id"))
                                .append(",\"message\":").append(quote(rs.getString("message")))
                                .append(",\"is_read\":").append(rs.getBoolean("is_read"))
                                .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class NotificationReadHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int id = intParam(p, "notification_id", -1);
            int userId = intParam(p, "user_id", -1);
            try (Connection conn = DBUtil.getConnection()) {
                if (id == -1 && userId > 0) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE user_id = ?")) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                        sendJson(e, 200, boolJson(true, "全部設為已讀"));
                        return;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE notification_id = ?")) {
                    ps.setInt(1, id);
                    sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "已讀"));
                }
            }
        }
    }

    static class VipRequestHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            try (Connection conn = DBUtil.getConnection()) {
                try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM vip_requests WHERE user_id = ? AND status = 'PENDING'")) {
                    check.setInt(1, userId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            sendJson(e, 200, boolJson(false, "你已經有一筆待審核 VIP 申請"));
                            return;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO vip_requests (user_id, message) VALUES (?, '使用者申請升級 VIP')")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                    sendJson(e, 200, boolJson(true, "VIP 申請已送出"));
                }
            }
        }
    }

    // =========================================================
    // Admin APIs
    // =========================================================

    static class MyBadgesHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int userId = intParam(params(e), "user_id", -1);
            try (Connection conn = DBUtil.getConnection()) {
                int history = 0, reviews = 0, favorites = 0, current = 0, overdue = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE user_id = ?")) { ps.setInt(1, userId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) history=rs.getInt(1); } }
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM book_reviews WHERE user_id = ?")) { ps.setInt(1, userId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) reviews=rs.getInt(1); } }
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM favorites WHERE user_id = ?")) { ps.setInt(1, userId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) favorites=rs.getInt(1); } }
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL")) { ps.setInt(1, userId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) current=rs.getInt(1); } }
                overdue = countUserOverdue(conn, userId);
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                first = appendBadge(json, first, "新手借閱者", history >= 1, "完成第一次借書");
                first = appendBadge(json, first, "閱讀達人", history >= 5, "累積借閱 5 本以上");
                first = appendBadge(json, first, "書評貢獻者", reviews >= 1, "撰寫至少 1 則書評");
                first = appendBadge(json, first, "收藏家", favorites >= 3, "收藏 3 本以上書籍");
                first = appendBadge(json, first, "準時讀者", current > 0 && overdue == 0, "目前借閱沒有逾期");
                json.append(']');
                sendJson(e, 200, json.toString());
            }
        }
    }

    private static boolean appendBadge(StringBuilder json, boolean first, String name, boolean earned, String description) {
        if (!first) json.append(',');
        json.append("{\"name\":").append(quote(name))
                .append(",\"earned\":").append(earned)
                .append(",\"description\":").append(quote(description))
                .append('}');
        return false;
    }

    static class AdminDashboardHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            try (Connection conn = DBUtil.getConnection()) {
                int books = count(conn, "SELECT COUNT(*) FROM books");
                int users = count(conn, "SELECT COUNT(*) FROM users");
                int borrowing = count(conn, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL");
                int overdue = count(conn, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL AND due_date < NOW()");
                int reviews = count(conn, "SELECT COUNT(*) FROM book_reviews");
                int reservations = count(conn, "SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING','READY')");
                int vipRequests = count(conn, "SELECT COUNT(*) FROM vip_requests WHERE status = 'PENDING'");
                sendJson(e, 200, "{\"bookCount\":" + books + ",\"userCount\":" + users + ",\"borrowingCount\":" + borrowing + ",\"overdueCount\":" + overdue + ",\"reviewCount\":" + reviews + ",\"reservationCount\":" + reservations + ",\"vipRequestCount\":" + vipRequests + "}");
            }
        }
    }

    static class AdminBooksHandler extends BooksHandler {}

    static class AdminBookAddHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String title = s(p, "title");
            if (title.isEmpty()) {
                sendJson(e, 200, boolJson(false, "請輸入書名"));
                return;
            }
            String sql = "INSERT INTO books (title, authors, subjects, publisher, publish_year, status) VALUES (?, ?, ?, ?, ?, 'AVAILABLE')";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, s(p, "authors"));
                ps.setString(3, s(p, "subjects"));
                ps.setString(4, s(p, "publisher"));
                ps.setString(5, s(p, "publish_year"));
                ps.executeUpdate();
                sendJson(e, 200, boolJson(true, "新增書籍成功"));
            }
        }
    }

    static class AdminBookStatusHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int bookId = intParam(p, "book_id", -1);
            String status = s(p, "status").toUpperCase();
            if (!List.of("AVAILABLE", "BORROWED", "REMOVED").contains(status)) status = "AVAILABLE";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE books SET status = ? WHERE book_id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, bookId);
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "更新書籍狀態完成"));
            }
        }
    }

    static class AdminUsersHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String keyword = s(p, "keyword");
            String sql = "SELECT user_id, student_no, name, role_level, status, created_at FROM users " +
                    (keyword.isBlank() ? "" : "WHERE student_no LIKE ? OR name LIKE ? ") + "ORDER BY user_id ASC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!keyword.isBlank()) {
                    ps.setString(1, "%" + keyword + "%");
                    ps.setString(2, "%" + keyword + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        String role = normalizeRoleLevel(rs.getString("role_level"));
                        json.append("{\"user_id\":").append(rs.getInt("user_id"))
                                .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                                .append(",\"name\":").append(quote(rs.getString("name")))
                                .append(",\"role_level\":").append(quote(role))
                                .append(",\"borrow_limit\":").append(borrowLimit(role))
                                .append(",\"status\":").append(quote(rs.getString("status")))
                                .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminUserStatusHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            String status = s(p, "status").toUpperCase();
            if (!List.of("ACTIVE", "SUSPENDED", "DISABLED").contains(status)) status = "ACTIVE";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = ? WHERE user_id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, userId);
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "使用者狀態已更新"));
            }
        }
    }

    static class AdminUserRoleHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            int userId = intParam(p, "user_id", -1);
            String role = normalizeRoleLevel(s(p, "role_level"));
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE users SET role_level = ? WHERE user_id = ?")) {
                ps.setString(1, role);
                ps.setInt(2, userId);
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "使用者等級已更新"));
            }
        }
    }

    static class AdminRecordsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String keyword = s(p, "keyword");
            StringBuilder sql = new StringBuilder("SELECT r.record_id, u.student_no, u.name, b.title, r.borrow_date, r.due_date, r.return_date " +
                    "FROM borrow_records r JOIN users u ON r.user_id = u.user_id JOIN books b ON r.book_id = b.book_id WHERE 1=1 ");
            List<String> bind = new ArrayList<>();
            if (!keyword.isBlank()) {
                sql.append("AND (u.student_no LIKE ? OR u.name LIKE ? OR b.title LIKE ?) ");
                String k = "%" + keyword + "%";
                bind.add(k); bind.add(k); bind.add(k);
            }
            sql.append("ORDER BY r.return_date IS NULL DESC, r.borrow_date DESC");
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < bind.size(); i++) ps.setString(i + 1, bind.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append("{\"record_id\":").append(rs.getInt("record_id"))
                                .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                                .append(",\"name\":").append(quote(rs.getString("name")))
                                .append(",\"title\":").append(quote(rs.getString("title")))
                                .append(",\"borrow_date\":").append(quote(rs.getString("borrow_date")))
                                .append(",\"due_date\":").append(quote(rs.getString("due_date")))
                                .append(",\"return_date\":").append(quote(rs.getString("return_date")))
                                .append('}');
                    }
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminOverdueHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT r.record_id, u.student_no, u.name, b.title, r.borrow_date, r.due_date, DATEDIFF(NOW(), r.due_date) AS overdue_days " +
                    "FROM borrow_records r JOIN users u ON r.user_id = u.user_id JOIN books b ON r.book_id = b.book_id " +
                    "WHERE r.return_date IS NULL AND r.due_date < NOW() ORDER BY overdue_days DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    int overdue = rs.getInt("overdue_days");
                    String level = overdue >= 14 ? "嚴重逾期" : overdue >= 7 ? "警告" : "提醒";
                    json.append("{\"record_id\":").append(rs.getInt("record_id"))
                            .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                            .append(",\"name\":").append(quote(rs.getString("name")))
                            .append(",\"title\":").append(quote(rs.getString("title")))
                            .append(",\"borrow_date\":").append(quote(rs.getString("borrow_date")))
                            .append(",\"due_date\":").append(quote(rs.getString("due_date")))
                            .append(",\"overdue_days\":").append(overdue)
                            .append(",\"fine\":").append(overdue * 20)
                            .append(",\"level\":").append(quote(level))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }


    static class AdminOverdueNotifyHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT u.user_id, u.name, COUNT(*) AS cnt, MAX(DATEDIFF(NOW(), r.due_date)) AS max_days, SUM(DATEDIFF(NOW(), r.due_date) * 20) AS fine " +
                    "FROM borrow_records r JOIN users u ON r.user_id = u.user_id " +
                    "WHERE r.return_date IS NULL AND r.due_date < NOW() GROUP BY u.user_id, u.name";
            int users = 0;
            int records = 0;
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("user_id");
                    int cnt = rs.getInt("cnt");
                    int maxDays = rs.getInt("max_days");
                    int fine = rs.getInt("fine");
                    addNotification(conn, uid, "管理者逾期提醒：你目前有 " + cnt + " 本書逾期未還，最久逾期 " + maxDays + " 天，模擬罰款 NT$ " + fine + "。請盡快歸還。 ");
                    users++;
                    records += cnt;
                }
            }
            sendJson(e, 200, boolJson(true, "已寄出逾期通知給 " + users + " 位使用者，共 " + records + " 筆逾期紀錄"));
        }
    }

    static class AdminRecordsExportHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            Map<String, String> p = params(e);
            String keyword = s(p, "keyword");
            StringBuilder sql = new StringBuilder("SELECT r.record_id, u.student_no, u.name, b.title, r.borrow_date, r.due_date, r.return_date, " +
                    "CASE WHEN r.return_date IS NULL AND r.due_date < NOW() THEN DATEDIFF(NOW(), r.due_date) ELSE 0 END AS overdue_days " +
                    "FROM borrow_records r JOIN users u ON r.user_id = u.user_id JOIN books b ON r.book_id = b.book_id WHERE 1=1 ");
            List<String> bind = new ArrayList<>();
            if (!keyword.isBlank()) {
                sql.append("AND (u.student_no LIKE ? OR u.name LIKE ? OR b.title LIKE ?) ");
                String k = "%" + keyword + "%";
                bind.add(k); bind.add(k); bind.add(k);
            }
            sql.append("ORDER BY r.borrow_date DESC");
            StringBuilder csv = new StringBuilder("record_id,student_no,name,title,borrow_date,due_date,return_date,overdue_days,fine\n");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < bind.size(); i++) ps.setString(i + 1, bind.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int overdue = rs.getInt("overdue_days");
                        csv.append(rs.getInt("record_id")).append(',')
                                .append(csv(rs.getString("student_no"))).append(',')
                                .append(csv(rs.getString("name"))).append(',')
                                .append(csv(rs.getString("title"))).append(',')
                                .append(csv(rs.getString("borrow_date"))).append(',')
                                .append(csv(rs.getString("due_date"))).append(',')
                                .append(csv(rs.getString("return_date"))).append(',')
                                .append(overdue).append(',')
                                .append(overdue * 20).append('\n');
                    }
                }
            }
            sendCsv(e, "borrow_records.csv", csv.toString());
        }
    }

    static class AdminReservationsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT r.reservation_id, r.user_id, u.student_no, u.name, r.book_id, b.title, r.reserve_date, r.status " +
                    "FROM reservations r JOIN users u ON r.user_id = u.user_id JOIN books b ON r.book_id = b.book_id ORDER BY r.reserve_date DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append(reservationObject(rs));
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminReviewsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT rv.review_id, rv.rating, rv.comment, rv.created_at, u.student_no, u.name, b.title " +
                    "FROM book_reviews rv JOIN users u ON rv.user_id = u.user_id JOIN books b ON rv.book_id = b.book_id ORDER BY rv.created_at DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"review_id\":").append(rs.getInt("review_id"))
                            .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                            .append(",\"name\":").append(quote(rs.getString("name")))
                            .append(",\"title\":").append(quote(rs.getString("title")))
                            .append(",\"rating\":").append(rs.getInt("rating"))
                            .append(",\"comment\":").append(quote(rs.getString("comment")))
                            .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminReviewDeleteHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int id = intParam(params(e), "review_id", -1);
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM book_reviews WHERE review_id = ?")) {
                ps.setInt(1, id);
                sendJson(e, 200, boolJson(ps.executeUpdate() > 0, "書評已刪除"));
            }
        }
    }

    static class AdminStatsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT COALESCE(NULLIF(b.subjects,''),'未分類') AS subject, COUNT(*) AS cnt " +
                    "FROM borrow_records r JOIN books b ON r.book_id = b.book_id GROUP BY COALESCE(NULLIF(b.subjects,''),'未分類') ORDER BY cnt DESC LIMIT 10";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"subject\":").append(quote(rs.getString("subject")))
                            .append(",\"count\":").append(rs.getInt("cnt"))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }


    static class AdminRiskUsersHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT u.user_id, u.student_no, u.name, COUNT(*) AS overdue_count, " +
                    "MAX(DATEDIFF(NOW(), r.due_date)) AS max_overdue_days, " +
                    "SUM(DATEDIFF(NOW(), r.due_date) * 20) AS total_fine " +
                    "FROM borrow_records r JOIN users u ON r.user_id = u.user_id " +
                    "WHERE r.return_date IS NULL AND r.due_date < NOW() " +
                    "GROUP BY u.user_id, u.student_no, u.name ORDER BY total_fine DESC, max_overdue_days DESC LIMIT 10";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"user_id\":").append(rs.getInt("user_id"))
                            .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                            .append(",\"name\":").append(quote(rs.getString("name")))
                            .append(",\"overdue_count\":").append(rs.getInt("overdue_count"))
                            .append(",\"max_overdue_days\":").append(rs.getInt("max_overdue_days"))
                            .append(",\"total_fine\":").append(rs.getInt("total_fine"))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminInventoryAlertsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT b.book_id, b.title, b.subjects, b.status, " +
                    "(SELECT COUNT(*) FROM borrow_records r WHERE r.book_id = b.book_id) AS borrow_count, " +
                    "(SELECT COUNT(*) FROM reservations rs WHERE rs.book_id = b.book_id AND rs.status IN ('WAITING','READY')) AS reservation_count, " +
                    "(SELECT ROUND(AVG(rv.rating),1) FROM book_reviews rv WHERE rv.book_id = b.book_id) AS avg_rating " +
                    "FROM books b ORDER BY reservation_count DESC, borrow_count ASC, b.book_id ASC LIMIT 15";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    int reservationCount = rs.getInt("reservation_count");
                    int borrowCount = rs.getInt("borrow_count");
                    String suggestion = reservationCount >= 3 ? "建議增購" : (borrowCount == 0 ? "低流通觀察" : "正常追蹤");
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"book_id\":").append(rs.getInt("book_id"))
                            .append(",\"title\":").append(quote(rs.getString("title")))
                            .append(",\"subjects\":").append(quote(rs.getString("subjects")))
                            .append(",\"status\":").append(quote(rs.getString("status")))
                            .append(",\"borrow_count\":").append(borrowCount)
                            .append(",\"reservation_count\":").append(reservationCount)
                            .append(",\"avg_rating\":").append(rs.getString("avg_rating") == null ? "null" : rs.getString("avg_rating"))
                            .append(",\"suggestion\":").append(quote(suggestion))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminVipRequestsHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            String sql = "SELECT vr.request_id, vr.status, vr.message, vr.created_at, vr.handled_at, u.user_id, u.student_no, u.name, u.role_level " +
                    "FROM vip_requests vr JOIN users u ON vr.user_id = u.user_id ORDER BY vr.status = 'PENDING' DESC, vr.created_at DESC";
            StringBuilder json = new StringBuilder("[");
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"request_id\":").append(rs.getInt("request_id"))
                            .append(",\"user_id\":").append(rs.getInt("user_id"))
                            .append(",\"student_no\":").append(quote(rs.getString("student_no")))
                            .append(",\"name\":").append(quote(rs.getString("name")))
                            .append(",\"role_level\":").append(quote(rs.getString("role_level")))
                            .append(",\"status\":").append(quote(rs.getString("status")))
                            .append(",\"message\":").append(quote(rs.getString("message")))
                            .append(",\"created_at\":").append(quote(rs.getString("created_at")))
                            .append(",\"handled_at\":").append(quote(rs.getString("handled_at")))
                            .append('}');
                }
            }
            json.append(']');
            sendJson(e, 200, json.toString());
        }
    }

    static class AdminVipApproveHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int requestId = intParam(params(e), "request_id", -1);
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int userId = -1;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM vip_requests WHERE request_id = ? AND status = 'PENDING'")) {
                        ps.setInt(1, requestId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                sendJson(e, 200, boolJson(false, "找不到待審核申請"));
                                return;
                            }
                            userId = rs.getInt("user_id");
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET role_level = 'VIP' WHERE user_id = ?")) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE vip_requests SET status = 'APPROVED', handled_at = NOW() WHERE request_id = ?")) {
                        ps.setInt(1, requestId);
                        ps.executeUpdate();
                    }
                    addNotification(conn, userId, "你的 VIP 申請已通過，借閱上限與借閱天數已提升。");
                    conn.commit();
                    sendJson(e, 200, boolJson(true, "已核准 VIP 申請"));
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        }
    }

    static class AdminVipRejectHandler extends BaseHandler {
        protected void doHandle(HttpExchange e) throws Exception {
            int requestId = intParam(params(e), "request_id", -1);
            try (Connection conn = DBUtil.getConnection()) {
                int userId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM vip_requests WHERE request_id = ?")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getInt("user_id");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE vip_requests SET status = 'REJECTED', handled_at = NOW() WHERE request_id = ?")) {
                    ps.setInt(1, requestId);
                    boolean ok = ps.executeUpdate() > 0;
                    if (ok && userId > 0) addNotification(conn, userId, "你的 VIP 申請未通過，如有疑問請洽管理者。");
                    sendJson(e, 200, boolJson(ok, "已拒絕 VIP 申請"));
                }
            }
        }
    }
}
