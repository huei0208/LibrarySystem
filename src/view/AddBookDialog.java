package view;

import javax.swing.*;
import java.awt.*;
import model.Book;
import model.BookDAO;

public class AddBookDialog extends JDialog {
    private JTextField titleField = new JTextField(15);
    private JTextField authorField = new JTextField(15);
    private JComboBox<String> subjectBox;
    private JTextField pubField = new JTextField(15);
    private JTextField yearField = new JTextField(15);
    private JTextField noteField = new JTextField(15);
    private BookDAO bookDAO = new BookDAO();

    public AddBookDialog(JFrame parent, Runnable onSave) {
        super(parent, "📚 新增書籍", true);
        
        setLayout(new GridLayout(7, 2, 10, 15)); 
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        String[] defaultSubjects = {"資訊科技", "自然科學", "商業理財", "文學小說", "人文史地", "藝術設計", "語言學習", "其他"};
        subjectBox = new JComboBox<>(defaultSubjects);
        subjectBox.setEditable(true); 
        subjectBox.setBackground(Color.WHITE);

        // ✨ 介面更新：把所有欄位都加上 ⭐ 標示為必填
        add(new JLabel("⭐ 書名:")); add(titleField);
        add(new JLabel("⭐ 作者:")); add(authorField);
        add(new JLabel("⭐ 主題:")); add(subjectBox);
        add(new JLabel("⭐ 出版社:")); add(pubField);
        add(new JLabel("⭐ 出版年:")); add(yearField);
        add(new JLabel("⭐ ISBN / 備註:")); add(noteField);

        JButton saveBtn = new JButton("💾 儲存並上架");
        saveBtn.setBackground(new Color(70, 130, 180)); 
        saveBtn.setForeground(Color.WHITE);
        
        saveBtn.addActionListener(evt -> {
            // 取得所有輸入資料
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String subject = subjectBox.getSelectedItem().toString().trim(); 
            String publisher = pubField.getText().trim();
            String yearStr = yearField.getText().trim();
            String isbnOrNote = noteField.getText().trim();
            
            // 🛑 防呆機制 1：檢查「所有」欄位是否都有填寫
            if (title.isEmpty() || author.isEmpty() || subject.isEmpty() || 
                publisher.isEmpty() || yearStr.isEmpty() || isbnOrNote.isEmpty()) {
                JOptionPane.showMessageDialog(this, "帶有 ⭐ 的欄位全部都必須填寫喔！", "資料不完整", JOptionPane.WARNING_MESSAGE);
                return; // 直接中斷，不讓它存進資料庫
            }

            // 🛑 防呆機制 2：確保「出版年」輸入的是純數字
            try {
                Integer.parseInt(yearStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "「出版年」只能輸入數字喔！(例如：2026)", "格式錯誤", JOptionPane.WARNING_MESSAGE);
                return; // 中斷儲存
            }

            // 資料都沒問題，準備打包存檔！
            Book b = new Book();
            b.setTitle(title);
            b.setAuthors(author);
            b.setSubjects(subject);
            b.setPublisher(publisher);
            b.setPublishYear(yearStr);
            b.setNote(isbnOrNote);
            
            b.setEdition("");
            b.setFormatDesc("");
            b.setSource("");
            
            if (bookDAO.addBook(b)) {
                JOptionPane.showMessageDialog(this, "🎉 書籍新增成功！");
                onSave.run(); 
                dispose(); 
            } else {
                JOptionPane.showMessageDialog(this, "新增失敗，請檢查資料庫連線！", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        add(new JLabel()); 
        add(saveBtn);
        
        pack();
        setLocationRelativeTo(parent); 
    }
}