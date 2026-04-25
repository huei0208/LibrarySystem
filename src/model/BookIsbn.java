/**
 * 代表書籍的 ISBN 紀錄，一本書可能對應多個 ISBN 紀錄 。
 */
package model;

/**
 * 
 */
public class BookIsbn {
	private int isbnId; // PK [cite: 120]
    private int bookId; // FK [cite: 120]
    private String isbn;

    public int getIsbnId() { return isbnId; }
    public void setIsbnId(int isbnId) { this.isbnId = isbnId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
}
