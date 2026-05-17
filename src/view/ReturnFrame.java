package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import model.BookDAO; // 確保有 import 你的 DAO

public class ReturnFrame extends JFrame {
    private static final long serialVersionUID = 1L; // 解決你之前的紅線警告
    private JTable returnTable;
    private DefaultTableModel tableModel;
    private int currentUserId;
    private BookDAO bookDAO = new BookDAO(); // 建立 DAO 實例

    public ReturnFrame(int userId) {
        this.currentUserId = userId;
        setTitle("🔄 海底圖書館 - 還書與到期查詢");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupUI();
        loadMyBorrowedBooks(); // 初始化時直接抓資料庫
    }

    private void setupUI() {
    // 1. 強制讓視窗的 ContentPane 使用 BorderLayout
    this.getContentPane().setLayout(new BorderLayout());

    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(new Color(45, 45, 45));

    JLabel lblTitle = new JLabel("您目前尚未歸還的書籍清單");
    lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 18));
    lblTitle.setForeground(Color.WHITE);
    mainPanel.add(lblTitle, BorderLayout.NORTH);

    // 表格
    String[] columns = {"書籍ID", "書名", "借出日期", "到期日", "剩餘時間", "狀態", "紀錄ID"};
    tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };

    returnTable = new JTable(tableModel);
    returnTable.setRowHeight(30);
    
    // 隱藏 ID 欄位
    returnTable.removeColumn(returnTable.getColumnModel().getColumn(6));
    returnTable.removeColumn(returnTable.getColumnModel().getColumn(0));

    JScrollPane scrollPane = new JScrollPane(returnTable);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    // 2. 建立一個按鈕容器，確保按鈕有足夠的空間顯示
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(new Color(45, 45, 45)); // 跟背景顏色一致
    
    JButton btnReturn = new JButton("確認還書");
    btnReturn.setPreferredSize(new Dimension(200, 50)); // 強制給按鈕一個大小
    btnReturn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
    btnReturn.setBackground(new Color(70, 130, 180));
    btnReturn.setForeground(Color.WHITE);
    btnReturn.setOpaque(true); // Mac 系統下必須加這行顏色才會出來
    btnReturn.setBorderPainted(false); // 讓外觀乾淨一點
    
    btnReturn.addActionListener(e -> handleReturn());
    
    buttonPanel.add(btnReturn);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    // 3. 確保將 mainPanel 加入到視窗最底層
    this.getContentPane().add(mainPanel, BorderLayout.CENTER);
}

    private void loadMyBorrowedBooks() {
        tableModel.setRowCount(0);
        LocalDate today = LocalDate.now();

        // 1. 從資料庫獲取該使用者的「未歸還」紀錄
        // 這裡回傳的是 List<Object[]> {bookId, title, borrowDate, dueDate, recordId}
        List<Object[]> records = bookDAO.getBorrowedBooks(currentUserId);

        for (Object[] data : records) {
            // 將資料庫拿到的 Timestamp 轉為 LocalDate 進行計算
            LocalDate borrowDate = ((java.sql.Timestamp) data[2]).toLocalDateTime().toLocalDate();
            LocalDate dueDate = ((java.sql.Timestamp) data[3]).toLocalDateTime().toLocalDate();
            
            // 2. 計算剩餘天數
            long daysRemaining = ChronoUnit.DAYS.between(today, dueDate);
            
            String remainStr = daysRemaining >= 0 ? daysRemaining + " 天" : "逾期 " + Math.abs(daysRemaining) + " 天";
            String status = daysRemaining >= 0 ? "借閱中" : "⚠️ 已逾期";

            // 3. 填入表格 (依序：ID, 書名, 借出, 到期, 剩餘, 狀態, 紀錄ID)
            tableModel.addRow(new Object[]{
                data[0], data[1], borrowDate, dueDate, remainStr, status, data[4]
            });
        }
    }

    private void handleReturn() {
        int row = returnTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "請先選擇要歸還的書籍。");
            return;
        }

        // 1. 取得資料
        int bookId = (int) tableModel.getValueAt(row, 0);
        int recordId = (int) tableModel.getValueAt(row, 6); 
        String bookName = tableModel.getValueAt(row, 1).toString();

        // 2. 確認視窗
        int confirm = JOptionPane.showConfirmDialog(this, 
            "確定要歸還 《" + bookName + "》 嗎？", "還書確認", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // --- 核心邏輯：只呼叫這個整合了預約通知的新方法 ---
            String notifyUser = bookDAO.returnBookAndNotify(recordId, bookId, this.currentUserId);

            if ("ERROR".equals(notifyUser)) {
                // 執行 SQL 失敗
                JOptionPane.showMessageDialog(this, "還書失敗，請檢查資料庫連線。", "錯誤", JOptionPane.ERROR_MESSAGE);
            } else {
                // 還書成功（notifyUser 有可能是 姓名字串，也可能是 null）
                String msg = "《" + bookName + "》 已歸還成功！";
                
                if (notifyUser != null) {
                    // 如果有人預約，多加一行通知
                    msg += "\n\n📢 系統提醒：此書已被使用者「" + notifyUser + "」預約，\n請將書放至預約保留架。";
                }
                
                JOptionPane.showMessageDialog(this, msg, "系統通知", JOptionPane.INFORMATION_MESSAGE);
                
                // 重新整理介面清單
                loadMyBorrowedBooks(); 
            }
        }
    }
}