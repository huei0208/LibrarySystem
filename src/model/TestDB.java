package model; // 這一行告訴 Java：「我屬於 model 這個家庭」

import java.sql.*; // 因為它是 Java 內建的，所以還是要 import

public class TestDB {
    public static void main(String[] args) {
        // 因為在同一個 package，可以直接用 DBUtil，不用 import
        try (Connection conn = DBUtil.getConnection()) { 
            if (conn != null) {
                System.out.println("✅ 連線成功！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}