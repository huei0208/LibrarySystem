/**
 * 
 */
package view;

import javax.swing.JPanel;
import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.Image;

public class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String fileName) {
        // 設定背景圖片路徑
        this.backgroundImage = new ImageIcon(fileName).getImage();
    }

    // Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}