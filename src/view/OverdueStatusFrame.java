package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.BookDAO;
import model.BorrowRecord; 

public class OverdueStatusFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private BookDAO bookDAO = new BookDAO();
    private int currentUserId;
    private DefaultTableModel normalModel;
    private DefaultTableModel overdueModel;
    private JLabel lblSummary;
    private JTextArea warningArea; // ✨ 新增：負責顯示警告文字的區塊

    public OverdueStatusFrame(int userId) {
        this.currentUserId = userId;
        setTitle("書籍到期與逾期查詢");
        setSize(850, 750); // ✨ 稍微拉高一點視窗，讓警告面板有足夠空間
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

        // 使用 SplitPane 分割上下表格
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northPanel, southPanel);
        splitPane.setDividerLocation(250);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // ✨ 最下方：狀態總結與警告面板區塊
        // ==========================================
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 1. 罰款總結文字
        lblSummary = new JLabel("正在讀取資料...", JLabel.RIGHT);
        lblSummary.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        bottomPanel.add(lblSummary, BorderLayout.NORTH);

        // 2. 建立警告文字區塊
        warningArea = new JTextArea();
        warningArea.setEditable(false);
        warningArea.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        warningArea.setLineWrap(true);
        warningArea.setWrapStyleWord(true);

        JScrollPane warningScrollPane = new JScrollPane(warningArea);
        warningScrollPane.setPreferredSize(new Dimension(800, 120)); // 固定高度
        warningScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.RED, 2), "⚠️ 到期與逾期警告"));
        
        bottomPanel.add(warningScrollPane, BorderLayout.CENTER);

        // 將整個底部區塊加入主畫面
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

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
                    formattedBorrowDate, 
                    formattedDueDate,    
                    Math.abs(overdueDays) + " 天"
                });
            } 
            else {
                // ✨ FIX：幫你補上計數器與罰款累加，不然下面的 lblSummary 永遠是 0
                overdueCount++;
                totalFine += (overdueDays * 20);

                overdueModel.addRow(new Object[]{
                    r.getTitle(), 
                    formattedDueDate, 
                    overdueDays + " 天", 
                    "NT$ " + (overdueDays * 20)
                });
            }
        }

        // 更新罰款標籤
        if (overdueCount > 0) {
            lblSummary.setText("<html><font color='red'>總計 " + overdueCount + " 本逾期！總罰款金額：NT$ " + totalFine + " 元。請儘速歸還並找管理員繳費。</font></html>");
        } else {
            lblSummary.setText("目前沒有逾期書籍，請繼續保持！");
        }

        // ==========================================
        // ✨ 更新專屬警告面板內容
        // ==========================================
        List<String> expiringBooks = bookDAO.getExpiringBooks(currentUserId, 3);
        if (expiringBooks == null || expiringBooks.isEmpty()) {
            warningArea.setText("\n 🟢 目前無逾期或即將到期書籍，請繼續保持良好的借閱習慣！");
            warningArea.setBackground(new Color(230, 255, 230)); // 淺綠色背景
            warningArea.setForeground(new Color(0, 100, 0)); // 深綠色字體
        } else {
            StringBuilder sb = new StringBuilder(" 🚨 系統提醒您有 " + expiringBooks.size() + " 筆重要通知：\n\n");
            for (String msg : expiringBooks) {
                sb.append("   ").append(msg).append("\n");
            }
            warningArea.setText(sb.toString());
            warningArea.setBackground(new Color(255, 235, 235)); // 淺紅色背景
            warningArea.setForeground(Color.RED); // 紅色字體
        }
    }
}