package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.User;
import model.UserDAO; // 引入 DAO
import exception.*;
import model.AdminDAO;

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
        txtUser.setBounds(395, 239, 214, 26); 
        txtUser.setOpaque(false); 
        txtUser.setBorder(null);  
        txtUser.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
        bgPanel.add(txtUser);

        // 4. 密碼輸入框
        txtPass = new JPasswordField();
        txtPass.setBounds(395, 300, 214, 26);
        txtPass.setOpaque(false);
        txtPass.setBorder(null);
        bgPanel.add(txtPass);

        // 5. 登入按鈕 (隱形)
        JButton btnLogin = new JButton();
        btnLogin.setBounds(385, 343, 217, 33);
        btnLogin.setContentAreaFilled(false); 
        btnLogin.setBorderPainted(false);     
        bgPanel.add(btnLogin);
        

        // 6. 註冊按鈕 (隱形)
        JButton btnRegister = new JButton();
        btnRegister.setBounds(480, 390, 30, 18); 
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
            // --- 通道 1：先去「學生表 (Users)」找找看 ---
            User targetUser = userDAO.login(inputNo, inputPass);

            if (targetUser != null) {
                // 檢查是否被停權
                if (targetUser.getStatus() == User.Status.SUSPENDED || 
                    targetUser.getStatus() == User.Status.DISABLED) { 
                    throw new BorrowingRuleException("登入失敗：使用者 " + targetUser.getName() + " 已被停權。");
                }

                // 學生 (NORMAL 或 VIP) 登入成功
                JOptionPane.showMessageDialog(this, "🎉 登入成功！歡迎回來 " + targetUser.getName());
                String roleStr = targetUser.getRoleLevel().toString(); 
                int userId = targetUser.getUserId(); 
                
                this.dispose(); // 關閉登入視窗
                new MainFrame(targetUser.getName(), roleStr, userId).setVisible(true); // 進入學生主畫面
                return; // 執行完就結束
            }

            // --- 通道 2：如果學生表找不到，換去「管理者表 (Admins)」找 ---
            AdminDAO adminDAO = new AdminDAO(); 
            if (adminDAO.verifyAdmin(inputNo, inputPass)) { 
                // 管理員登入成功
                JOptionPane.showMessageDialog(this, "🛠️ 歡迎進入管理者後台！");
                this.dispose();
                new AdminFrame().setVisible(true); // 進入管理員專屬畫面
                return; // 執行完就結束
            }

            // --- 如果兩邊都找不到人 ---
            throw new ResourceNotFoundException("登入失敗：帳號不存在或密碼錯誤。");

        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "登入提示", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "系統錯誤：" + e.getMessage(), "Debug", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    // --- 請把這段放在 LoginFrame 類別的最底下，最後一個 } 之前 ---
    public static void main(String[] args) {
        // 啟動登入畫面的測試引擎
        java.awt.EventQueue.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    } 
}