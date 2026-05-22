import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import model.DBUtil;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            if (conn != null) {
                System.out.println("成功連上資料庫了");

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Books");

                if (rs.next()) {
                    System.out.println("Books 資料筆數：" + rs.getInt(1));
                }
            }
        } catch (Exception e) {
            System.out.println("資料庫連線失敗");
            e.printStackTrace();
        }
    }
}
