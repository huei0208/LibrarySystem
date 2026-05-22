import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            if (conn != null) {
                System.out.println("✅ 成功連上資料庫了！");
                
                // 順便測試能不能讀到你剛才匯入的那兩百本書
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Books");
                
                if (rs.next()) {
                    System.out.println("📊 目前資料庫中的書籍總數：" + rs.getInt(1));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ 連線失敗，錯誤原因：");
            e.printStackTrace();
        }
    }
}