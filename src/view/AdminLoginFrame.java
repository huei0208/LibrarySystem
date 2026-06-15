package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.AdminDAO;

public class AdminLoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField txtAdminAcc;
    private JPasswordField txtAdminPass;

    public AdminLoginFrame() {
        setTitle("🛠️ 管理者系統 - 安全驗證中心");
        setSize(1000, 500); // ✨ 與原本登入分流大門尺寸一致
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 關閉此窗不結束程式
        setLocationRelativeTo(null);

        // ============================================================
        // 🚪 1. 建立背景面板，使用你提供的 manager.jpg
        // ============================================================
        BackgroundPanel bgPanel = new BackgroundPanel("resources/manager.jpg");
        bgPanel.setLayout(null); // 使用絕對座標，精準定位輸入框與按鈕
        this.setContentPane(bgPanel);

        // 🔍 對位偵測器 (開發完畢可註解掉或刪除)
        bgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("點擊位置座標: x=" + e.getX() + ", y=" + e.getY());
            }
        });

        // ============================================================
        // 👤 2. 管理者帳號輸入框 (隱形，對位在圖片 User 框)
        // ============================================================
        txtAdminAcc = new JTextField();
        // 🔍 大致座標 [x=380, y=210, w=240, h=35] (請根據實機調整)
        txtAdminAcc.setBounds(380, 210, 240, 35); 
        
        // 🛠️ 隱形魔法：去除背景色、邊框
        txtAdminAcc.setOpaque(false); 
        txtAdminAcc.setBorder(null);  
        txtAdminAcc.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        txtAdminAcc.setForeground(Color.DARK_GRAY); // 設定字體顏色
        bgPanel.add(txtAdminAcc);

        // ============================================================
        // 🔐 3. 管理者密碼輸入框 (隱形，對位在圖片 Password 框)
        // ============================================================
        txtAdminPass = new JPasswordField();
        // 🔍 大致座標 [x=380, y=280, w=240, h=35] (在 User 框下方約 70px)
        txtAdminPass.setBounds(380, 280, 240, 35);
        
        txtAdminPass.setOpaque(false);
        txtAdminPass.setBorder(null);
        txtAdminPass.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        bgPanel.add(txtAdminPass);

        // ============================================================
        // 🚀 4. 管理者登入按鈕 (隱形，對位在圖片圓形按鈕區域)
        // ============================================================
        JButton btnLogin = new JButton();
        // 🔍 大致座標 [x=430, y=345, w=140, h=40] (在密碼框下方)
        btnLogin.setBounds(430, 345, 140, 40); 
        
        // 🛠️ 隱形按鈕魔法
        btnLogin.setContentAreaFilled(false); // 去除按鈕背景
        btnLogin.setBorderPainted(false);     // 去除邊框
        btnLogin.setFocusPainted(false);      // 去除焦點虛線
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 滑鼠移上去變「小手指」
        
        // 綁定登入邏輯
        btnLogin.addActionListener(e -> handleAdminLogin());
        bgPanel.add(btnLogin);
    }

    /**
     * 處理純管理者登入驗證邏輯
     */
    private void handleAdminLogin() {
        String inputAcc = txtAdminAcc.getText().trim();
        String inputPass = new String(txtAdminPass.getPassword()).trim();

        if (inputAcc.isEmpty() || inputPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入管理者帳號與密碼！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 🚪 這裡我們【只撈】Admins (管理員) 表格
            AdminDAO adminDAO = new AdminDAO(); 
            if (adminDAO.verifyAdmin(inputAcc, inputPass)) { 
                // 管理員驗證成功
                JOptionPane.showMessageDialog(this, "🛠️ 驗證成功！歡迎進入管理者後台！");
                this.dispose(); // 關閉管理員登入視窗
                
                // 💡 提示：請確保你的專案裡有 AdminFrame.java，否則會報錯
                new AdminFrame().setVisible(true); // 進入管理員專屬畫面
                return; 
            }

            // 如果在 Admin 表找不到人
            JOptionPane.showMessageDialog(this, "驗證失敗：管理員帳號不存在或密碼錯誤。", "驗證失敗", JOptionPane.ERROR_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "連線失敗：" + e.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
}