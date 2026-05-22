import javax.swing.SwingUtilities;
import view.EntryFrame;

public class LibrarySystemLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EntryFrame().setVisible(true);
        });
    }
}