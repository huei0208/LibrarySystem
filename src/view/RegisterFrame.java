package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import model.UserDAO;

public class RegisterFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private JTextField txtAccount, txtEmail, txtRoleCode;
    private JPasswordField txtPass;
    private UserDAO userDAO = new UserDAO();

    public RegisterFrame() {
        setTitle("圖書館管理系統 - 註冊");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel bgPanel = new BackgroundPanel("resources/userre.jpg");
        bgPanel.setLayout(null);
        this.setContentPane(bgPanel);

        txtAccount = createInput(412, 229, 200, 28);
        bgPanel.add(txtAccount);

        txtEmail = createInput(412, 282, 200, 28);
        bgPanel.add(txtEmail);

        txtPass = createPasswordInput(412, 334, 200, 28);
        bgPanel.add(txtPass);

        txtRoleCode = createInput(412, 386, 200, 28);
        bgPanel.add(txtRoleCode);

        JButton btnRegister = createHiddenButton(404, 434, 195, 35);
        bgPanel.add(btnRegister);

        JButton btnBack = createHiddenButton(457, 485, 87, 13);
        bgPanel.add(btnBack);

        btnRegister.addActionListener(e -> {
            String account = txtAccount.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPass.getPassword());
            String role = txtRoleCode.getText().trim().toUpperCase();

            if (account.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "帳號、Email 與密碼不能為空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(this, "Email 格式不正確，請重新輸入。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!role.equals("VIP") && !role.equals("NORMAL")) {
                role = "NORMAL";
            }

            boolean isSuccess = userDAO.register(account, account, password, role);

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