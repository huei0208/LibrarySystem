package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Book;
import model.BookDAO;
import model.User;
import model.UserDAO;
import java.awt.*;
import java.util.List;
import model.ReviewDAO;

public class AdminFrame extends JFrame {
    // DAO 元件
    private BookDAO bookDAO = new BookDAO();
    private UserDAO userDAO = new UserDAO();

    // 書籍管理元件
    private JTable bookTable;
    private DefaultTableModel bookTableModel;

    // 學生管理元件
    private JTable userTable;
    private DefaultTableModel userTableModel;

    public AdminFrame() {
        setTitle("圖書館系統 - 管理員全面後台");
        setSize(950, 650); // 拉寬拉高，讓資料顯示更完整
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 建立分頁標籤面板
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. 載入並組合「書籍管理分頁」
        tabbedPane.addTab("📚 書籍規格管理", createBookManagerPanel());

        // 2. 載入並組合「學生權限管理分頁」
        tabbedPane.addTab("👥 學生帳號管理", createUserManagerPanel());

        // 3. 載入並組合「全校借還紀錄分頁」
        tabbedPane.addTab("📄 全校借還紀錄", createAllRecordsPanel());

        // 4. ✨ 新增並組合「借閱統計分析分頁」 (將 ReportPanel 整合進來)
        ReportPanel reportPanel = new ReportPanel();
        tabbedPane.addTab("📊 借閱統計分析", reportPanel);

        // 5. ✨ 載入並組合「全校書評管理分頁」
        tabbedPane.addTab("💬 全校書評管理", createAllReviewsPanel());

        // 將分頁面板放到視窗中間
        add(tabbedPane, BorderLayout.CENTER);

        // 🚀 加入切換分頁的監聽器：當切換到統計分析分頁時，自動重新整理圖表
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 3) { // 第四個分頁的 Index 是 3
                reportPanel.refreshChart();
            }
        });

        // 初始載入兩個表格的數據
        refreshBookTable();
        refreshUserTable();
    }

    /**
     * 建立書籍管理面板
     */
    private JPanel createBookManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 上方按鈕區
        JPanel topPanel = new JPanel();
        JButton addBtn = new JButton("新增新書");
        JButton deleteBtn = new JButton("下架所選書籍");
        topPanel.add(addBtn);
        topPanel.add(deleteBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        // 中間表格區
        String[] columnNames = {"ID", "書名", "作者", "主題", "出版年", "狀態"};
        bookTableModel = new DefaultTableModel(columnNames, 0);
        bookTable = new JTable(bookTableModel);
        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);

        // 按鈕事件綁定
        addBtn.addActionListener(evt -> {
            new AddBookDialog(this, () -> refreshBookTable()).setVisible(true);
        });

        deleteBtn.addActionListener(evt -> {
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow != -1) {
                int bookId = (int) bookTableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this, "確定要下架這本書嗎？", "確認下架", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (bookDAO.deleteBook(bookId)) {
                        JOptionPane.showMessageDialog(this, "書籍已下架！");
                        refreshBookTable();
                    } else {
                        JOptionPane.showMessageDialog(this, "下架失敗！");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "請先選取一本書籍！");
            }
        });

        return panel;
    }

    /**
     * 建立學生（使用者）管理面板 (升級版：支援變更一般/VIP權限)
     */
    private JPanel createUserManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 上方功能按鈕區
        JPanel topPanel = new JPanel();
        JButton toggleStatusBtn = new JButton("變更學生狀態 (停權/復權)");
        JButton toggleRoleBtn = new JButton("變更權限層級 (一般/VIP)"); 
        
        topPanel.add(toggleStatusBtn);
        topPanel.add(toggleRoleBtn); 
        panel.add(topPanel, BorderLayout.NORTH);

        // 中間學生表格區
        String[] columnNames = {"學生內部ID", "學號/帳號", "真實姓名", "權限層級", "目前狀態"};
        userTableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(userTableModel);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // 1. 停權/復權 按鈕事件
        toggleStatusBtn.addActionListener(evt -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                int userId = (int) userTableModel.getValueAt(selectedRow, 0);
                String currentStatus = (String) userTableModel.getValueAt(selectedRow, 4);
                String targetStatus = "ACTIVE".equals(currentStatus) ? "SUSPENDED" : "ACTIVE";
                String actionText = "ACTIVE".equals(currentStatus) ? "【停權】" : "【解除停權】";

                int confirm = JOptionPane.showConfirmDialog(this, 
                        "確定要對該名學生執行 " + actionText + " 嗎？\n停權後該學生將無法借書與登入。", 
                        "變更權限確認", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    if (userDAO.updateUserStatus(userId, targetStatus)) {
                        JOptionPane.showMessageDialog(this, actionText + " 執行成功！");
                        refreshUserTable(); 
                    } else {
                        JOptionPane.showMessageDialog(this, "操作失敗，請檢查資料庫連線。");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "請先在表格中選取一名學生！");
            }
        });

        // 2. 升級/降級 VIP 按鈕事件實作
        toggleRoleBtn.addActionListener(evt -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                int userId = (int) userTableModel.getValueAt(selectedRow, 0);
                
                Object currentRoleObj = userTableModel.getValueAt(selectedRow, 3);
                String currentRole = currentRoleObj != null ? currentRoleObj.toString() : "NORMAL";
                
                String targetRole = "NORMAL".equals(currentRole) ? "VIP" : "NORMAL";
                String actionText = "NORMAL".equals(currentRole) ? "【升級為 VIP】" : "【降級為 一般會員】";

                int confirm = JOptionPane.showConfirmDialog(this, 
                        "確定要將該名學生 " + actionText + " 嗎？\n權限變更將影響其借閱天數上限。", 
                        "變更權限層級確認", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    if (userDAO.updateUserRole(userId, targetRole)) {
                        JOptionPane.showMessageDialog(this, actionText + " 成功！");
                        refreshUserTable(); 
                    } else {
                        JOptionPane.showMessageDialog(this, "操作失敗，請檢查資料庫連線。");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "請先在表格中選取一名學生！");
            }
        });

        return panel;
    }

    /**
     * 建立「全校借還紀錄」查詢面板
     */
    private JPanel createAllRecordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // --- 上方：搜尋列 ---
        JPanel topPanel = new JPanel();
        JTextField txtSearch = new JTextField(15);
        JButton btnSearch = new JButton("搜尋 (學號/姓名/書名)");
        JButton btnClear = new JButton("顯示全部");
        
        topPanel.add(new JLabel("關鍵字："));
        topPanel.add(txtSearch);
        topPanel.add(btnSearch);
        topPanel.add(btnClear);
        panel.add(topPanel, BorderLayout.NORTH);

        // --- 中間：紀錄表格 ---
        String[] columnNames = {"學號", "借閱人姓名", "借閱書名", "借出時間", "應還日期", "實際歸還時間", "當前狀態"};
        DefaultTableModel recordTableModel = new DefaultTableModel(columnNames, 0);
        JTable recordTable = new JTable(recordTableModel);
        panel.add(new JScrollPane(recordTable), BorderLayout.CENTER);

        // --- 載入資料的邏輯 ---
        Runnable refreshTable = () -> {
            recordTableModel.setRowCount(0); // 清空表格
            String keyword = txtSearch.getText();
            List<Object[]> records = bookDAO.getAllSystemBorrowRecords(keyword);
            
            for (Object[] row : records) {
                String status = (row[5] != null) ? "🟢 已歸還" : "🔴 借閱中";
                String returnDateStr = (row[5] != null) ? row[5].toString() : "--- 尚未歸還 ---";
                
                recordTableModel.addRow(new Object[]{
                    row[0], // 學號
                    row[1], // 姓名
                    row[2], // 書名
                    row[3], // 借出時間
                    row[4], // 應還日期
                    returnDateStr, // 歸還時間
                    status  // 狀態
                });
            }
        };

        // 綁定按鈕事件
        btnSearch.addActionListener(e -> refreshTable.run());
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable.run();
        });

        // 初始化時先載入所有紀錄
        refreshTable.run();

        return panel;
    }

    /**
     * 刷新書籍表格資料
     */
    private void refreshBookTable() {
        bookTableModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks();
        for (Book b : books) {
            bookTableModel.addRow(new Object[]{
                b.getBookId(), 
                b.getTitle(), 
                b.getAuthors(), 
                b.getSubjects(), 
                b.getPublishYear(), 
                b.getStatus().toString() // 顯示 在庫 / 借出
            });
        }
    }

    /**
     * 刷新學生表格資料
     */
    private void refreshUserTable() {
        userTableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();
        for (User u : users) {
            userTableModel.addRow(new Object[]{
                u.getUserId(),
                u.getStudentNo(),
                u.getName(),
                u.getRoleLevel(),
                u.getStatus() != null ? u.getStatus().name() : "ACTIVE" 
            });
        }
    }
    /**
     * 建立「全校書評管理」面板
     */
    private JPanel createAllReviewsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ReviewDAO reviewDAO = new ReviewDAO();

        // --- 上方：搜尋與控制列 ---
        JPanel topPanel = new JPanel();
        JTextField txtSearch = new JTextField(15);
        JButton btnSearch = new JButton("搜尋 (書名/姓名)");
        JButton btnClear = new JButton("顯示全部");
        JButton btnDelete = new JButton("🗑️ 刪除不當留言");
        btnDelete.setBackground(new Color(255, 102, 102));
        btnDelete.setForeground(Color.WHITE);
        
        topPanel.add(new JLabel("關鍵字："));
        topPanel.add(txtSearch);
        topPanel.add(btnSearch);
        topPanel.add(btnClear);
        topPanel.add(btnDelete);
        panel.add(topPanel, BorderLayout.NORTH);

        // --- 中間：書評表格 ---
        String[] columnNames = {"評論ID(隱藏)", "書籍名稱", "留言者", "評分", "評論內容", "留言時間"};
        DefaultTableModel reviewTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable reviewTable = new JTable(reviewTableModel);
        reviewTable.setRowHeight(35);
        
        // 隱藏 ID 欄位
        reviewTable.getColumnModel().getColumn(0).setMinWidth(0);
        reviewTable.getColumnModel().getColumn(0).setMaxWidth(0);
        reviewTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        reviewTable.getColumnModel().getColumn(4).setPreferredWidth(300); // 評論內容放寬

        panel.add(new JScrollPane(reviewTable), BorderLayout.CENTER);

        // --- 載入資料的邏輯 ---
        Runnable refreshTable = () -> {
            reviewTableModel.setRowCount(0); 
            String keyword = txtSearch.getText();
            List<Object[]> reviews = reviewDAO.getAllSystemReviews(keyword);
            for (Object[] row : reviews) {
                reviewTableModel.addRow(row);
            }
        };

        // 綁定事件
        btnSearch.addActionListener(e -> refreshTable.run());
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable.run();
        });
        
        btnDelete.addActionListener(e -> {
            int selectedRow = reviewTable.getSelectedRow();
            if (selectedRow != -1) {
                int reviewId = (int) reviewTableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(panel, 
                    "確定要刪除這筆書評嗎？刪除後無法復原。", "刪除確認", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (reviewDAO.deleteReview(reviewId)) {
                        JOptionPane.showMessageDialog(panel, "書評已刪除！");
                        refreshTable.run();
                    } else {
                        JOptionPane.showMessageDialog(panel, "刪除失敗！");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(panel, "請先選取一筆要刪除的書評！");
            }
        });

        // 初始化載入
        refreshTable.run();
        return panel;
    }
}