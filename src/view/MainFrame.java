/**
 * 
 */
package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import model.BookDAO; 

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private String userRole; 
    private int userId; 
    private BookDAO bookDAO = new BookDAO();

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

        // (1) 書籍查詢系統
        addMenuButton(longBgPanel, 350, 150, 300, 60, "書籍查詢、借書系統", e -> {
            String searchRole = (userRole.equals("V") || userRole.equalsIgnoreCase("VIP")) ? "VIP" : "NORMAL";
            new SearchFrame(searchRole, this.userId).setVisible(true);
        });

        // (2) 還書功能
        addMenuButton(longBgPanel, 350, 230, 300, 60, "還書功能", e -> {
            new ReturnFrame(this.userId).setVisible(true);
        });

        // (3) 修改這裡：導向新的「到期與逾期查詢」視窗
        addMenuButton(longBgPanel, 350, 310, 300, 60, "到期與逾期查詢", e -> {
            new OverdueStatusFrame(this.userId).setVisible(true);
        });

        // (4) 個人借還紀錄
        addMenuButton(longBgPanel, 350, 390, 300, 60, "個人借還紀錄", e -> {
            new HistoryFrame(this.userId).setVisible(true);
        });

        // (5) VIP 專屬區
        if (role.equals("V") || role.equalsIgnoreCase("VIP")) {
            addMenuButton(longBgPanel, 350, 470, 300, 60, "VIP 預約功能", e -> {
                JOptionPane.showMessageDialog(this, "VIP 專屬預約介面已開啟。");
            });
        } else {
            addMenuButton(longBgPanel, 350, 470, 300, 60, "升級 VIP", e -> {
                JOptionPane.showMessageDialog(this, "請洽櫃台付費升級以享有預約權限。");
            });
        }

        // (6) 登出
        addMenuButton(longBgPanel, 350, 670, 300, 60, "登出系統", e -> {
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        JScrollPane scrollPane = new JScrollPane(longBgPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        this.setContentPane(scrollPane);
        
        // 執行所有檢查
        checkDueReminder();
        checkReservationArrival();
        checkOverdueStatus(); // 呼叫你寫的方法
    } 

    private void checkOverdueStatus() {
        // 修改變數名為 this.userId 並使用宣告好的 bookDAO
        List<model.BorrowRecord> records = bookDAO.getCurrentBorrowingWithFine(this.userId);
        
        int totalFine = 0;
        int overdueCount = 0;

        for (model.BorrowRecord r : records) {
            if (r.getOverdueDays() > 0) {
                overdueCount++;
                totalFine += (r.getOverdueDays() * 20);
            }
        }
        
        if (overdueCount > 0) {
            String msg = String.format(
                "【逾期還書提醒】\n\n" +
                "您目前有 %d 本書已超過歸還期限！\n" +
                "目前的累計罰款為：NT$ %d 元\n\n" +
                "請儘速前往「到期查詢」確認細節，並歸還書籍。", 
                overdueCount, totalFine
            );
            JOptionPane.showMessageDialog(this, msg, "逾期告警", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- 其餘方法 (checkDueReminder, checkReservationArrival, addMenuButton) 保持不變 ---
    private void checkDueReminder() {
        model.BookDAO dao = new model.BookDAO();
        List<String> expiringBooks = dao.getExpiringBooks(this.userId, 3); 
        if (expiringBooks != null && !expiringBooks.isEmpty()) {
            StringBuilder msg = new StringBuilder("⚠️到期提醒：\n");
            for (String info : expiringBooks) msg.append(info).append("\n");
            JOptionPane.showMessageDialog(this, msg.toString(), "系統通知", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void checkReservationArrival() {
        model.BookDAO dao = new model.BookDAO();
        List<String> readyBooks = dao.getReadyReservations(this.userId);
        if (readyBooks != null && !readyBooks.isEmpty()) {
            StringBuilder msg = new StringBuilder("您預約的書籍已到館：\n");
            for (String title : readyBooks) msg.append("— 《").append(title).append("》\n");
            msg.append("\n請儘速前往櫃檯或搜尋系統辦理借閱。");
            JOptionPane.showMessageDialog(this, msg.toString(), "預約取書通知", JOptionPane.INFORMATION_MESSAGE);
            dao.clearReservationNotifications(this.userId);
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