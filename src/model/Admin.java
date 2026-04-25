/**
 * 
 */
package model;

import java.time.LocalDateTime;

public class Admin {
	private int adminId; // PK 
    private String username; // Unique
    private String password; 
    private LocalDateTime createdAt; 

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
