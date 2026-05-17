package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.BookDAO;
import model.BorrowRecord; 

public class OverdueStatusFrame extends JFrame {
    private BookDAO bookDAO = new BookDAO();
    private int currentUserId;
    private DefaultTableModel normalModel;
    private DefaultTableModel overdueModel;
    private JLabel lblSummary;

    public OverdueStatusFrame(int userId) {
        this.currentUserId = userId;
        setTitle("書籍到期與逾期查詢");
        setSize(850, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupUI();
        refreshData();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 245));

        // --- 上半部：即將到期 ---
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(BorderFactory.createTitledBorder("即將到期（請注意還書時間）"));
        String[] normalCols = {"書名", "借閱日期", "應還日期", "剩餘天數"};
        normalModel = new DefaultTableModel(normalCols, 0);
        JTable normalTable = new JTable(normalModel);
        northPanel.add(new JScrollPane(normalTable), BorderLayout.CENTER);

        // --- 下半部：已逾期與罰款 ---
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createTitledBorder("已逾期書籍（需繳納罰款）"));
        String[] overdueCols = {"書名", "應還日期", "逾期天數", "目前罰款 (NT$20/天)"};
        overdueModel = new DefaultTableModel(overdueCols, 0);
        JTable overdueTable = new JTable(overdueModel);
        overdueTable.setBackground(new Color(255, 235, 235)); // 淡淡的紅色背景
        southPanel.add(new JScrollPane(overdueTable), BorderLayout.CENTER);

        // 使用 SplitPane 分割上下
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northPanel, southPanel);
        splitPane.setDividerLocation(300);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // --- 最下方狀態欄 ---
        lblSummary = new JLabel("正在讀取資料...", JLabel.RIGHT);
        lblSummary.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        mainPanel.add(lblSummary, BorderLayout.SOUTH);

        this.add(mainPanel);
    }

    private void refreshData() {
        // 從我們之前討論的 DAO 方法抓取資料
        List<BorrowRecord> records = bookDAO.getCurrentBorrowingWithFine(currentUserId);
        
        normalModel.setRowCount(0);
        overdueModel.setRowCount(0);
        
        int totalFine = 0;
        int overdueCount = 0;

     // 1. 定義格式：年-月-日 時:分 (去掉 T，去掉秒)
        java.time.format.DateTimeFormatter customFormatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (BorrowRecord r : records) {
            // 2. 將 LocalDateTime 轉為漂亮的字串
            String formattedBorrowDate = r.getBorrowDate().format(customFormatter);
            String formattedDueDate = r.getDueDate().format(customFormatter);
            
            int overdueDays = r.getOverdueDays();

            if (overdueDays <= 0) {
                normalModel.addRow(new Object[]{
                    r.getTitle(), 
                    formattedBorrowDate, // 使用美化後的日期
                    formattedDueDate,    // 使用美化後的日期
                    Math.abs(overdueDays) + " 天"
                });
            } 
            else {
                overdueModel.addRow(new Object[]{
                    r.getTitle(), 
                    formattedDueDate, 
                    overdueDays + " 天", 
                    "NT$ " + (overdueDays * 20)
                });
            }
        }
        if (overdueCount > 0) {
            lblSummary.setText("<html><font color='red'>總罰款金額：NT$ " + totalFine + " 元。請儘速歸還並找管理員繳費。</font></html>");
        } else {
            lblSummary.setText("目前沒有逾期書籍，請繼續保持！");
        }
    }
}