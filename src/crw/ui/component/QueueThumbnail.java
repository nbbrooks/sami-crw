package crw.ui.component;

import static crw.ui.queue.QueueItem.THUMB_SCALED_HEIGHT;
import static crw.ui.queue.QueueItem.THUMB_SCALED_WIDTH;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import sami.markup.Priority;
import sami.uilanguage.UiPanel;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class QueueThumbnail extends UiPanel {

    protected ToUiMessage message;

    public QueueThumbnail(ToUiMessage message) {
        this.message = message;
        BufferedImage original = new BufferedImage(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        addText(original, Priority.getPriority(message.getPriority()).toString(), message.getClass().getSimpleName());
        add(new JLabel(new ImageIcon(original.getScaledInstance(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, Image.SCALE_FAST))));
    }

    private BufferedImage addText(BufferedImage image, String line1, String line2) {
        int w = image.getWidth();
        int h = image.getHeight();
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        // Write line1
        g2d.setPaint(Color.red);
        g2d.setFont(new Font("Serif", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        int x = image.getWidth() - fm.stringWidth(line1) - 5;
        int y = fm.getHeight();
        g2d.drawString(line1, x, y);
        // Write line2
        g2d.setPaint(Color.black);
        g2d.setFont(new Font("Serif", Font.BOLD, 14));
        x = image.getWidth() - fm.stringWidth(line2) - 5;
        g2d.drawString(line2, 0, y * 2);
        g2d.dispose();
        return image;
    }
}
