package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import model.BookDAO;

public class HistoryFrame extends JFrame {
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private int currentUserId;
    private BookDAO bookDAO = new BookDAO();

    public HistoryFrame(int userId) {
        this.currentUserId = userId;
        setTitle("📖 個人借閱歷史紀錄");
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupUI();
        loadHistoryData();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(30, 30, 30));

        JLabel lblTitle = new JLabel("您的借閱歷史紀錄一覽");
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 22));
        lblTitle.setForeground(new Color(255, 215, 0)); // 金色文字
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        String[] columns = {"書名", "借出日期", "歸還日期", "應還日期", "狀態/是否逾期"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        historyTable = new JTable(tableModel);
        historyTable.setRowHeight(35);
        historyTable.getTableHeader().setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(historyTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        this.add(mainPanel);
    }

    private void loadHistoryData() {
        tableModel.setRowCount(0);
        List<Object[]> records = bookDAO.getBorrowHistory(currentUserId);

        for (Object[] data : records) {
            String title = (String) data[0];
            java.sql.Timestamp borrowTs = (java.sql.Timestamp) data[1];
            java.sql.Timestamp returnTs = (java.sql.Timestamp) data[2];
            java.sql.Timestamp dueTs = (java.sql.Timestamp) data[3];

            String borrowDateStr = borrowTs.toLocalDateTime().toLocalDate().toString();
            String returnDateStr = (returnTs != null) ? returnTs.toLocalDateTime().toLocalDate().toString() : "尚未歸還";
            String dueDateStr = dueTs.toLocalDateTime().toLocalDate().toString();

            // 判斷是否逾期
            String status;
            LocalDate dueDate = dueTs.toLocalDateTime().toLocalDate();
            LocalDate actualEndDate = (returnTs != null) ? returnTs.toLocalDateTime().toLocalDate() : LocalDate.now();

            if (actualEndDate.isAfter(dueDate)) {
                status = "🔴 已逾期";
            } else if (returnTs == null) {
                status = "🔵 借閱中";
            } else {
                status = "🟢 準時歸還";
            }

            tableModel.addRow(new Object[]{
                title, borrowDateStr, returnDateStr, dueDateStr, status
            });
        }
    }
}