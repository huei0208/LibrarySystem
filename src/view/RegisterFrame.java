package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.UserDAO; // 引入負責資料庫操作的 DAO

/**
 * 註冊頁面：負責將新使用者資料寫入資料庫
 */
public class RegisterFrame extends JFrame {
    private static final long serialVersionUID = 1L; 
    private JTextField txtAccount, txtRealName; 
    private JPasswordField txtPass, txtConfirmPass; // ✨ 修正：將 txtRoleCode 改為 txtConfirmPass 密碼框
    private UserDAO userDAO = new UserDAO(); 

    public RegisterFrame() {
        setTitle("圖書館管理系統 - 註冊");
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 設定背景面板
        BackgroundPanel bgPanel = new BackgroundPanel("resources/userre.jpg");
        bgPanel.setLayout(null); 
        this.setContentPane(bgPanel);

        // 2. 建立輸入框並對位 (座標完全不動，保證對齊你的海洋藍原圖)
        txtAccount = createInput(425, 223, 200, 28);
        bgPanel.add(txtAccount);

        txtRealName = createInput(425, 280, 200, 28);
        bgPanel.add(txtRealName);

        txtPass = createPasswordInput(425, 336, 200, 28);
        bgPanel.add(txtPass);

        // ✨ 修正：最下面那一欄改成「確認密碼」輸入框，並且是密碼遮罩格式 (***)
        txtConfirmPass = createPasswordInput(425, 390, 200, 28);
        bgPanel.add(txtConfirmPass);

        // 3. 建立隱形按鈕
        JButton btnRegister = createHiddenButton(404, 434, 195, 35);
        bgPanel.add(btnRegister);

        JButton btnBack = createHiddenButton(457, 485, 87, 13);
        bgPanel.add(btnBack);

        // --- 功能邏輯 ---

        // 註冊按鈕事件
        btnRegister.addActionListener(e -> {
            String account = txtAccount.getText().trim();
            String realName = txtRealName.getText().trim(); 
            String password = new String(txtPass.getPassword());
            String confirmPassword = new String(txtConfirmPass.getPassword()); // ✨ 撈取確認密碼

            // 1. 基礎防呆檢查
            if (account.isEmpty() || realName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有欄位皆不能為空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. ✨✨ 核心安全驗證：檢查兩次密碼是否相同 ✨✨
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "❌ 兩次輸入的密碼不一致，請重新檢查！", "密碼錯誤", JOptionPane.ERROR_MESSAGE);
                txtConfirmPass.setText(""); // 清空確認密碼框方便使用者重填
                txtConfirmPass.requestFocus(); // 將滑鼠游標自動跳回該框
                return; // 攔截，不讓程式繼續往下走資料庫寫入
            }

            // 3. 邏輯完全合理化：新註冊帳號，權限一律寫死為最安全的 "NORMAL"
            String role = "NORMAL";

            // 呼叫 UserDAO 將資料存入資料庫
            boolean isSuccess = userDAO.register(account, realName, password, role);

            if (isSuccess) {
                JOptionPane.showMessageDialog(this, "🎉 註冊成功！請使用新帳號登入。");
                new LoginFrame().setVisible(true); // 返回登入頁面
                this.dispose(); 
            } else {
                JOptionPane.showMessageDialog(this, "註冊失敗：帳號可能重複或連線錯誤。", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 返回按鈕事件
        btnBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });

        // 座標偵測器
        bgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("註冊頁點擊座標: x=" + e.getX() + ", y=" + e.getY());
            }
        });
    }

    // 輔助工具：建立透明輸入框
    private JTextField createInput(int x, int y, int w, int h) {
        JTextField f = new JTextField();
        f.setBounds(x, y, w, h);
        f.setOpaque(false);
        f.setBorder(null);
        f.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        return f;
    }

    // 輔助工具：建立透明密碼輸入框
    private JPasswordField createPasswordInput(int x, int y, int w, int h) {
        JPasswordField f = new JPasswordField();
        f.setBounds(x, y, w, h);
        f.setOpaque(false);
        f.setBorder(null);
        f.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        return f;
    }

    // 輔助工具：建立隱形點擊按鈕
    private JButton createHiddenButton(int x, int y, int w, int h) {
        JButton b = new JButton();
        b.setBounds(x, y, w, h);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        return b;
    }
}