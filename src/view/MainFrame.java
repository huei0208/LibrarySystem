package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList; // ✨ 新增
import java.util.List;
import model.BookDAO; 

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private String userRole; 
    private int userId; 
    private BookDAO bookDAO = new BookDAO();
    private List<String> notificationList = new ArrayList<>(); // ✨ 新增：訊息容器
    private JButton btnMailbox;

    public MainFrame(String userName, String role, int userId) {
        this.userRole = role;
        this.userId = userId; 
        
        setTitle("圖書館借書系統 - 歡迎回來 " + userName);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel longBgPanel = new BackgroundPanel("resources/main_long_menu.jpg");
        longBgPanel.setLayout(null);
        longBgPanel.setPreferredSize(new Dimension(1000, 1000)); 

        JLabel lblStatus = new JLabel("目前身分：" + (role.equals("V") || role.equalsIgnoreCase("VIP") ? "VIP 會員" : "普通會員"));
        lblStatus.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        lblStatus.setForeground(Color.BLUE);
        lblStatus.setBounds(50, 20, 300, 30);
        longBgPanel.add(lblStatus);

        // --- ✨ 改良版：訊息中心按鈕 (放在右上角) ---
        collectAllNotifications(); // 登入時默默收集訊息
        
        JButton btnMailbox = new JButton();
        // 1. 如果有通知，顯示紅色數字；沒有的話顯示「無新訊息」
        if (notificationList.size() > 0) {
            btnMailbox.setText("📬 訊息中心 (" + notificationList.size() + ")");
            btnMailbox.setForeground(Color.RED); // ✨ 將文字變為紅色
        } else {
            btnMailbox.setText("📬 訊息中心 (0)");
            btnMailbox.setForeground(Color.GRAY);
        }
        
        btnMailbox.setBounds(750, 20, 200, 40);
        btnMailbox.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        
        // 2. ✨✨ 刪除按鈕邊框與背景的關鍵設定 ✨✨
        btnMailbox.setContentAreaFilled(false); // 去除按鈕背景色
        btnMailbox.setBorderPainted(false);     // ✨ 關鍵：移除原本的小框框邊框
        btnMailbox.setFocusPainted(false);      // 移除點擊時的焦點框
        btnMailbox.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 滑鼠移上去變手指
        
        btnMailbox.addActionListener(e -> showNotificationDialog());
        longBgPanel.add(btnMailbox);

        // (1) 書籍查詢
        addMenuButton(longBgPanel, 350, 150, 300, 60, "書籍查詢、借書系統", e -> {
            String searchRole = (userRole.equals("V") || userRole.equalsIgnoreCase("VIP")) ? "VIP" : "NORMAL";
            new SearchFrame(searchRole, this.userId).setVisible(true);
        });

        // (2) 還書功能
        addMenuButton(longBgPanel, 350, 230, 300, 60, "還書功能", e -> {
            new ReturnFrame(this.userId).setVisible(true);
        });

        // (3) 到期與逾期查詢
        addMenuButton(longBgPanel, 350, 310, 300, 60, "到期與逾期查詢", e -> {
            new OverdueStatusFrame(this.userId).setVisible(true);
        });

        // (4) 個人借還紀錄
        addMenuButton(longBgPanel, 350, 390, 300, 60, "個人借還紀錄", e -> {
            new HistoryFrame(this.userId).setVisible(true);
        });

        // (5) VIP 專屬區
        if (role.equals("V") || role.equalsIgnoreCase("VIP")) {
            addMenuButton(longBgPanel, 350, 470, 300, 60, "VIP 預約功能", e -> JOptionPane.showMessageDialog(this, "VIP 專屬預約介面已開啟。"));
        } else {
            addMenuButton(longBgPanel, 350, 470, 300, 60, "升級 VIP", e -> JOptionPane.showMessageDialog(this, "請洽櫃台付費升級以享有預約權限。"));
        }

        // (6) 登出
        addMenuButton(longBgPanel, 350, 670, 300, 60, "登出系統", e -> {
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        JScrollPane scrollPane = new JScrollPane(longBgPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        this.setContentPane(scrollPane);
    } 

    // --- ✨ 新增：收集所有系統訊息的方法 ---
    private void collectAllNotifications() {
        notificationList.clear();
        
        // 1. 逾期提醒
        List<model.BorrowRecord> records = bookDAO.getCurrentBorrowingWithFine(this.userId);
        int totalFine = 0, overdueCount = 0;
        for (model.BorrowRecord r : records) {
            if (r.getOverdueDays() > 0) {
                overdueCount++;
                totalFine += (r.getOverdueDays() * 20);
            }
        }
        if (overdueCount > 0) notificationList.add("❌ 逾期提醒：您有 " + overdueCount + " 本書已逾期，累計罰款 NT$" + totalFine);

        // 2. 到期提醒
        List<String> expiringBooks = bookDAO.getExpiringBooks(this.userId, 3);
        if (expiringBooks != null) notificationList.addAll(expiringBooks);

        // 3. 預約通知
        List<String> readyBooks = bookDAO.getReadyReservations(this.userId);
        if (readyBooks != null) {
            for (String title : readyBooks) notificationList.add("📚 預約到館：您預約的《" + title + "》已到館");
        }
    }

    private void showNotificationDialog() {
        if (notificationList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "目前沒有新訊息。");
            return;
        }

        JTextArea area = new JTextArea(10, 40);
        for (String msg : notificationList) {
            area.append(msg + "\n");
        }
        area.setEditable(false);

        // 顯示訊息
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "系統訊息中心", JOptionPane.INFORMATION_MESSAGE);
        
        // 點開後處理邏輯
        bookDAO.clearReservationNotifications(this.userId);
        notificationList.clear();

        // ✨ 關鍵修正：更新按鈕文字而不是清空整個畫面
        // 找到你的 btnMailbox (記得將 btnMailbox 設為成員變數 private JButton btnMailbox;)
        // 如果 btnMailbox 是區域變數，請把它改成類別成員變數
        updateMailboxButton(); 
    }

    private void updateMailboxButton() {
        // 假設 btnMailbox 已經是類別成員變數
        if (btnMailbox != null) {
            btnMailbox.setText("📬 訊息中心 (0)");
            btnMailbox.setForeground(Color.GRAY);
        }
    }

    private void addMenuButton(JPanel p, int x, int y, int w, int h, String title, ActionListener action) {
        JButton btn = new JButton(title);
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        btn.setForeground(new Color(0, 191, 255));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        p.add(btn);
    }
}