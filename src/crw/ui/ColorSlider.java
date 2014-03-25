package crw.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
    JLabel nullLabel;
    boolean nullColor;
    final String NULL_TEXT = "<html><font color=rgb(188,6,6)>NULL</font></html>";
    final Color[] colorDividers = new Color[]{Color.BLACK, Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.WHITE};
    final double dividerIncrement = 1.0 / (colorDividers.length - 1);

    public ColorSlider() {
        super(new BorderLayout());
        nullLabel = new JLabel(NULL_TEXT);
        nullColor = true;
        slider = new JSlider(0, 100, 0);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                Color color = valueToColor(source.getValue(), 0, 100);
                if (color != null) {
                    nullLabel.setText(" ");
                    nullColor = false;
                    setBackground(color);
                } else {
                    // No selected color
                    nullLabel.setText(NULL_TEXT);
                    nullColor = true;
                    setBackground(Color.WHITE);
                }
            }
        });
        add(slider, BorderLayout.NORTH);
        add(nullLabel, BorderLayout.SOUTH);
        setBackground(Color.WHITE);
    }

    public Color getColor() {
        if (nullColor) {
            return null;
        } else {
            return getBackground();
        }
    }

    public void setColor(Color color) {
        slider.setValue((int) (colorToValue(color) * 100));
        repaint();
    }

    public Color valueToColor(double value, double min, double max) {
        if (value == 0.0) {
            // No value
            return null;
        } else {
            double sliderPercent = value / 100.0;
            int lowerColorIndex = (int) (sliderPercent / dividerIncrement);
            if (lowerColorIndex == colorDividers.length - 1) {
                // Bar is all the way to the right, return the last divider color
                return colorDividers[colorDividers.length - 1];
            } else {
                // Linearly scale between the two adjacent divider colors
                double adjacentFraction = (sliderPercent - lowerColorIndex * dividerIncrement) / dividerIncrement;
                int r = (int) (colorDividers[lowerColorIndex].getRed() + (adjacentFraction * (colorDividers[lowerColorIndex + 1].getRed() - colorDividers[lowerColorIndex].getRed())));
                int g = (int) (colorDividers[lowerColorIndex].getGreen() + (adjacentFraction * (colorDividers[lowerColorIndex + 1].getGreen() - colorDividers[lowerColorIndex].getGreen())));
                int b = (int) (colorDividers[lowerColorIndex].getBlue() + (adjacentFraction * (colorDividers[lowerColorIndex + 1].getBlue() - colorDividers[lowerColorIndex].getBlue())));
                return new Color(r, g, b);
            }
        }
    }

    public double colorToValue(Color color) {
        if (color == null) {
            return 0.0;
        } else {
            for (int i = 0; i < colorDividers.length - 1; i++) {
                if (((color.getRed() >= colorDividers[i].getRed() && color.getRed() <= colorDividers[i + 1].getRed())
                        || (color.getRed() >= colorDividers[i + 1].getRed() && color.getRed() <= colorDividers[i].getRed()))
                        && ((color.getGreen() >= colorDividers[i].getGreen() && color.getGreen() <= colorDividers[i + 1].getGreen())
                        || (color.getGreen() >= colorDividers[i + 1].getGreen() && color.getGreen() <= colorDividers[i].getGreen()))
                        && ((color.getBlue() >= colorDividers[i].getBlue() && color.getBlue() <= colorDividers[i + 1].getBlue())
                        || (color.getBlue() >= colorDividers[i + 1].getBlue() && color.getBlue() <= colorDividers[i].getBlue()))) {
                    // All of the color's RGB values are between these two divider colors
                    // Linearly scale value between the two divider colors' values
                    double adjacentFraction = 0.0;
                    if (colorDividers[i].getRed() != colorDividers[i + 1].getRed()) {
                        adjacentFraction = fromRangeToProgress(color.getRed() * 1.0, colorDividers[i].getRed() * 1.0, colorDividers[i + 1].getRed() * 1.0);
                    } else if (colorDividers[i].getGreen() != colorDividers[i + 1].getGreen()) {
                        adjacentFraction = fromRangeToProgress(color.getGreen() * 1.0, colorDividers[i].getGreen() * 1.0, colorDividers[i + 1].getGreen() * 1.0);
                    } else if (colorDividers[i].getBlue() != colorDividers[i + 1].getBlue()) {
                        adjacentFraction = fromRangeToProgress(color.getBlue() * 1.0, colorDividers[i].getBlue() * 1.0, colorDividers[i + 1].getBlue() * 1.0);
                    }
                    return (i * dividerIncrement) + (adjacentFraction * dividerIncrement);
                }
            }
        }
        return 1.0;
    }

    private double fromProgressToRange(double progress, double min, double max) {
        return min + (max - min) * progress;
    }

    private double fromRangeToProgress(double value, double min, double max) {
        return ((value - min) / (max - min));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        ColorSlider slider = new ColorSlider();
        frame.add(slider);
        frame.setVisible(true);
        frame.pack();
    }
}
