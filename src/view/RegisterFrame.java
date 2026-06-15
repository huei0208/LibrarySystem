package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.UserDAO;

public class RegisterFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private JTextField txtAccount, txtRealName;
    private JPasswordField txtPass, txtConfirmPass;
    private UserDAO userDAO = new UserDAO();

    public RegisterFrame() {
        setTitle("圖書館管理系統 - 註冊");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel bgPanel = new BackgroundPanel("resources/userre.jpg");
        bgPanel.setLayout(null);
        this.setContentPane(bgPanel);

        txtAccount = createInput(425, 223, 200, 28);
        bgPanel.add(txtAccount);

        txtRealName = createInput(425, 280, 200, 28);
        bgPanel.add(txtRealName);

        txtPass = createPasswordInput(425, 336, 200, 28);
        bgPanel.add(txtPass);

        txtConfirmPass = createPasswordInput(425, 390, 200, 28);
        bgPanel.add(txtConfirmPass);

        JButton btnRegister = createHiddenButton(404, 434, 195, 35);
        bgPanel.add(btnRegister);

        JButton btnBack = createHiddenButton(457, 485, 87, 13);
        bgPanel.add(btnBack);

        btnRegister.addActionListener(e -> {
            String account = txtAccount.getText().trim();
            String realName = txtRealName.getText().trim();
            String password = new String(txtPass.getPassword());
            String confirmPassword = new String(txtConfirmPass.getPassword());

            if (account.isEmpty() || realName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有欄位皆不能為空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "兩次輸入的密碼不一致，請重新檢查！", "密碼錯誤", JOptionPane.ERROR_MESSAGE);
                txtConfirmPass.setText("");
                txtConfirmPass.requestFocus();
                return;
            }

            String role = "NORMAL";

            boolean isSuccess = userDAO.register(account, realName, password, role);

            if (isSuccess) {
                JOptionPane.showMessageDialog(this, "註冊成功！請使用新帳號登入。");
                new LoginFrame().setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "註冊失敗：帳號可能重複或連線錯誤。", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });

        bgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("註冊頁點擊座標: x=" + e.getX() + ", y=" + e.getY());
            }
        });
    }

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
        f.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        return f;
    }

    private JButton createHiddenButton(int x, int y, int w, int h) {
        JButton b = new JButton();
        b.setBounds(x, y, w, h);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        return b;
    }
}