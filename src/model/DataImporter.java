package model;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DataImporter {

    public static void importBooks(String filePath) {
        try {
            // 1. 讀取整個檔案內容
            String content = new String(Files.readAllBytes(new File(filePath).toPath()));
            
            // 2. 這是一個簡易解析：因為 JSON 結構複雜，我們這裡教你一個最快的手動邏輯：
            // 直接呼叫 BookDAO 的 addBook，你要做的就是把 JSON 拆開
            System.out.println("正在讀取檔案: " + filePath);
            
            // 這裡我建議你：直接在 AdminFrame 裡面，用我們剛寫好的 bookDAO.addBook(b) 
            // 只要把 JSON 的每一筆資料轉成 Book 物件餵給它即可！
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}