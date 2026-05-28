/**
 * 
 */
package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.Book;
import model.BookDAO;

public class SearchFrame extends JFrame {
    private BookDAO bookDAO = new BookDAO();
    private DefaultTableModel tableModel;
    private JTable resultTable;
    private String userRole; 
    private int currentUserId;
    
    private JTextField txtSearch;
    private JComboBox<String> comboCategory;

    public SearchFrame(String role, int userId) {
        this.userRole = role;
        this.currentUserId = userId;
        
        setTitle("海底圖書館 - 書籍查詢系統 (" + userRole + " 模式)");
        setSize(1100, 750); // 稍微調高高度
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupUI(); 
        refreshTable(bookDAO.getAllBooks()); 
    }

    private void setupUI() {
        JPanel bgPanel = new JPanel(); 
        bgPanel.setLayout(null);
        bgPanel.setBackground(new Color(30, 50, 100)); 
        this.setContentPane(bgPanel);

        // --- 搜尋欄位 ---
        JLabel lblHint = new JLabel("查詢方式：");
        lblHint.setBounds(50, 15, 100, 25);
        lblHint.setForeground(Color.BLACK);
        lblHint.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        bgPanel.add(lblHint);
        
        String[] categories = {"書名", "作者", "主題", "出版社", "ISBN (識別號)"};
        comboCategory = new JComboBox<>(categories);
        comboCategory.setBounds(50, 40, 120, 35);
        bgPanel.add(comboCategory);

        txtSearch = new JTextField();
        txtSearch.setBounds(180, 40, 350, 35);
        bgPanel.add(txtSearch);

        JButton btnSearch = new JButton("查詢");
        btnSearch.setBounds(540, 40, 120, 35);
        bgPanel.add(btnSearch);

        JButton btnReset = new JButton("顯示全部");
        btnReset.setBounds(670, 40, 100, 35);
        bgPanel.add(btnReset);

        JButton btnShowFav = new JButton("我的收藏");
        btnShowFav.setBounds(780, 40, 120, 35);
        btnShowFav.setBackground(new Color(255, 215, 0));
        btnShowFav.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        bgPanel.add(btnShowFav);

        // 表格設定
        String[] columns = {"ID", "識別號", "題名", "作者", "主題", "出版社", "狀態"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(30);
        resultTable.getColumnModel().getColumn(0).setMinWidth(0);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBounds(50, 100, 1000, 450);
        bgPanel.add(scrollPane);

        //下方功能按鈕區
     
        // 1. 借閱按鈕
        JButton btnBorrow = new JButton("借閱此書");
        btnBorrow.setBounds(900, 560, 150, 40);
        btnBorrow.setBackground(new Color(60, 179, 113));
        btnBorrow.setForeground(Color.BLACK);
        bgPanel.add(btnBorrow);

        // 2. 查看歷史按鈕
        JButton btnBookHistory = new JButton("查看借閱歷史");
        btnBookHistory.setBounds(730, 560, 150, 40); 
        btnBookHistory.setBackground(new Color(100, 149, 237));
        btnBookHistory.setForeground(Color.BLACK);
        bgPanel.add(btnBookHistory);

        // 3. 預約按鈕
        JButton btnReserve = new JButton("預約此書");
        btnReserve.setBounds(560, 560, 150, 40);
        btnReserve.setBackground(new Color(255, 140, 0));
        btnReserve.setForeground(Color.BLACK);
        bgPanel.add(btnReserve);

        // 4. 加入收藏
        JButton btnAddFav = new JButton("加入收藏");
        btnAddFav.setBounds(900, 610, 150, 40); 
        btnAddFav.setBackground(new Color(255, 215, 0));
        bgPanel.add(btnAddFav);

        // 5. 取消收藏
        JButton btnRemoveFav = new JButton("取消收藏");
        btnRemoveFav.setBounds(730, 610, 150, 40); 
        btnRemoveFav.setBackground(new Color(205, 92, 92)); 
        btnRemoveFav.setForeground(Color.BLACK);
        bgPanel.add(btnRemoveFav);

        // 搜尋與刷新
        btnSearch.addActionListener(e -> {
            String keyword = txtSearch.getText().trim();
            refreshTable(bookDAO.searchBooks((String)comboCategory.getSelectedItem(), keyword));
        });

        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable(bookDAO.getAllBooks());
        });

        btnShowFav.addActionListener(e -> {
            List<Book> favList = bookDAO.getFavoriteBooks(currentUserId);
            if (favList.isEmpty()) JOptionPane.showMessageDialog(this, "收藏清單是空的！");
            else refreshTable(favList);
        });

        // 借閱邏輯
        btnBorrow.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "請選擇一本書！"); return; }
         // 借閱數量限制檢查
            int currentCount = bookDAO.getCurrentBorrowCount(currentUserId);
            int limit = (userRole.contains("V") || userRole.equalsIgnoreCase("VIP")) ? 10 : 5;

            if (currentCount >= limit) {
                String msg = String.format("⚠️ 借閱失敗！\n您的身分為：%s\n目前已借閱：%d 本\n借閱上限為：%d 本\n請先還書後再進行借閱。", 
                              (limit == 10 ? "VIP 會員" : "普通會員"), currentCount, limit);
                JOptionPane.showMessageDialog(this, msg, "數量上限提醒", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String status = resultTable.getValueAt(row, 6).toString();
            if ("借出".equals(status)) { JOptionPane.showMessageDialog(this, "書已被借走囉！"); return; }
            
            String[] options;
            if (userRole.contains("V") || userRole.equalsIgnoreCase("VIP")) {
                // VIP的選項：1, 3, 7,專屬的14天
                options = new String[]{"1天", "3天", "7天", "14天 (VIP專屬)"};
            } else {
                // 普通會員1, 3, 7 天
                options = new String[]{"1天", "3天", "7天"};
            }
            int choice = JOptionPane.showOptionDialog(this, "選擇天數", "借閱確認", 0, 3, null, options, options[0]);
            if (choice != -1) {
                int days = Integer.parseInt(options[choice].replaceAll("[^0-9]", ""));
                if (bookDAO.borrowBook(currentUserId, (int)resultTable.getValueAt(row, 0), days)) {
                    JOptionPane.showMessageDialog(this, "借閱成功！");
                    refreshTable(bookDAO.getAllBooks());
                }
            }
        });

        // 3. 預約邏輯 (✨ 加入在庫防呆機制)
        btnReserve.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row == -1) { 
                JOptionPane.showMessageDialog(this, "請先選擇一本書！"); 
                return; 
            }
            
            // 🛑 核心防呆：抓取表格第 6 欄的「狀態」
            String status = resultTable.getValueAt(row, 6).toString();
            if (status.contains("在庫") || status.contains("AVAILABLE")) {
                JOptionPane.showMessageDialog(this, 
                    "這本書還乖乖躺在圖書館的書架上喔！\n請直接點擊「借閱此書」，不需要排隊預約。", 
                    "預約無效", 
                    JOptionPane.WARNING_MESSAGE);
                return; // 擋下來，不執行後續的資料庫預約寫入
            }

            // 確定不是在庫後，才執行原本的預約寫入
            String result = bookDAO.reserveBook(currentUserId, (int)resultTable.getValueAt(row, 0));
            if ("SELF_BORROWED".equals(result)) {
                JOptionPane.showMessageDialog(this, "您正持有此書，無需預約。");
            } else if ("SUCCESS".equals(result)) {
                JOptionPane.showMessageDialog(this, "預約成功！已經幫您排入等待名單。");
            } else {
                JOptionPane.showMessageDialog(this, "預約失敗，您可能已經預約過這本書了。");
            }
        });

        // 查看歷史
        btnBookHistory.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row != -1) new BookHistoryFrame((int)resultTable.getValueAt(row, 0), resultTable.getValueAt(row, 2).toString()).setVisible(true);
        });

        // 收藏與取消收藏
        btnAddFav.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row != -1 && bookDAO.addFavorite(currentUserId, (int)resultTable.getValueAt(row, 0))) 
                JOptionPane.showMessageDialog(this, "已加入收藏！");
        });

        btnRemoveFav.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row != -1 && bookDAO.deleteFavorite(currentUserId, (int)resultTable.getValueAt(row, 0))) {
                JOptionPane.showMessageDialog(this, "已移除收藏。");
                refreshTable(bookDAO.getFavoriteBooks(currentUserId));
            }
        });

        // ... (在設定 btnReserve 或 btnAddFav 的附近加入這段)

        // 6. 查看書評按鈕
        JButton btnViewReview = new JButton("查看書評 💬");
        btnViewReview.setBounds(560, 610, 150, 40); // 找個空位塞進去，你可以依照畫面微調座標
        btnViewReview.setBackground(new Color(135, 206, 250)); // 淺天空藍
        btnViewReview.setForeground(Color.BLACK);
        bgPanel.add(btnViewReview);

        // 綁定事件：點擊後彈出評論列表
        btnViewReview.addActionListener(e -> {
            int row = resultTable.getSelectedRow();
            if (row == -1) { 
                JOptionPane.showMessageDialog(this, "請先選擇一本想看書評的書籍！"); 
                return; 
            }
            int bookId = (int) resultTable.getValueAt(row, 0);
            String bookTitle = resultTable.getValueAt(row, 2).toString();
            
            // 彈出我們剛剛寫好的查看書評視窗
            new ViewReviewDialog(this, bookId, bookTitle).setVisible(true);
        });
    } 

    private void refreshTable(List<Book> list) {
        tableModel.setRowCount(0);
        for (Book b : list) {
            tableModel.addRow(new Object[]{ b.getBookId(), b.getFirstIsbn(), b.getTitle(), b.getAuthors(), b.getSubjects(), b.getPublisher(), b.getStatus().toString() });
        }
    }
}