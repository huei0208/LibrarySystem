package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.BookDAO;

public class BookHistoryFrame extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private BookDAO bookDAO = new BookDAO();

    public BookHistoryFrame(int bookId, String bookTitle) {
        setTitle("🔍 書籍流向查詢 - " + bookTitle);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(60, 63, 65));

        JLabel lblInfo = new JLabel("書籍名稱：" + bookTitle);
        lblInfo.setForeground(Color.WHITE);
        lblInfo.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        mainPanel.add(lblInfo, BorderLayout.NORTH);

        String[] cols = {"借閱人", "借出時間", "歸還時間"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        mainPanel.add(sp, BorderLayout.CENTER);

        // 載入資料 (✨ 加入長度防呆機制)
        List<Object[]> data = bookDAO.getBookBorrowHistory(bookId);
        for (Object[] row : data) {
            String bDate = (row[1] != null) ? row[1].toString() : "未知時間";
            String rDate = (row[2] != null) ? row[2].toString() : "尚未歸還";
            
            // 安全地截斷秒數 (只有當長度大於 16 時才截斷)
            if (bDate.length() >= 16) bDate = bDate.substring(0, 16);
            if (rDate.length() >= 16 && !rDate.equals("尚未歸還")) rDate = rDate.substring(0, 16);
            
            model.addRow(new Object[]{ row[0], bDate, rDate });
        }

        this.add(mainPanel);
    }
}