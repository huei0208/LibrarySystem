/**
 * 代表圖書館書籍，包含完整書目規格 
 */
package model;

/**
 * 
 */
public class Book {
	private int bookId; // 書籍唯一識別碼 (PK)
    private String title; 
    private String authors; 
    private String subjects; 
    private String publisher; 
    private String publishYear; 
    private String edition; 
    private String formatDesc; 
    private String source;
    private String note; 
    private Status status; // AVAILABLE / BORROWED [cite: 118]

    public enum Status { AVAILABLE, BORROWED } 

    // Getter 與 Setter 
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
}
