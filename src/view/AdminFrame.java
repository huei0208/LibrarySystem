package view;
import javax.swing.*;

public class AdminFrame extends JFrame {
    public AdminFrame() {
        setTitle("圖書館管理系統 - 管理員專區");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 先加個文字標籤測試是否跳轉成功
        add(new JLabel("歡迎回來，管理員！", SwingConstants.CENTER));
    }
}