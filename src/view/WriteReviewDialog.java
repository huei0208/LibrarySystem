package view;

import javax.swing.*;
import java.awt.*;
import model.ReviewDAO;

public class WriteReviewDialog extends JDialog {
    private int bookId;
    private int userId;
    private String bookTitle;
    private ReviewDAO reviewDAO = new ReviewDAO();

    public WriteReviewDialog(JFrame parent, int bookId, String bookTitle, int userId) {
        super(parent, "✍️ 撰寫書評 - " + bookTitle, true);
        this.bookId = bookId;
        this.userId = userId;
        this.bookTitle = bookTitle;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 248, 255)); // 海洋淺藍

        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setOpaque(false);

        // 星星評分區
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("請給這本書評分："));
        JComboBox<String> ratingBox = new JComboBox<>(new String[]{"5 ⭐⭐⭐⭐⭐", "4 ⭐⭐⭐⭐", "3 ⭐⭐⭐", "2 ⭐⭐", "1 ⭐"});
        topPanel.add(ratingBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // 留言區
        JTextArea txtComment = new JTextArea("這本書給我的感覺是...");
        txtComment.setLineWrap(true);
        txtComment.setWrapStyleWord(true);
        txtComment.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        panel.add(new JScrollPane(txtComment), BorderLayout.CENTER);

        // 送出按鈕
        JButton btnSubmit = new JButton("送出書評 🚀");
        btnSubmit.setBackground(new Color(70, 130, 180));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnSubmit.addActionListener(e -> {
            int rating = 5 - ratingBox.getSelectedIndex(); // 轉換選擇為 1~5 數字
            String comment = txtComment.getText().trim();
            
            if(reviewDAO.addReview(bookId, userId, rating, comment)) {
                JOptionPane.showMessageDialog(this, "感謝您的分享！書評已送出。");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "送出失敗，請稍後再試。");
            }
        });
        
        panel.add(btnSubmit, BorderLayout.SOUTH);
        add(panel);
    }
}