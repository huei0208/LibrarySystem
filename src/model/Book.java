/**
 * 代表圖書館書籍，包含完整書目規格 
 */
package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Book {
    private int bookId; 
    private String title; 
    private String authors; 
    private String subjects; 
    private String publisher; 
    private String publishYear; 
    private String edition; 
    private String formatDesc; 
    private String source;
    private String note; 
    private Status status = Status.AVAILABLE; 
    private List<String> isbnList = new ArrayList<>();

    public enum Status { 
        AVAILABLE, BORROWED;
        @Override
        public String toString() {
            return this == AVAILABLE ? "在庫" : "借出";
        }
    } 

    // --- 新增：空構造函數 (保留給原本的 DataCenter 使用) ---
    public Book() {}
    public void setFirstIsbn(String isbn) {
        if (this.isbnList == null) {
            this.isbnList = new java.util.ArrayList<>();
        }
        this.isbnList.clear(); // 清除舊資料
        if (isbn != null) {
            this.isbnList.add(isbn); // 加入新的 ISBN
        }
    }

    // --- 新增：給資料庫使用的構造函數 ---
    // 當我們從資料庫抓取資料時，直接用這個方法建立物件
    public Book(int id, String title, String authors, String subjects, String pub, 
                String year, String ed, String format, String src, String note, String statusStr) {
        this.bookId = id;
        this.title = title;
        this.authors = authors;
        this.subjects = subjects;
        this.publisher = pub;
        this.publishYear = year;
        this.edition = ed;
        this.formatDesc = format;
        this.source = src;
        
        // 處理備註與識別號 (因為資料庫我們把識別號存進備註欄位了)
        this.note = note;
        if (note != null && note.contains("識別號: ")) {
            String isbnPart = note.split("識別號: ")[1];
            this.isbnList = Arrays.asList(isbnPart.split(", "));
        }

        // 轉換資料庫的 String 狀態回 Enum 狀態
        this.status = "BORROWED".equals(statusStr) ? Status.BORROWED : Status.AVAILABLE;
    }

    // --- 以下是你原本的 Getter 與 Setter (保持不變) ---
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    
    public String getSubjects() { return subjects; }
    public void setSubjects(String subjects) { this.subjects = subjects; }
    
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    
    public String getPublishYear() { return publishYear; }
    public void setPublishYear(String publishYear) { this.publishYear = publishYear; }
    
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    
    public String getFormatDesc() { return formatDesc; }
    public void setFormatDesc(String formatDesc) { this.formatDesc = formatDesc; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public List<String> getIsbnList() { return isbnList; }
    public void setIsbnList(List<String> isbnList) { this.isbnList = isbnList; }
    
    public String getFirstIsbn() {
        return (isbnList != null && !isbnList.isEmpty()) ? isbnList.get(0) : "無識別號";
    }
}
