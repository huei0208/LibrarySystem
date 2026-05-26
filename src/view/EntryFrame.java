package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EntryFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public EntryFrame() {
        setTitle("圖書館借還書系統 - 歡迎光臨");
        setSize(1000, 500); // 對齊原本登入畫面的尺寸
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 建立背景面板，使用你提供的 entryinterface.jpg
        BackgroundPanel bgPanel = new BackgroundPanel("resources/entryinterface.jpg");
        bgPanel.setLayout(null); // 使用絕對座標
        this.setContentPane(bgPanel);

        // 🔍 對位偵測器 (開發完畢可註解掉或刪除)
        bgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("點擊位置座標: x=" + e.getX() + ", y=" + e.getY());
            }
        });

        // ============================================================
        // ⚙️ 2. 上方按鈕：管理者登入通道 (對應海龜圖片)
        // ============================================================
        JButton btnAdmin = new JButton();
        // 🔍 大致對準圖片上方的藍色按鈕區域 (置中)
        btnAdmin.setBounds(350, 160, 300, 70); 
        makeButtonInvisible(btnAdmin);

        // 點擊後打開「管理員專用登入頁面」
        btnAdmin.addActionListener(e -> {
            this.dispose(); 
            new AdminLoginFrame().setVisible(true); 
        });
        bgPanel.add(btnAdmin);

        // ============================================================
        // 🚪 3. 下方按鈕：讀者登入通道 (對應章魚圖片)
        // ============================================================
        JButton btnReader = new JButton();
        // 🔍 大致對準圖片下方的藍色按鈕區域 (置中)
        btnReader.setBounds(350, 300, 300, 70); 
        makeButtonInvisible(btnReader);
        
        // 點擊後打開「讀者專用登入頁面」
        btnReader.addActionListener(e -> {
            this.dispose(); // 關閉這扇大門
            new LoginFrame().setVisible(true); // 開啟讀者登入頁
        });
        bgPanel.add(btnReader);
    }

    /**
     * 將按鈕變為隱形，但保留點擊功能與手指游標
     */
    private void makeButtonInvisible(JButton btn) {
        btn.setContentAreaFilled(false); // 去除背景色
        btn.setBorderPainted(false);     // 去除邊框
        btn.setFocusPainted(false);      // 去除焦點框
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 滑鼠移上去變「小手指」
    }

    public static void main(String[] args) {
        // 啟動最前面的分流大門
        SwingUtilities.invokeLater(() -> {
            new EntryFrame().setVisible(true);
        });
    }
}