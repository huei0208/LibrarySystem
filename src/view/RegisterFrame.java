/**
 * 
 */
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
    private static final long serialVersionUID = 1L; // 解決 Serial Version 警告
    private JTextField txtAccount, txtEmail, txtRoleCode; 
    private JPasswordField txtPass; 
    private UserDAO userDAO = new UserDAO(); // 建立 DAO 實例

    public RegisterFrame() {
        setTitle("圖書館管理系統 - 註冊");
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 設定背景面板
        BackgroundPanel bgPanel = new BackgroundPanel("resources/userre.jpg");
        bgPanel.setLayout(null); 
        this.setContentPane(bgPanel);

        // 2. 建立輸入框並對位 (座標沿用你原本的設定)
        txtAccount = createInput(412, 229, 200, 28);
        bgPanel.add(txtAccount);

        txtEmail = createInput(412, 282, 200, 28);
        bgPanel.add(txtEmail);

        txtPass = createPasswordInput(412, 334, 200, 28);
        bgPanel.add(txtPass);

        txtRoleCode = createInput(412, 386, 200, 28);
        bgPanel.add(txtRoleCode);

        // 3. 建立隱形按鈕
        JButton btnRegister = createHiddenButton(404, 434, 195, 35);
        bgPanel.add(btnRegister);

        JButton btnBack = createHiddenButton(457, 485, 87, 13);
        bgPanel.add(btnBack);

        // --- 功能邏輯 ---

        // 註冊按鈕事件
        btnRegister.addActionListener(e -> {
            String account = txtAccount.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPass.getPassword());
            String role = txtRoleCode.getText().trim().toUpperCase(); 

            // 基礎防呆檢查
            if (account.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "帳號與密碼不能為空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 角色權限處理：若輸入不是 VIP 或 NORMAL，則預設為 NORMAL
            if (!role.equals("VIP") && !role.equals("NORMAL")) {
                role = "NORMAL";
            }

            // 呼叫 UserDAO 將資料存入資料庫
            // 我們將 account 同時作為資料庫的 username 與 real_name 存入
            boolean isSuccess = userDAO.register(account, account, password, role);

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

        // 座標偵測器 (開發微調用)
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

    private JPasswordField createPasswordInput(int x, int y, int w, int h) {
        JPasswordField f = new JPasswordField();
        f.setBounds(x, y, w, h);
        f.setOpaque(false);
        f.setBorder(null);
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