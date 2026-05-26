package view;

import javax.swing.*;
import java.awt.*;
import model.Book;
import model.BookDAO;

public class AddBookDialog extends JDialog {
    // 1. 定義所有欄位
    private JTextField titleField = new JTextField(15);
    private JTextField authorField = new JTextField(15);
    private JTextField subjectField = new JTextField(15);
    private JTextField pubField = new JTextField(15);
    private JTextField yearField = new JTextField(15);
    private JTextField editionField = new JTextField(15);
    private JTextField formatField = new JTextField(15);
    private JTextField sourceField = new JTextField(15);
    private JTextField noteField = new JTextField(15);
    private BookDAO bookDAO = new BookDAO();

    public AddBookDialog(JFrame parent, Runnable onSave) {
        super(parent, "新增書籍", true);
        setLayout(new GridLayout(10, 2, 10, 10)); // 10行2列，加上間距

        // 2. 加入介面
        add(new JLabel("書名:")); add(titleField);
        add(new JLabel("作者:")); add(authorField);
        add(new JLabel("主題:")); add(subjectField);
        add(new JLabel("出版社:")); add(pubField);
        add(new JLabel("出版年:")); add(yearField);
        add(new JLabel("版本:")); add(editionField);
        add(new JLabel("格式:")); add(formatField);
        add(new JLabel("資料來源:")); add(sourceField);
        add(new JLabel("識別號/備註:")); add(noteField);

        JButton saveBtn = new JButton("儲存");
        saveBtn.addActionListener(evt -> {
            Book b = new Book();
            b.setTitle(titleField.getText());
            b.setAuthors(authorField.getText());
            b.setSubjects(subjectField.getText());
            b.setPublisher(pubField.getText());
            b.setPublishYear(yearField.getText());
            b.setEdition(editionField.getText());
            b.setFormatDesc(formatField.getText());
            b.setSource(sourceField.getText());
            b.setNote(noteField.getText());
            
            if (bookDAO.addBook(b)) {
                JOptionPane.showMessageDialog(this, "新增成功！");
                onSave.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "新增失敗，請檢查輸入內容！");
            }
        });
        add(saveBtn);
        pack();
        setLocationRelativeTo(parent); // 彈出視窗置中
    }
}