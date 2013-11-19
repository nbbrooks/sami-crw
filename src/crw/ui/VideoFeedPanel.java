package crw.ui;

import java.awt.image.BufferedImage;
import java.awt.Color;

/**
 *
 * @author Jijun Wang
 */
public class VideoFeedPanel extends javax.swing.JPanel {

    protected BufferedImage image = null;
    public int margin;
    public boolean fixedRatio;
    protected double scale = 1.0;
    protected int ox = 0, oy = 0, width = 0, height = 0;
    private int oldWidth = 0, oldHeight = 0;
    private java.awt.Dimension oldDim = null;

    public VideoFeedPanel() {
        this(0, true);
    }

    public VideoFeedPanel(int margin, boolean fixedRatio) {
        this.margin = margin;
        this.fixedRatio = fixedRatio;
    }

    public void setBorder(Color clr, int thick) {
        setBorder(new javax.swing.border.LineBorder(clr, thick));
    }

    @Override
    public void paint(java.awt.Graphics g) {
        super.paint(g);
        if (image != null) {
            java.awt.Dimension d = getSize();

            if (!fixedRatio) {
                ox = 0;
                oy = 0;
                width = d.width;
                height = d.height;
            } else if (oldDim == null || oldDim.width != d.width || oldDim.height != d.height
                    || oldWidth != image.getWidth() || oldHeight != image.getHeight()) {
                double sx = (double) (d.width - 2 * margin) / image.getWidth();
                double sy = (double) (d.height - 2 * margin) / image.getHeight();
                scale = (sx > sy) ? sy : sx;
                ox = (int) (d.width - scale * image.getWidth()) / 2;
                oy = (int) (d.height - scale * image.getHeight()) / 2;
                width = (int) (scale * image.getWidth());
                height = (int) (scale * image.getHeight());
                oldDim = d;
                oldWidth = image.getWidth();
                oldHeight = image.getHeight();
            }
            g.drawImage(image, ox, oy, width, height, null);
        }
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }
}