package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.User;
import model.UserDAO; // 引入 DAO
import exception.*;

public class LoginFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private UserDAO userDAO = new UserDAO(); // 建立資料存取物件

    public LoginFrame() {
        setTitle("圖書館借還書系統 - 登入");
        setSize(1000, 500); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 建立背景面板
        BackgroundPanel bgPanel = new BackgroundPanel("resources/userlogin.jpg");
        bgPanel.setLayout(null);
        this.setContentPane(bgPanel);

        // 2. 對位偵測器 (開發用)
        bgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("點擊位置座標: x=" + e.getX() + ", y=" + e.getY());
            }
        });

        // 3. 使用者名稱輸入框 (學號)
        txtUser = new JTextField();
        txtUser.setBounds(395, 246, 214, 26); 
        txtUser.setOpaque(false); 
        txtUser.setBorder(null);  
        txtUser.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
        bgPanel.add(txtUser);

        // 4. 密碼輸入框
        txtPass = new JPasswordField();
        txtPass.setBounds(395, 304, 214, 26);
        txtPass.setOpaque(false);
        txtPass.setBorder(null);
        bgPanel.add(txtPass);

        // 5. 登入按鈕 (隱形)
        JButton btnLogin = new JButton();
        btnLogin.setBounds(390, 354, 214, 26);
        btnLogin.setContentAreaFilled(false); 
        btnLogin.setBorderPainted(false);     
        bgPanel.add(btnLogin);

        // 6. 註冊按鈕 (隱形)
        JButton btnRegister = new JButton();
        btnRegister.setBounds(485, 402, 30, 18); 
        btnRegister.setContentAreaFilled(false);
        btnRegister.setBorderPainted(false);
        bgPanel.add(btnRegister);

        // 7. 動作監聽
        btnLogin.addActionListener(e -> handleLogin());
        btnRegister.addActionListener(e -> new RegisterFrame().setVisible(true));
    }

    private void handleLogin() {
        String inputNo = txtUser.getText().trim();
        String inputPass = new String(txtPass.getPassword()).trim();

        if (inputNo.isEmpty() || inputPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入帳號與密碼！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // --- 核心改動：直接從資料庫找人 ---
            User targetUser = userDAO.login(inputNo, inputPass);

            if (targetUser == null) {
                // 如果找不到人或密碼錯，DAO 會回傳 null
                throw new ResourceNotFoundException("登入失敗：帳號不存在或密碼錯誤。");
            }

            // 檢查狀態 (SUSPENDED 或 DISABLED)
            if (targetUser.getStatus() == User.Status.SUSPENDED || 
                targetUser.getStatus() == User.Status.DISABLED) { 
                throw new BorrowingRuleException("登入失敗：使用者 " + targetUser.getName() + " 已被停權。");
            }

            // 登入成功
            JOptionPane.showMessageDialog(this, "🎉 登入成功！歡迎回來 " + targetUser.getName());
            
            String roleStr = targetUser.getRoleLevel().toString(); 
            int userId = targetUser.getUserId(); 
            
            this.dispose(); // 關閉登入視窗
            
            // --- 修改後的登入跳轉邏輯 ---
            this.dispose(); // 先關閉登入視窗

            // 判斷權限等級 (假設 Admin 的 RoleLevel 為 "ADMIN" 或對應的數字)
            if ("ADMIN".equals(roleStr)) { 
                // 如果是管理員，導向 AdminFrame
                new AdminFrame().setVisible(true); 
            } else {
                // 如果是一般使用者，導向 MainFrame
                new MainFrame(targetUser.getName(), roleStr, userId).setVisible(true);
            }
            
        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "登入提示", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "系統錯誤：" + e.getMessage(), "Debug", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}