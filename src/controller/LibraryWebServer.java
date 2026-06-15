package controller; // 記得確認你的 package 名稱，如果有紅線就刪掉或改成對的

import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import model.BookDAO; // 💡 確保有 import 你的神級大腦

public class LibraryWebServer {

    public static void main(String[] args) throws Exception {
        // 1. 建立伺服器，監聽 8080 號通訊埠
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 2. 設定一個網址路徑，當網頁呼叫 /api/records 時，就觸發 RecordHandler 去拿資料
        server.createContext("/api/records", new RecordHandler());
        server.createContext("/api/login", new LoginHandler()); // ✨ 登入專用外送員
        server.createContext("/api/register", new RegisterHandler()); // ✨ 這是註冊的專屬櫃台
        server.createContext("/api/books", new BookHandler()); // ✨ 負責送書的外送員
        server.createContext("/api/myBorrowRecords", new BorrowHandler()); // ✨ 個人借閱紀錄外送員
        server.createContext("/api/myReservations", new MyReservationHandler()); // ✨ 個人預約紀錄外送員
        server.createContext("/api/overdue", new OverdueHandler()); // ✨ 新增這條給逾期查詢
        server.createContext("/api/borrow", new BorrowHandler()); // 📤 新增：借書專屬櫃台

        // 3. 啟動伺服器！
        server.setExecutor(null); 
        server.start();
        System.out.println("🚀 海底圖書館 網頁伺服器已啟動！");
        System.out.println("👉 請打開瀏覽器，測試輸入網址： http://localhost:8080/api/records");
    }

    // =========================================================
    // 🚚 專門負責運送「借還紀錄」的外送員
    // =========================================================
    static class RecordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // [設定檔] 解決 CORS 跨網域問題 (很重要！不然前端網頁會被瀏覽器擋住)
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            // ✨ 1. 呼叫你寫好的神級大腦！直接拿全校借還紀錄
            BookDAO dao = new BookDAO();
            List<Object[]> records = dao.getAllSystemBorrowRecords("");

            // ✨ 2. 把 Java 的 List 轉換成網頁看得懂的 JSON 文字格式
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < records.size(); i++) {
                Object[] row = records.get(i);
                
                // 根據我們之前寫好的邏輯，row 裡面有 6 個欄位：
                // [學號, 姓名, 書名, 借出時間, 到期時間, 歸還時間]
                json.append("  {\n");
                json.append("    \"student_no\": \"").append(row[0]).append("\",\n");
                json.append("    \"name\": \"").append(row[1]).append("\",\n");
                json.append("    \"title\": \"").append(row[2]).append("\",\n");
                json.append("    \"borrow_date\": \"").append(row[3]).append("\",\n");
                
                // 處理尚未歸還的 NULL 顯示
                String returnDate = row[5] == null ? "尚未歸還" : row[5].toString();
                json.append("    \"return_date\": \"").append(returnDate).append("\"\n");
                
                json.append("  }");
                
                // 如果不是最後一筆，就要加逗號
                if (i < records.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");

            // ✨ 3. 把打包好的 JSON 送出給網頁！
            byte[] response = json.toString().getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
// =========================================================
    // 🚚 專門負責「驗證登入」的警衛 (✅ 已無縫對接你的 UserDAO 大腦)
    // =========================================================
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            boolean success = false;
            String name = "讀者";
            String roleFromDB = "一般會員";
            int borrowDays = 7; 
            String studentNo = "";

            if (query != null && query.contains("username=") && query.contains("password=")) {
                studentNo = query.split("username=")[1].split("&")[0];
                String p = query.split("password=")[1].split("&")[0];
                
                String clientRole = "user"; 
                if (query.contains("role=")) {
                    clientRole = query.split("role=")[1].split("&")[0];
                }

                try {
                    // ✨ 核心升級：直接呼叫你寫好的 UserDAO！完全不碰 SQL！
                    model.UserDAO userDAO = new model.UserDAO();
                    model.User user = userDAO.login(studentNo, p); // 👈 你的舊大腦在這裡發揮作用了！

                    // 如果回傳的 user 不是 null，代表帳密正確！
                    if (user != null) {
                        // 從你的 User 物件中拿出姓名與權限
                        name = user.getName() != null ? user.getName() : studentNo;
                        String dbRoleLevel = user.getRoleLevel() != null ? user.getRoleLevel().toString().toUpperCase() : "USER";

                        // 🛡️ 身分安全防線比對 (邏輯與之前相同，但寫法乾淨 10 倍)
                        if (clientRole.equals("admin")) {
                            if (dbRoleLevel.contains("ADMIN") || dbRoleLevel.contains("管理")) {
                                success = true;
                                roleFromDB = "系統管理員";
                                borrowDays = 999;
                            } else {
                                System.out.println("🚨 警告：[" + studentNo + "] 試圖越權登入管理台被攔截！");
                            }
                        } else {
                            success = true; 
                            if (dbRoleLevel.contains("ADMIN") || dbRoleLevel.contains("管理")) {
                                roleFromDB = "系統管理員";
                                borrowDays = 999;
                            } else if (dbRoleLevel.contains("VIP")) {
                                roleFromDB = "VIP 會員";
                                borrowDays = 14; 
                            } else {
                                roleFromDB = "一般會員";
                                borrowDays = 7; 
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("登入呼叫過程發生錯誤：" + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 打包傳回前端
            String jsonResponse = String.format(
                "{\"success\": %b, \"studentNo\": \"%s\", \"name\": \"%s\", \"role\": \"%s\", \"borrowDays\": %d}",
                success, studentNo, name, roleFromDB, borrowDays
            );
            
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    // =========================================================
    // 🚚 專門負責「處理註冊請求」的外送員 (已加入帳號重複檢查 🛡️)
    // =========================================================
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            boolean success = false;
            String message = "註冊初始化中";

            if (query != null && query.contains("studentNo=")) {
                String studentNo = "";
                String name = "";
                String password = "";
                
                try {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("studentNo=")) studentNo = param.split("=")[1];
                        if (param.startsWith("name=")) name = java.net.URLDecoder.decode(param.split("=")[1], "UTF-8");
                        if (param.startsWith("password=")) password = param.split("=")[1];
                    }

                    try (java.sql.Connection conn = model.DBUtil.getConnection()) {
                        // 1. 檢查帳號
                        String checkSql = "SELECT student_no FROM Users WHERE student_no = ?";
                        try (java.sql.PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                            checkPstmt.setString(1, studentNo);
                            java.sql.ResultSet rs = checkPstmt.executeQuery();
                            
                            if (rs.next()) {
                                message = "此學號已被註冊！";
                                success = false; // 明確標記失敗
                            } else {
                                // 2. 執行寫入
                                String insertSql = "INSERT INTO Users (student_no, name, password, role_level) VALUES (?, ?, ?, '一般會員')";
                                try (java.sql.PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                                    insertPstmt.setString(1, studentNo);
                                    insertPstmt.setString(2, name);
                                    insertPstmt.setString(3, password);
                                    insertPstmt.executeUpdate();
                                    
                                    success = true;
                                    message = "註冊成功！";
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    message = "資料庫錯誤：" + e.getMessage();
                    success = false;
                }
            } else {
                message = "請求參數錯誤";
            }

            String jsonResponse = String.format("{\"success\": %b, \"message\": \"%s\", \"debug\": \"到達後端完成處理\"}", success, message);
            System.out.println("準備回傳給前端的 JSON: " + jsonResponse); // ✨ 這行非常重要！
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    // =========================================================
    // 🚚 專門負責「展示圖書館所有藏書」的外送員 (✅ 已補上 status 與 book_id)
    // =========================================================
    static class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            model.BookDAO bookDAO = new model.BookDAO();
            java.util.List<model.Book> books = bookDAO.getAllBooks(); 

            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < books.size(); i++) {
                model.Book b = books.get(i);
                
                // 🛡️ 確保 status 是字串，且如果為 null 預設為 "未知"
                String status = (b.getStatus() != null) ? b.getStatus().toString() : "未知";
                
                json.append("  {\n");
                json.append("    \"book_id\": \"").append(b.getBookId()).append("\",\n");
                json.append("    \"title\": \"").append(b.getTitle() != null ? b.getTitle().replace("\"", "\\\"") : "未知").append("\",\n");
                json.append("    \"author\": \"").append(b.getAuthors() != null ? b.getAuthors().replace("\"", "\\\"") : "未知").append("\",\n");
                json.append("    \"subject\": \"").append(b.getSubjects() != null ? b.getSubjects().replace("\"", "\\\"") : "未分類").append("\",\n");
                json.append("    \"status\": \"").append(status).append("\"\n"); // 👈 這邊直接輸出純文字狀態
                json.append("  }");
                
                if (i < books.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");

            byte[] response = json.toString().getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    // =========================================================
    // 🚚 專門負責「借書審核」的外送員 (✅ 已無縫對接 BookDAO 大腦)
    // =========================================================
    static class BorrowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            boolean success = false;
            String message = "系統錯誤";

            if (query != null && query.contains("bookId=") && query.contains("studentNo=")) {
                try {
                    // 解析參數
                    int bookId = Integer.parseInt(query.split("bookId=")[1].split("&")[0]);
                    String studentNo = query.split("studentNo=")[1].split("&")[0];

                    // ✨ 核心升級：直接呼叫 BookDAO 大腦！
                    model.BookDAO bookDAO = new model.BookDAO();
                    String result = bookDAO.borrowBook(studentNo, bookId);

                    // 判斷 DAO 大腦傳回來的字串
                    if (result.startsWith("成功")) {
                        success = true;
                        String days = result.split("：")[1]; // 抽出借閱天數
                        message = "借閱成功！請記得在 " + days + " 天內歸還喔！";
                    } else {
                        success = false;
                        message = result; // 把失敗原因 (如逾期、上限) 原封不動傳回前端
                    }

                } catch (NumberFormatException e) {
                    message = "書籍 ID 格式錯誤！";
                } catch (Exception e) {
                    message = "執行借書失敗: " + e.getMessage();
                    e.printStackTrace();
                }
            } else {
                message = "缺少必要的借閱參數！";
            }

            // 打包傳回前端
            String jsonResponse = String.format("{\"success\": %b, \"message\": \"%s\"}", success, message);
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // =========================================================
    // 🚚 專門負責運送「個人預約紀錄」的外送員 (已串接真實資料庫)
    // =========================================================
    static class MyReservationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            int userId = 1; 
            if (query != null && query.contains("studentNo=")) {
                String studentNo = query.split("studentNo=")[1].split("&")[0];
                if (studentNo.equals("b13505054")) { userId = 2; }
                else if (studentNo.equals("D99887766")) { userId = 3; }
            }

            model.BookDAO dao = new model.BookDAO();
            // 呼叫你的大腦！
            List<Object[]> reservations = dao.getUserReservations(userId);

            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < reservations.size(); i++) {
                Object[] row = reservations.get(i);
                // 你原本的 DAO 回傳: {書名, 預約時間, 狀態}
                
                String title = row[0] != null ? row[0].toString().replace("\"", "\\\"") : "未知";
                String reserveDate = row[1] != null ? row[1].toString() : "未知";
                String status = row[2] != null ? row[2].toString() : "未知狀態";
                
                json.append("  {\n");
                json.append("    \"title\": \"").append(title).append("\",\n");
                json.append("    \"reserve_date\": \"").append(reserveDate).append("\",\n");
                json.append("    \"queue_no\": \"排隊中\",\n"); // 你的DAO目前沒算名次，先顯示排隊中
                json.append("    \"status\": \"").append(status).append("\"\n");
                json.append("  }");
                if (i < reservations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");

            byte[] response = json.toString().getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // =========================================================
    // 🚚 專門負責運送「逾期警告」的外送員 (已串接真實資料庫)
    // =========================================================
    static class OverdueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            int userId = 1; 
            if (query != null && query.contains("studentNo=")) {
                String studentNo = query.split("studentNo=")[1].split("&")[0];
                if (studentNo.equals("b13505054")) { userId = 2; }
                else if (studentNo.equals("D99887766")) { userId = 3; }
            }

            model.BookDAO dao = new model.BookDAO();
            // 呼叫你的大腦！抓取 3 天內到期的書
            List<String> warnings = dao.getExpiringBooks(userId, 3);

            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < warnings.size(); i++) {
                String warningMsg = warnings.get(i).replace("\"", "\\\"");
                json.append("  \"").append(warningMsg).append("\"");
                if (i < warnings.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");

            byte[] response = json.toString().getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}