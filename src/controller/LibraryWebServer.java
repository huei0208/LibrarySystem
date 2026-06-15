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
    // 🚚 專門負責「驗證帳號密碼」的登入外送員
    // =========================================================
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            // 取得網頁傳來的網址參數 (例如：?username=abc&password=123)
            String query = t.getRequestURI().getQuery();
            boolean success = false;
            
            if (query != null && query.contains("username=") && query.contains("password=")) {
                // 超簡易的參數切割法 (為了加速開發，我們用最單純的字串處理)
                String username = query.split("username=")[1].split("&")[0];
                String password = query.split("password=")[1];
                
                // 💡 【核心接軌區】這裡要接上你原本寫好的大腦！
                // 如果你有 UserDAO，請把下面這段解開註解：
                // model.UserDAO userDAO = new model.UserDAO();
                // success = userDAO.validateLogin(username, password); 
                
                // [測試用] 在你接上 UserDAO 之前，我們先寫死一組密碼讓你測試網頁會不會動：
                if (username.equals("admin") && password.equals("1234")) {
                    success = true;
                }
            }

            // 把結果 (true 或 false) 包裝成 JSON 格式回傳給網頁
            String jsonResponse = "{\"success\": " + success + "}";
            
            t.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            byte[] response = jsonResponse.getBytes("UTF-8");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}