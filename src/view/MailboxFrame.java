package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.NotificationDAO;

public class MailboxFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private int userId;
    private NotificationDAO notiDAO = new NotificationDAO();
    private JTable table;
    private DefaultTableModel model;

    public MailboxFrame(int userId) {
        this.userId = userId;
        setTitle("📬 系統訊息中心");
        setSize(750, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 只關閉自己，不關閉主程式

        setupUI();
        loadData();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 248, 255)); // 淡淡的海洋藍背景

        // --- 頂部標題 ---
        JLabel lblTitle = new JLabel("個人訊息信箱", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 22));
        lblTitle.setForeground(new Color(0, 102, 204));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // --- 中間：含有 Checkbox 的信件表格 ---
        String[] cols = {"選取", "ID (隱藏)", "系統通知內容", "收到時間"};
        
        // 自訂 TableModel 讓第一欄變成可以打勾的 Checkbox
        model = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class; 
                return super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // 只有第一欄的 Checkbox 允許點擊更改
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        table.setRowHeight(40);
        
        // 隱藏第二欄的 notification_id (程式刪除時需要，但不用給使用者看)
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setWidth(0);
        
        // 調整欄寬
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(450);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);

        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- 底部：功能按鈕 ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);

        // ✨ 新增：一鍵清空按鈕
        JButton btnDeleteAll = new JButton("一鍵清空所有訊息 🗑️");
        btnDeleteAll.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnDeleteAll.setBackground(new Color(255, 99, 71)); // 番茄紅，提醒這是破壞性動作
        btnDeleteAll.setForeground(Color.WHITE);
        btnDeleteAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDeleteAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "確定要清空所有的系統通知嗎？\n(刪除後將無法復原喔！)", 
                "一鍵清空確認", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // 呼叫 DAO 的新方法進行全刪
                if (notiDAO.deleteAllNotifications(userId)) {
                    JOptionPane.showMessageDialog(this, "所有訊息已清空！");
                    loadData(); // 重新整理表格（讓畫面變成空的）
                } else {
                    JOptionPane.showMessageDialog(this, "目前信箱是空的，或者發生錯誤。");
                }
            }
        });

        // 原本的刪除選取按鈕 (改個顏色區隔開來)
        JButton btnDelete = new JButton("刪除選取訊息");
        btnDelete.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnDelete.setBackground(new Color(255, 160, 122)); // 亮珊瑚色
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.addActionListener(e -> deleteSelected());

        JButton btnClose = new JButton("關閉");
        btnClose.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> this.dispose());

        // 依序加入底部面板
        bottomPanel.add(btnDeleteAll);
        bottomPanel.add(btnDelete);
        bottomPanel.add(btnClose);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 載入信箱資料
     */
    private void loadData() {
        model.setRowCount(0); // 先清空表格
        List<Object[]> notis = notiDAO.getUserNotifications(userId);
        for (Object[] row : notis) {
            model.addRow(row);
        }
    }

    /**
     * 執行批次刪除
     */
    private void deleteSelected() {
        int count = 0;
        // 走訪每一列，檢查有沒有被打勾
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean isChecked = (Boolean) model.getValueAt(i, 0);
            if (isChecked != null && isChecked) {
                int notiId = (int) model.getValueAt(i, 1);
                notiDAO.deleteNotification(notiId);
                count++;
            }
        }
        
        if (count > 0) {
            JOptionPane.showMessageDialog(this, "成功刪除 " + count + " 則訊息！");
            loadData(); // 刪除後刷新表格
        } else {
            JOptionPane.showMessageDialog(this, "請先勾選要刪除的訊息！", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }
}