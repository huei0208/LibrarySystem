package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.BookDAO;

public class ReservationStatusFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private int userId;
    private BookDAO bookDAO = new BookDAO();
    private DefaultTableModel model;

    public ReservationStatusFrame(int userId) {
        this.userId = userId;
        setTitle("📚 我的預約書籍清單");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 只關閉自己

        setupUI();
        loadData();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(244, 249, 255)); // 海洋溫和淺藍底

        // --- 頂部標題 ---
        JLabel lblTitle = new JLabel("預約書籍排隊進度查詢", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 102, 204));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // --- 中間表格 ---
        String[] columns = {"預約書籍名稱", "登記預約時間", "目前排隊狀態"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 設定表格為唯讀
            }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        table.setRowHeight(35);
        
        // 調整欄寬百分比
        table.getColumnModel().getColumn(0).setPreferredWidth(350);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);

        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- 底部關閉按鈕 ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        JButton btnClose = new JButton("關閉視窗");
        btnClose.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> this.dispose());
        bottomPanel.add(btnClose);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 載入並刷新預約資料
     */
    private void loadData() {
        model.setRowCount(0); // 清空舊資料
        List<Object[]> reservations = bookDAO.getUserReservations(userId);
        
        if (reservations.isEmpty()) {
            // 如果空空如也，塞一筆提示訊息
            model.addRow(new Object[]{"💡 目前沒有任何預約中的書籍", "", ""});
        } else {
            for (Object[] row : reservations) {
                model.addRow(row);
            }
        }
    }
}