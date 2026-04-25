/**
 * 基礎例外類別，所有的圖書館相關錯誤都必須繼承此類別
 */
package exception;

public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
