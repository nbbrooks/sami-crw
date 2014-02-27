package crw.ui;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author nbb
 */
public class ColorSlider extends JPanel {

    JSlider slider;

    public ColorSlider() {
        super();
        slider = new JSlider(0, 100, 0);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                setBackground(valueToColor(source.getValue(), 0, 100));
            }
        });
        setBackground(valueToColor(slider.getValue(), 0, 100));
        add(slider);
    }

    /**
     * Heatmap color computation taken from
     * http://stackoverflow.com/questions/2374959/algorithm-to-convert-any-positive-integer-to-an-rgb-value
     *
     * @param value The value to compute the color for
     * @param min Min value of data range
     * @param max Max value of data range
     * @return Heatmap color
     */
    public Color valueToColor(double value, double min, double max) {

        double wavelength = 0.0, factor = 0.0, red = 0.0, green = 0.0, blue = 0.0, gamma = 1.0;
        double adjMin = min - 5;
        double adjMax = max - 5;

        if (value < adjMin) {
            wavelength = 0.0;
        } else if (value <= adjMax) {
            wavelength = (value - adjMin) / (adjMax - adjMin) * (750.0f - 350.0f) + 350.0f;
        } else {
            wavelength = 0.0;
        }

        if (wavelength == 0.0f) {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        } else if (wavelength < 440.0f) {
            red = -(wavelength - 440.0f) / (440.0f - 350.0f);
            green = 0.0;
            blue = 1.0;
        } else if (wavelength < 490.0f) {
            red = 0.0;
            green = (wavelength - 440.0f) / (490.0f - 440.0f);
            blue = 1.0;
        } else if (wavelength < 510.0f) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510.0f) / (510.0f - 490.0f);
        } else if (wavelength < 580.0f) {
            red = (wavelength - 510.0f) / (580.0f - 510.0f);
            green = 1.0;
            blue = 0.0;
        } else if (wavelength < 645) {
            red = 1.0;
            green = -(wavelength - 645.0f) / (645.0f - 580.0f);
            blue = 0.0;
        } else {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        }

        if (wavelength == 0.0f) {
            factor = 0.0;
        } else if (wavelength < 420) {
            factor = 0.3f + 0.7f * (wavelength - 350.0f) / (420.0f - 350.0f);
        } else if (wavelength < 680) {
            factor = 1.0;
        } else {
            factor = 0.3f + 0.7f * (750.0f - wavelength) / (750.0f - 680.0f);
        }

        Color color = new Color(
                (int) Math.floor(255.0 * Math.pow(red * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(green * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(blue * factor, gamma)));
        return color;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.add(new ColorSlider());
        frame.setVisible(true);
        frame.pack();
    }
}
