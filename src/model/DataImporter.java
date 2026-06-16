package model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataImporter {

    private static final String TABLE_USERS = "users";
    private static final String TABLE_BOOKS = "books";
    private static final String TABLE_RECORDS = "borrow_records";
    private static final String TABLE_REVIEWS = "book_reviews";

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("請依序輸入 Users.json、Books.json、Borrow_records.json 的路徑");
            System.out.println("範例：");
            System.out.println("java -cp \"bin;lib/*\" model.DataImporter \"C:/Users/Asus/Downloads/專題預設數據/使用者資料/Users.json\" \"C:/Users/Asus/Downloads/專題預設數據/書籍資料/Books.json\" \"C:/Users/Asus/Downloads/專題預設數據/借還紀錄資料/Borrow_records.json\"");
            return;
        }

        String usersPath = args[0];
        String booksPath = args[1];
        String recordsPath = args[2];

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            createTables(conn);
            clearOldData(conn);

            int userCount = importUsers(conn, usersPath);
            int bookCount = importBooks(conn, booksPath);
            int recordCount = importBorrowRecords(conn, recordsPath);

            updateBookStatusByBorrowRecords(conn);

            conn.commit();

            System.out.println("匯入完成！");
            System.out.println("Users 匯入：" + userCount + " 筆");
            System.out.println("Books 匯入：" + bookCount + " 筆");
            System.out.println("Borrow_records 匯入：" + recordCount + " 筆");

        } catch (Exception e) {
            System.err.println("匯入失敗！");
            e.printStackTrace();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        execute(conn, """
                CREATE TABLE IF NOT EXISTS Users (
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    student_no VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    password VARCHAR(100) NOT NULL,
                    role_level VARCHAR(20) DEFAULT 'NORMAL',
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS Books (
                    book_id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    authors TEXT,
                    subjects TEXT,
                    publisher VARCHAR(255),
                    publish_year VARCHAR(20),
                    edition VARCHAR(100),
                    format_desc VARCHAR(255),
                    source VARCHAR(255),
                    note TEXT,
                    status VARCHAR(20) DEFAULT 'AVAILABLE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS Borrow_records (
                    record_id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    book_id INT NOT NULL,
                    borrow_date DATETIME,
                    due_date DATETIME,
                    return_date DATETIME NULL,
                    borrow_days INT,
                    created_at DATETIME,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id),
                    FOREIGN KEY (book_id) REFERENCES Books(book_id)
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS book_reviews (
                    review_id INT AUTO_INCREMENT PRIMARY KEY,
                    book_id INT NOT NULL,
                    user_id INT NOT NULL,
                    rating INT DEFAULT 5,
                    comment TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (book_id) REFERENCES Books(book_id),
                    FOREIGN KEY (user_id) REFERENCES Users(user_id)
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS Admins (
                    admin_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS Reservations (
                    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    book_id INT NOT NULL,
                    reserve_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(20) DEFAULT 'WAITING',
                    FOREIGN KEY (user_id) REFERENCES Users(user_id),
                    FOREIGN KEY (book_id) REFERENCES Books(book_id)
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS Favorites (
                    user_id INT NOT NULL,
                    book_id INT NOT NULL,
                    PRIMARY KEY (user_id, book_id),
                    FOREIGN KEY (user_id) REFERENCES Users(user_id),
                    FOREIGN KEY (book_id) REFERENCES Books(book_id)
                )
                """);

        execute(conn, """
                CREATE TABLE IF NOT EXISTS notifications (
                    notification_id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    message TEXT NOT NULL,
                    is_read BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id)
                )
                """);

        execute(conn, "INSERT IGNORE INTO Admins (username, password) VALUES ('admin', '1234')");
    }

    private static void clearOldData(Connection conn) throws SQLException {
        execute(conn, "SET FOREIGN_KEY_CHECKS = 0");

        execute(conn, "TRUNCATE TABLE notifications");
        execute(conn, "TRUNCATE TABLE Favorites");
        execute(conn, "TRUNCATE TABLE Reservations");
        execute(conn, "TRUNCATE TABLE " + TABLE_REVIEWS);
        execute(conn, "TRUNCATE TABLE " + TABLE_RECORDS);
        execute(conn, "TRUNCATE TABLE " + TABLE_BOOKS);
        execute(conn, "TRUNCATE TABLE " + TABLE_USERS);

        execute(conn, "SET FOREIGN_KEY_CHECKS = 1");
    }

    private static int importUsers(Connection conn, String filePath) throws Exception {
        List<Map<String, Object>> users = readJsonObjectArray(filePath);

        String sql = "INSERT INTO Users (student_no, name, password, role_level, status) VALUES (?, ?, ?, ?, ?)";

        int count = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < users.size(); i++) {
                Map<String, Object> user = users.get(i);

                String studentNo = strAny(user, "student_no", "studentNo", "學號", "帳號", "account");
                String name = strAny(user, "name", "姓名", "使用者名稱", "real_name", "realName");
                String password = strAny(user, "password", "密碼");
                String role = strAny(user, "role_level", "role", "權限", "身分");
                String status = strAny(user, "status", "狀態");

                if (studentNo.isEmpty()) {
                    studentNo = String.format("B13505%03d", i + 1);
                }

                if (name.isEmpty()) {
                    name = "使用者" + (i + 1);
                }

                if (password.isEmpty()) {
                    password = "1234";
                }

                role = normalizeRole(role);
                status = normalizeStatus(status);

                ps.setString(1, studentNo);
                ps.setString(2, name);
                ps.setString(3, password);
                ps.setString(4, role);
                ps.setString(5, status);

                ps.addBatch();
                count++;
            }

            ps.executeBatch();
        }

        return count;
    }

    private static int importBooks(Connection conn, String filePath) throws Exception {
        List<Map<String, Object>> books = readJsonObjectArray(filePath);

        String sql = """
                INSERT INTO Books
                (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')
                """;

        int count = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map<String, Object> book : books) {
                String title = strAny(book, "題名", "title", "書名");
                String authors = strAny(book, "作者", "authors", "author");
                String subjects = strAny(book, "主題", "subjects", "subject");
                String publisher = strAny(book, "出版者", "publisher", "出版社");
                String publishYear = strAny(book, "出版年", "publish_year", "publishYear", "year");
                String edition = strAny(book, "版本", "edition");
                String formatDesc = strAny(book, "格式", "format_desc", "formatDesc");
                String source = strAny(book, "資料來源", "source");
                String isbn = strAny(book, "識別號", "isbn", "ISBN");
                String rawNote = strAny(book, "附註", "note");

                String note = buildNote(isbn, rawNote);

                if (title.isEmpty()) {
                    title = "未命名書籍";
                }

                ps.setString(1, title);
                ps.setString(2, authors);
                ps.setString(3, subjects);
                ps.setString(4, publisher);
                ps.setString(5, publishYear);
                ps.setString(6, edition);
                ps.setString(7, formatDesc);
                ps.setString(8, source);
                ps.setString(9, note);

                ps.addBatch();
                count++;
            }

            ps.executeBatch();
        }

        return count;
    }

    private static int importBorrowRecords(Connection conn, String filePath) throws Exception {
        List<Map<String, Object>> records = readJsonObjectArray(filePath);

        String sql = """
                INSERT INTO Borrow_records
                (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        int count = 0;
        LocalDateTime importTime = LocalDateTime.now();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map<String, Object> record : records) {
                int userId = intAny(record, "user_id", "userId", "使用者ID", "借閱者ID");
                int bookId = intAny(record, "book_id", "bookId", "書籍ID");
                int borrowDays = intAny(record, "borrow_days", "borrowDays", "借閱時長");

                Timestamp borrowDate = parseRelativeTimestamp(any(record, "borrow_date", "borrowDate", "借閱時間"), importTime);
                Timestamp dueDate = parseRelativeTimestamp(any(record, "due_date", "dueDate", "到期時間"), importTime);
                Timestamp returnDate = parseRelativeTimestamp(any(record, "return_date", "returnDate", "還書時間"), importTime);
                Timestamp createdAt = parseRelativeTimestamp(any(record, "created_at", "createdAt", "建立時間"), importTime);

                if (borrowDays <= 0 && borrowDate != null && dueDate != null) {
                    borrowDays = (int) ChronoUnit.DAYS.between(
                            borrowDate.toLocalDateTime().toLocalDate(),
                            dueDate.toLocalDateTime().toLocalDate()
                    );
                }

                if (borrowDays <= 0) {
                    borrowDays = 7;
                }

                if (borrowDate == null) {
                    borrowDate = Timestamp.valueOf(importTime);
                }

                if (dueDate == null) {
                    dueDate = Timestamp.valueOf(borrowDate.toLocalDateTime().plusDays(borrowDays));
                }

                if (createdAt == null) {
                    createdAt = borrowDate;
                }

                ps.setInt(1, userId);
                ps.setInt(2, bookId);
                ps.setTimestamp(3, borrowDate);
                ps.setTimestamp(4, dueDate);

                if (returnDate == null) {
                    ps.setNull(5, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(5, returnDate);
                }

                ps.setInt(6, borrowDays);
                ps.setTimestamp(7, createdAt);

                ps.addBatch();
                count++;
            }

            ps.executeBatch();
        }

        return count;
    }

    private static void updateBookStatusByBorrowRecords(Connection conn) throws SQLException {
        execute(conn, "UPDATE Books SET status = 'AVAILABLE'");

        execute(conn, """
                UPDATE Books b
                JOIN Borrow_records r ON b.book_id = r.book_id
                SET b.status = 'BORROWED'
                WHERE r.return_date IS NULL
                """);
    }

    private static String buildNote(String isbn, String rawNote) {
        StringBuilder sb = new StringBuilder();

        if (isbn != null && !isbn.isBlank()) {
            sb.append("識別號: ").append(isbn.trim());
        }

        if (rawNote != null && !rawNote.isBlank()) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(rawNote.trim());
        }

        return sb.toString();
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "NORMAL";
        }

        role = role.trim().toUpperCase();

        if (role.equals("V") || role.equals("VIP") || role.contains("貴賓")) {
            return "VIP";
        }

        return "NORMAL";
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        status = status.trim().toUpperCase();

        if (status.contains("停權") || status.equals("SUSPENDED")) {
            return "SUSPENDED";
        }

        if (status.contains("停用") || status.contains("禁用") || status.equals("DISABLED")) {
            return "DISABLED";
        }

        return "ACTIVE";
    }

    private static Timestamp parseRelativeTimestamp(Object raw, LocalDateTime baseTime) {
        if (raw == null) {
            return null;
        }

        String text = toText(raw).trim();

        if (text.isEmpty() || text.equalsIgnoreCase("null")) {
            return null;
        }

        Matcher matcher = Pattern.compile("([+-]?\\d+)\\s*days?").matcher(text);

        if (matcher.matches()) {
            int days = Integer.parseInt(matcher.group(1));
            return Timestamp.valueOf(baseTime.plusDays(days));
        }

        try {
            return Timestamp.valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(text.replace(" ", "T")));
        } catch (Exception ignored) {
        }

        try {
            return Timestamp.valueOf(text + " 00:00:00");
        } catch (Exception ignored) {
        }

        return null;
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static List<Map<String, Object>> readJsonObjectArray(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(new File(filePath).toPath()), StandardCharsets.UTF_8);
        Object parsed = new JsonParser(content).parse();

        if (!(parsed instanceof List<?> list)) {
            throw new IllegalArgumentException(filePath + " 不是 JSON 陣列格式");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }

                result.add(converted);
            }
        }

        return result;
    }

    private static Object any(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }

        return null;
    }

    private static String strAny(Map<String, Object> map, String... keys) {
        return toText(any(map, keys));
    }

    private static int intAny(Map<String, Object> map, String... keys) {
        Object value = any(map, keys);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(toText(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String toText(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>();

            for (Object item : list) {
                String text = toText(item);

                if (!text.isBlank()) {
                    parts.add(text);
                }
            }

            return String.join("、", parts);
        }

        if (value instanceof Number number) {
            double d = number.doubleValue();

            if (d == Math.floor(d)) {
                return String.valueOf(number.longValue());
            }

            return String.valueOf(d);
        }

        return String.valueOf(value);
    }

    static class JsonParser {
        private final String text;
        private int index = 0;

        JsonParser(String text) {
            this.text = text;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        private Object parseValue() {
            skipWhitespace();

            if (index >= text.length()) {
                return null;
            }

            char c = text.charAt(index);

            if (c == '{') {
                return parseObject();
            }

            if (c == '[') {
                return parseArray();
            }

            if (c == '"') {
                return parseString();
            }

            if (c == 't') {
                expect("true");
                return true;
            }

            if (c == 'f') {
                expect("false");
                return false;
            }

            if (c == 'n') {
                expect("null");
                return null;
            }

            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();

            if (peek() == '}') {
                index++;
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();

                skipWhitespace();
                expect(":");

                Object value = parseValue();
                map.put(key, value);

                skipWhitespace();
                char c = peek();

                if (c == ',') {
                    index++;
                    continue;
                }

                if (c == '}') {
                    index++;
                    break;
                }

                throw new RuntimeException("JSON 物件格式錯誤，位置：" + index);
            }

            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();

            if (peek() == ']') {
                index++;
                return list;
            }

            while (true) {
                Object value = parseValue();
                list.add(value);

                skipWhitespace();
                char c = peek();

                if (c == ',') {
                    index++;
                    continue;
                }

                if (c == ']') {
                    index++;
                    break;
                }

                throw new RuntimeException("JSON 陣列格式錯誤，位置：" + index);
            }

            return list;
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();

            if (peek() != '"') {
                throw new RuntimeException("JSON 字串格式錯誤，位置：" + index);
            }

            index++;

            while (index < text.length()) {
                char c = text.charAt(index++);

                if (c == '"') {
                    break;
                }

                if (c == '\\') {
                    if (index >= text.length()) {
                        break;
                    }

                    char next = text.charAt(index++);

                    switch (next) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = text.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }

            return sb.toString();
        }

        private Number parseNumber() {
            int start = index;

            while (index < text.length()) {
                char c = text.charAt(index);

                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    index++;
                } else {
                    break;
                }
            }

            String number = text.substring(start, index);

            if (number.contains(".") || number.contains("e") || number.contains("E")) {
                return Double.parseDouble(number);
            }

            return Long.parseLong(number);
        }

        private void expect(String expected) {
            skipWhitespace();

            if (!text.startsWith(expected, index)) {
                throw new RuntimeException("JSON 格式錯誤，預期：" + expected + "，位置：" + index);
            }

            index += expected.length();
        }

        private char peek() {
            if (index >= text.length()) {
                return '\0';
            }

            return text.charAt(index);
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}