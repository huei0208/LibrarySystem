/**
 * 當系統找不到指定的資源（書籍、使用者、紀錄等）時使用
 */
package exception;

public class ResourceNotFoundException extends LibraryException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}