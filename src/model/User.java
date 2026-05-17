/**
 * 代表系統使用者 (學生)，對應資料庫Users表
 */
/**
 * 代表系統使用者 (學生)，對應資料庫Users表
 */
package model;

import java.time.LocalDateTime;

public class User {
    private int userId; 
    private String studentNo; // 對應資料庫的 username (A12345678)
    private String name;      // 對應資料庫的 real_name (張家豪)
    private String password;  // 對應資料庫的 password (雜湊碼)
    private RoleLevel roleLevel; 
    private LocalDateTime createdAt;
    private Status status; 

    public enum RoleLevel { NORMAL, VIP } 
    public enum Status { ACTIVE, SUSPENDED, DISABLED } // 增加 DISABLED 以防萬一

    // 1. 保留一個空建構子，這在 DAO 處理資料時非常方便
    public User() {
        this.createdAt = java.time.LocalDateTime.now();
    }

    // 2. 修改原本的建構子，確保它能處理傳入的字串並轉為 Enum
    public User(int userId, String studentNo, String name, String password, String roleLevel, String status) {
        this.userId = userId; 
        this.studentNo = studentNo;
        this.name = name;
        this.password = password;
        // 使用 try-catch 或直接轉大寫，確保資料庫抓出來的字串能對應 Enum
        try {
            this.roleLevel = RoleLevel.valueOf(roleLevel.toUpperCase()); 
            this.status = Status.valueOf(status.toUpperCase());
        } catch (Exception e) {
            this.roleLevel = RoleLevel.NORMAL; // 萬一出錯的預設值
            this.status = Status.ACTIVE;
        }
        this.createdAt = java.time.LocalDateTime.now();
    }

    // Getter 與 Setter (保持不變)
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public RoleLevel getRoleLevel() { return roleLevel; }
    public void setRoleLevel(RoleLevel roleLevel) { this.roleLevel = roleLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
