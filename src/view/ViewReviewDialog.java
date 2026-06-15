package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.ReviewDAO;

public class ViewReviewDialog extends JDialog {
    private ReviewDAO reviewDAO = new ReviewDAO();

    public ViewReviewDialog(JFrame parent, int bookId, String bookTitle) {
        super(parent, "💬 書評專區 - " + bookTitle, true);
        setSize(600, 400);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("大家對《" + bookTitle + "》的評價");
        titleLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 表格設定
        String[] cols = {"讀者", "評分", "評論內容", "留言時間"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(300); // 評論放寬
        table.getColumnModel().getColumn(3).setPreferredWidth(140);

        // 載入資料
        List<Object[]> reviews = reviewDAO.getReviewsByBook(bookId);
        if(reviews.isEmpty()) {
            model.addRow(new Object[]{"-", "-", "目前還沒有人留下評論喔！快來當第一個吧！", "-"});
        } else {
            for (Object[] r : reviews) model.addRow(r);
        }

        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(mainPanel);
    }
}
