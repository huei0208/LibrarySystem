/**
 * 代表系統使用者 (學生)，對應資料庫Users表
 */
package model;

import java.time.LocalDateTime;

public class User {
	private int userId; // PK
    private String studentNo; // 學號 
    private String name; 
    private String password; 
    private RoleLevel roleLevel; // NORMAL / VIP 
    private LocalDateTime createdAt;
    private Status status; // ACTIVE / SUSPENDED 

    public enum RoleLevel { NORMAL, VIP } 
    public enum Status { ACTIVE, SUSPENDED } 

    // Getter 與 Setter
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
