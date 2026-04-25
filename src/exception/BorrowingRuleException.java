/**
 * 當使用者的操作違反借閱規則時，如書籍已被借出、使用者已被停權、借閱數量超過上限。
 */
package exception;

public class BorrowingRuleException extends LibraryException {
    public BorrowingRuleException(String message) {
        super(message);
    }
}