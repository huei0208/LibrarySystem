/**
 * 代表借還書交易紀錄，包含借出日期、到期日與實際歸還時間 
 */
package model; 

import java.time.LocalDateTime;

public class BorrowRecord {
	private int recordId; // PK 
    private int userId; // FK
    private int bookId; // FK
    private LocalDateTime borrowDate; 
    private LocalDateTime dueDate; 
    private LocalDateTime returnDate; // 未歸還則為 NULL 
    private int borrowDays; 
    private LocalDateTime createdAt; 

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public int getBorrowDays() { return borrowDays; }
    public void setBorrowDays(int borrowDays) { this.borrowDays = borrowDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}