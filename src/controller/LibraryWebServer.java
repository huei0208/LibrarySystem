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
        // ✨ 向伺服器註冊這兩個全新的點餐櫃台
        server.createContext("/api/myFavorites", new MyFavoritesHandler());
        server.createContext("/api/bookHistory", new BookHistoryHandler());
        server.createContext("/api/bookReviews", new BookReviewsHandler());
        server.createContext("/api/toggleFavorite", new ToggleFavoriteHandler());
        // ✨ 向伺服器開通還書的兩個專屬服務櫃台
        server.createContext("/api/myBorrowedBooks", new MyBorrowedBooksHandler());
        server.createContext("/api/returnBook", new ReturnBookHandler());
        server.createContext("/api/reserve", new ReserveHandler());

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
    // 🚚 櫃台：專門負責「辦理借書」的外送員 (/api/borrow)
    // =========================================================
    static class BorrowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            String query = t.getRequestURI().getQuery();
            String studentNo = "";
            int bookId = -1;

            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("studentNo=")) {
                        studentNo = param.split("=")[1];
                    } else if (param.startsWith("bookId=")) {
                        try { bookId = Integer.parseInt(param.split("=")[1]); } catch (Exception e) {}
                    }
                }
            }

            String message = "系統發生未知錯誤";
            
            try {
                if (!studentNo.isEmpty() && bookId != -1) {
                    model.BookDAO bookDAO = new model.BookDAO();
                    // 呼叫大腦借書邏輯
                    String result = bookDAO.borrowBook(studentNo, bookId);
                    
                    // 🛡️ 終極無敵判斷法：不管有沒有隱形空白，只要包含「成功」就放行！
                    if (result != null && result.trim().contains("成功")) {
                        // 聰明提取：只把回傳字串裡的「數字」抓出來 (例如把 "成功：7" 變成 "7")
                        String days = result.replaceAll("[^0-9]", "");
                        if (days.isEmpty()) days = "7"; // 預設給個防呆值
                        
                        message = "借閱成功！🎉 系統已為您辦理，請於 " + days + " 天內歸還喔 🐳";
                    } else {
                        message = "❌ 借閱失敗：" + result; 
                    }
                } else {
                    message = "❌ 借閱失敗：學號或書籍ID遺失！";
                }
            } catch (Exception e) {
                e.printStackTrace();
                message = "伺服器處理借書時發生例外錯誤！";
            }

            // 打包成 JSON 送回前端
            String jsonResponse = "{\"message\": \"" + message + "\"}";
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            java.io.OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // =========================================================
    // 🚚 櫃台 A：專門負責「拉取個人收藏書籍」的外送員 (/api/myFavorites)
    // =========================================================
    static class MyFavoritesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            String studentNo = "";
            if (query != null && query.contains("studentNo=")) {
                studentNo = query.split("studentNo=")[1].split("&")[0];
            }

            model.BookDAO bookDAO = new model.BookDAO();
            
            // 💡 智慧分流：因為 getFavoriteBooks 需要傳入資料庫內部的 int userId，
            // 我們先透過學號 studentNo 來找出真實的 user_id
            int userId = -1;
            try (java.sql.Connection conn = model.DBUtil.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM Users WHERE student_no = ?")) {
                pstmt.setString(1, studentNo);
                java.sql.ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 呼叫大腦撈取收藏
            java.util.List<model.Book> favBooks = new java.util.ArrayList<>();
            if (userId != -1) {
                favBooks = bookDAO.getFavoriteBooks(userId);
            }

            // 打包成與 BookHandler 完全一致的 JSON 規格，這樣前端渲染才不會當機！
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < favBooks.size(); i++) {
                model.Book b = favBooks.get(i);
                String status = (b.getStatus() != null) ? b.getStatus().toString() : "在庫";
                
                json.append("  {\n");
                json.append("    \"book_id\": \"").append(b.getBookId()).append("\",\n");
                json.append("    \"title\": \"").append(b.getTitle() != null ? b.getTitle().replace("\"", "\\\"") : "未知").append("\",\n");
                json.append("    \"author\": \"").append(b.getAuthors() != null ? b.getAuthors().replace("\"", "\\\"") : "未知").append("\",\n");
                json.append("    \"subject\": \"").append(b.getSubjects() != null ? b.getSubjects().replace("\"", "\\\"") : "未分類").append("\",\n");
                json.append("    \"status\": \"").append(status).append("\"\n");
                json.append("  }");
                if (i < favBooks.size() - 1) json.append(",");
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
    // 🚚 櫃台 B：專門負責「拉取單一書籍歷史紀錄」的外送員 (/api/bookHistory)
    // =========================================================
    static class BookHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            int bookId = -1;
            if (query != null && query.contains("bookId=")) {
                try {
                    bookId = Integer.parseInt(query.split("bookId=")[1].split("&")[0]);
                } catch (Exception e) {
                    bookId = -1;
                }
            }

            model.BookDAO bookDAO = new model.BookDAO();
            // 呼叫你的強大大腦：回傳格式是 List<Object[]> -> [姓名, 借出時間, 歸還時間]
            java.util.List<Object[]> history = bookDAO.getBookBorrowHistory(bookId);

            // 將 List<Object[]> 打包成 JSON 雙維度陣列格式 [[欄位1, 欄位2], [欄位1, 欄位2]]
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < history.size(); i++) {
                Object[] row = history.get(i);
                String name = row[0] != null ? row[0].toString() : "神祕讀者";
                String borrowDate = row[1] != null ? row[1].toString() : "-";
                String returnDate = row[2] != null ? row[2].toString() : ""; // 未歸還就直接傳空字串，前端會判斷

                json.append("  [\"").append(name).append("\", \"").append(borrowDate).append("\", \"").append(returnDate).append("\"]");
                if (i < history.size() - 1) json.append(",");
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
    // 🚚 櫃台：專門負責「拉取單一書籍所有書評」的外送員 (/api/bookReviews)
    // =========================================================
    static class BookReviewsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // 🛡️ 1. 核心關鍵：不論成功或失敗，這兩行一定要最先執行，強灌許可證！
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            int bookId = -1;
            if (query != null && query.contains("bookId=")) {
                try { 
                    bookId = Integer.parseInt(query.split("bookId=")[1].split("&")[0]); 
                } catch (Exception e) {
                    bookId = -1;
                }
            }

            StringBuilder json = new StringBuilder();
            try {
                // 完美對接：改用你現成的 ReviewDAO 
                model.ReviewDAO reviewDAO = new model.ReviewDAO();
                // 回傳格式：{留言者姓名, 星星字串, 評論內容, 留言時間}
                java.util.List<Object[]> reviews = reviewDAO.getReviewsByBook(bookId);

                json.append("[\n");
                for (int i = 0; i < reviews.size(); i++) {
                    Object[] row = reviews.get(i);
                    
                    String name = row[0] != null ? row[0].toString() : "匿名";
                    String stars = row[1] != null ? row[1].toString() : "";
                    // 處理評論內容的換行與引號，防範 JSON 格式崩潰
                    String comment = row[2] != null ? row[2].toString().replace("\"", "\\\"").replace("\n", "\\n") : "";
                    String date = row[3] != null ? row[3].toString() : "";

                    json.append("  [\"").append(name).append("\", \"").append(stars).append("\", \"").append(comment).append("\", \"").append(date).append("\"]");
                    if (i < reviews.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
            } catch (Exception e) {
                // 🚨 如果 DAO 的 SQL 出錯，至少回傳空陣列，才不會讓前端直接斷線掛掉
                e.printStackTrace();
                json.setLength(0);
                json.append("[]");
            }

            // 送出包裹！
            byte[] response = json.toString().getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    // =========================================================
    // 🚚 櫃台 C：專門負責「拉取個人借閱中清單」的外送員 (/api/myBorrowedBooks)
    // =========================================================
    static class MyBorrowedBooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String query = t.getRequestURI().getQuery();
            String studentNo = "";
            if (query != null && query.contains("studentNo=")) {
                studentNo = query.split("studentNo=")[1].split("&")[0];
            }

            // 1. 先用學號找到 user_id
            int userId = -1;
            try (java.sql.Connection conn = model.DBUtil.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM Users WHERE student_no = ?")) {
                pstmt.setString(1, studentNo);
                java.sql.ResultSet rs = pstmt.executeQuery();
                if (rs.next()) userId = rs.getInt("user_id");
            } catch (Exception e) { e.printStackTrace(); }

            model.BookDAO bookDAO = new model.BookDAO();
            // 2. 呼叫大腦：撈出未歸還的書籍 [book_id, title, borrow_date, due_date, record_id]
            java.util.List<Object[]> borrowedList = new java.util.ArrayList<>();
            if (userId != -1) {
                borrowedList = bookDAO.getBorrowedBooks(userId);
            }

            // 3. 打包成 JSON 陣列送回前端
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < borrowedList.size(); i++) {
                Object[] row = borrowedList.get(i);
                json.append("  {\n");
                json.append("    \"book_id\": \"").append(row[0]).append("\",\n");
                json.append("    \"title\": \"").append(row[1].toString().replace("\"", "\\\"")).append("\",\n");
                json.append("    \"borrow_date\": \"").append(row[2]).append("\",\n");
                json.append("    \"due_date\": \"").append(row[3]).append("\",\n");
                json.append("    \"record_id\": \"").append(row[4]).append("\"\n");
                json.append("  }");
                if (i < borrowedList.size() - 1) json.append(",");
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
    // 🚚 櫃台 D：專門負責「辦理還書與遞補預約」的外送員 (/api/returnBook)
    // =========================================================
    static class ReturnBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            String query = t.getRequestURI().getQuery();
            int recordId = -1;
            int bookId = -1;
            String studentNo = "";

            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("recordId=")) recordId = Integer.parseInt(param.split("=")[1]);
                    else if (param.startsWith("bookId=")) bookId = Integer.parseInt(param.split("=")[1]);
                    else if (param.startsWith("studentNo=")) studentNo = param.split("=")[1];
                }
            }

            boolean success = false;
            String msg = "還書手續失敗";

            if (recordId != -1 && bookId != -1 && !studentNo.isEmpty()) {
                // 1. 找 user_id
                int userId = -1;
                try (java.sql.Connection conn = model.DBUtil.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM Users WHERE student_no = ?")) {
                    pstmt.setString(1, studentNo);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) userId = rs.getInt("user_id");
                } catch (Exception e) { e.printStackTrace(); }

                if (userId != -1) {
                    model.BookDAO bookDAO = new model.BookDAO();
                    // 2. 呼叫核心大腦執行交易！
                    String result = bookDAO.returnBookAndNotify(recordId, bookId, userId);
                    
                    if (!"ERROR".equals(result)) {
                        success = true;
                        if (result != null) {
                            // 預約排隊系統成功遞補！
                            msg = "還書成功！🐳\\n✨ 本書熱門！已自動發信通知下一位預約排隊者：[" + result + "] 同學到館借閱！";
                        } else {
                            msg = "還書手續已成功辦理！書本已歸回人魚機關庫 🫧";
                        }
                    }
                }
            }

            String jsonResponse = "{\"success\": " + success + ", \"message\": \"" + msg + "\"}";
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
    // =========================================================
    // 🚚 櫃台：專門負責「加入 / 取消收藏」的外送員 (/api/toggleFavorite)
    // =========================================================
    static class ToggleFavoriteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // 🛡️ 處理 CORS 許可證 (包含 POST 請求的預檢)
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            String query = t.getRequestURI().getQuery();
            String studentNo = "";
            int bookId = -1;

            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("studentNo=")) {
                        studentNo = param.split("=")[1];
                    } else if (param.startsWith("bookId=")) {
                        try { bookId = Integer.parseInt(param.split("=")[1]); } catch (Exception e) {}
                    }
                }
            }

            String message = "操作失敗";
            boolean success = false;

            if (!studentNo.isEmpty() && bookId != -1) {
                int userId = -1;
                // 1. 透過學號找到真實的 userId
                try (java.sql.Connection conn = model.DBUtil.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM Users WHERE student_no = ?")) {
                    pstmt.setString(1, studentNo);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        userId = rs.getInt("user_id");
                    }
                } catch (Exception e) { e.printStackTrace(); }

                if (userId != -1) {
                    model.BookDAO bookDAO = new model.BookDAO();
                    boolean isFavorited = false;
                    
                    // 2. 檢查是否已經收藏過這本書
                    try (java.sql.Connection conn = model.DBUtil.getConnection();
                         java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM Favorites WHERE user_id = ? AND book_id = ?")) {
                        pstmt.setInt(1, userId);
                        pstmt.setInt(2, bookId);
                        java.sql.ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            isFavorited = true; // 已經收藏過了！
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    // 3. 智慧切換：有收藏就刪除，沒收藏就加入
                    if (isFavorited) {
                        success = bookDAO.deleteFavorite(userId, bookId);
                        message = success ? "已為您取消收藏 💔" : "取消收藏失敗";
                    } else {
                        success = bookDAO.addFavorite(userId, bookId);
                        message = success ? "成功加入「我的收藏」 ❤️" : "加入收藏失敗";
                    }
                } else {
                    message = "找不到使用者帳號！";
                }
            }

            // 打包結果送回前端
            String jsonResponse = "{\"success\": " + success + ", \"message\": \"" + message + "\"}";
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            java.io.OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    // =========================================================
    // 🚚 櫃台：專門負責「預約排隊」的外送員 (/api/reserve)
    // =========================================================
    static class ReserveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // 🛡️ 照慣例：加上最強的 CORS 許可證防護罩
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            String query = t.getRequestURI().getQuery();
            String studentNo = "";
            int bookId = -1;

            // 解析網址傳來的學號與書籍ID
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("studentNo=")) studentNo = param.split("=")[1];
                    else if (param.startsWith("bookId=")) {
                        try { bookId = Integer.parseInt(param.split("=")[1]); } catch(Exception e) {}
                    }
                }
            }

            String message = "預約失敗：未知錯誤";
            
            try {
                if (!studentNo.isEmpty() && bookId != -1) {
                    // 1. 透過學號查出 user_id
                    int userId = -1;
                    try (java.sql.Connection conn = model.DBUtil.getConnection();
                         java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM Users WHERE student_no = ?")) {
                        pstmt.setString(1, studentNo);
                        java.sql.ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) userId = rs.getInt("user_id");
                    }

                    if (userId != -1) {
                        try (java.sql.Connection conn = model.DBUtil.getConnection()) {
                            // 2. 檢查是不是已經預約過這本書了 (防止重複排隊)
                            boolean alreadyReserved = false;
                            String checkSql = "SELECT 1 FROM Reservations WHERE user_id = ? AND book_id = ? AND status = 'PENDING'";
                            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                                pstmt.setInt(1, userId);
                                pstmt.setInt(2, bookId);
                                java.sql.ResultSet rs = pstmt.executeQuery();
                                if (rs.next()) alreadyReserved = true;
                            }

                            if (alreadyReserved) {
                                message = "您已經在這本書的排隊名單中囉！請耐心等候 🫧";
                            } else {
                                // 3. 正式寫入預約紀錄表
                                String insertSql = "INSERT INTO Reservations (user_id, book_id, reservation_date, status) VALUES (?, ?, NOW(), 'PENDING')";
                                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                                    pstmt.setInt(1, userId);
                                    pstmt.setInt(2, bookId);
                                    int rows = pstmt.executeUpdate();
                                    if (rows > 0) {
                                        message = "預約成功！🎉 當書本歸還時，系統會優先保留給您！";
                                    }
                                }
                            }
                        }
                    } else {
                        message = "❌ 預約失敗：找不到該使用者帳號！";
                    }
                } else {
                    message = "❌ 預約失敗：傳遞參數有誤！";
                }
            } catch (Exception e) {
                e.printStackTrace();
                message = "預約失敗：資料庫發生異常 (" + e.getMessage() + ")";
            }

            // 打包成 JSON 送回前端
            String jsonResponse = "{\"message\": \"" + message + "\"}";
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            java.io.OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}