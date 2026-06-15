package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import model.BookDAO;
import model.NotificationDAO; 

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private String userRole; 
    private int userId; 
    private BookDAO bookDAO = new BookDAO();
    private NotificationDAO notiDAO = new NotificationDAO(); 
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

        // ============================================================
        // ✨ 信箱與通知系統初始化
        // ============================================================
        syncSystemNotifications(); // 1. 登入時，自動偵測逾期/到期並寫入信箱資料庫

        btnMailbox = new JButton();
        btnMailbox.setBounds(750, 20, 200, 40);
        btnMailbox.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        
        btnMailbox.setContentAreaFilled(false); 
        btnMailbox.setBorderPainted(false);     
        btnMailbox.setFocusPainted(false);      
        btnMailbox.setCursor(new Cursor(Cursor.HAND_CURSOR)); 

        updateMailboxBadge(); // 2. 根據資料庫更新數字與顏色

        btnMailbox.addActionListener(e -> openMailbox());
        longBgPanel.add(btnMailbox);
        // ============================================================

        // (1) 書籍查詢系統
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

        // ============================================================
        // ✨ 新排版：將「預約紀錄查詢」與「VIP專屬區」分開排列
        // ============================================================

        // (5) 預約紀錄查詢 (放在原本 VIP 的位置 y=470)
        addMenuButton(longBgPanel, 350, 470, 300, 60, "預約紀錄查詢", e -> {
            new ReservationStatusFrame(this.userId).setVisible(true);
        });

        // (6) VIP 專屬區 (往下移一格，y 座標變成 550)
        if (role.equals("V") || role.equalsIgnoreCase("VIP")) {
            addMenuButton(longBgPanel, 350, 550, 300, 60, "VIP 預約功能", e -> {
                JOptionPane.showMessageDialog(this, "VIP 專屬預約介面已開啟。");
            });
        } else {
            addMenuButton(longBgPanel, 350, 550, 300, 60, "升級 VIP", e -> {
                JOptionPane.showMessageDialog(this, "請洽櫃台付費升級以享有預約權限。");
            });
        }

        // (7) 登出 (維持在最下方 y=670，因為你的長面板有 1000px 高，空間還很夠)
        addMenuButton(longBgPanel, 350, 670, 300, 60, "登出系統", e -> {
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        JScrollPane scrollPane = new JScrollPane(longBgPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        this.setContentPane(scrollPane);

        // ============================================================
        // ✨✨✨ 3. 雙重提醒：畫面載入後，自動彈出最新信件視窗 ✨✨✨
        // ============================================================
        showLoginPopup();
    } 

    /**
     * ✨ 登入時的彈出視窗 (雙重提醒)
     */
    private void showLoginPopup() {
        List<Object[]> notis = notiDAO.getUserNotifications(this.userId);
        if (!notis.isEmpty()) {
            // 使用 invokeLater 讓主畫面先跑出來，再彈出視窗，體驗更順暢！
            SwingUtilities.invokeLater(() -> {
                JTextArea area = new JTextArea(Math.min(10, notis.size() + 3), 40);
                area.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
                area.setText("🚨 系統偵測到您目前有 " + notis.size() + " 則重要通知：\n\n");
                
                for (Object[] row : notis) {
                    area.append(" • " + row[2] + "\n"); // row[2] 是資料庫裡的 message 欄位
                }
                
                area.append("\n💡 提示：您可以點擊右上角「📬 訊息中心」來管理或刪除這些通知。");
                area.setEditable(false);

                JOptionPane.showMessageDialog(MainFrame.this, new JScrollPane(area), "🔔 登入重要提醒", JOptionPane.WARNING_MESSAGE);
            });
        }
    }

    /**
     * ✨ 更新信箱按鈕上的數字和顏色
     */
    private void updateMailboxBadge() {
        int count = notiDAO.getUnreadCount(this.userId);
        if (count > 0) {
            btnMailbox.setText("📬 訊息中心 (" + count + ")");
            btnMailbox.setForeground(Color.RED); 
        } else {
            btnMailbox.setText("📬 訊息中心 (0)");
            btnMailbox.setForeground(Color.GRAY); 
        }
    }

    /**
     * ✨ 開啟信箱視窗
     */
    private void openMailbox() {
        MailboxFrame mailFrame = new MailboxFrame(this.userId);
        mailFrame.setVisible(true);
        
        // 關鍵：監聽信箱視窗何時被關閉，關閉時馬上重新計算右上角的數字！
        mailFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                updateMailboxBadge();
            }
        });
    }

    /**
     * ✨ 將系統提醒自動同步寫入資料庫
     */
    private void syncSystemNotifications() {
        // 1. 處理逾期 (如果沒還書，系統就會再次幫你寫進信箱)
        List<model.BorrowRecord> records = bookDAO.getCurrentBorrowingWithFine(this.userId);
        int totalFine = 0, overdueCount = 0;
        for (model.BorrowRecord r : records) {
            if (r.getOverdueDays() > 0) {
                overdueCount++;
                totalFine += (r.getOverdueDays() * 20);
            }
        }
        if (overdueCount > 0) {
            notiDAO.addNotification(this.userId, "❌ 逾期警告：您有 " + overdueCount + " 本書已逾期！累計罰款：NT$ " + totalFine + " 元");
        }

        // 2. 處理即將到期
        List<String> expiringBooks = bookDAO.getExpiringBooks(this.userId, 3); 
        if (expiringBooks != null) {
            for (String info : expiringBooks) {
                notiDAO.addNotification(this.userId, info);
            }
        }
        
        // 3. 處理預約到館
        List<String> readyBooks = bookDAO.getReadyReservations(this.userId);
        if (readyBooks != null && !readyBooks.isEmpty()) {
            for (String title : readyBooks) {
                notiDAO.addNotification(this.userId, "📚 預約到館：您預約的《" + title + "》已到館，請儘速前往借閱！");
            }
            // 寫入信箱後，清除舊的預約狀態
            bookDAO.clearReservationNotifications(this.userId);
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