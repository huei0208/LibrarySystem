package view;

import javax.swing.*;
import java.awt.*;

public class EntryFrame extends JFrame {

    public EntryFrame() {
        setTitle("圖書館管理系統 - 選擇身分");
       
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        
        BackgroundPanel bgPanel = new BackgroundPanel("resources/entryinterface.JPG");
        bgPanel.setLayout(null); // 使用絕對座標佈局
        this.setContentPane(bgPanel);

        // 2. 建立隱形的「使用者登入」按鈕 (疊在圖片左側波浪上)
        JButton btnUserLogin = new JButton();
        // 關鍵設定：讓按鈕完全隱形，不擋住圖片
        btnUserLogin.setContentAreaFilled(false); // 不顯示按鈕背景色
        btnUserLogin.setBorderPainted(false);     // 不顯示按鈕邊框
        btnUserLogin.setFocusPainted(false);      // 點擊時不出現虛線框

        // 【對位重點】setBounds(x, y, 寬, 高)
        // 這邊的數字是範例，請依據你在圖上點擊到的座標微調
        btnUserLogin.setBounds(327, 344, 336, 78); 
        bgPanel.add(btnUserLogin);

        // 3. 建立隱形的「管理者登入」按鈕 (疊在圖片右側註冊按鈕附近或預定位置)
        // (假設你圖片右邊預留了管理者登入的位置)
        JButton btnAdminLogin = new JButton();
        btnAdminLogin.setContentAreaFilled(false);
        btnAdminLogin.setBorderPainted(false);
        btnAdminLogin.setFocusPainted(false);

        btnAdminLogin.setBounds(326, 167, 336, 78); // 請用座標偵測器找出正確位置
        bgPanel.add(btnAdminLogin);

       

        // --- 按鈕邏輯 ---
        btnUserLogin.addActionListener(e -> {
            new LoginFrame().setVisible(true); // 跳轉到學號輸入頁面
            this.dispose();
        });

        btnAdminLogin.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "管理者系統(未實作)");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EntryFrame().setVisible(true);
        });
    }
}