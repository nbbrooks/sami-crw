package crw.general;

import crw.ui.ImagePanel;
import edu.cmu.ri.crw.data.UtmPose;
import java.awt.image.BufferedImage;

/**
 *
 * @author pscerri
 */
public class BufferedImageWithPose implements Comparable<BufferedImageWithPose> {
    
    public final BufferedImage img;
    private final UtmPose pose;
    long c = ImagePanel.count++;

    public BufferedImageWithPose(BufferedImage img, UtmPose pose) {
        this.img = img;
        this.pose = pose;
    }

    public int compareTo(BufferedImageWithPose t) {
        if (t.c > c) {
            return 1;
        } else if (t.c < c) {
            return -1;
        } else {
            return 0;
        }
    }

    public BufferedImage getImg() {
        return img;
    }

    public UtmPose getPose() {
        return pose;
    }
    
    
}
