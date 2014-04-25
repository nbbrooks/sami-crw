package crw.ui.teleop;

/**
 *
 * @author nbb
 */
public class GamepadState {

    public static final int KEY_FIRST = 400;
    public static final int KEY_LAST = 402;
    public static final int KEY_TYPED = 400;
    public static final int KEY_PRESSED = 401;
    public static final int KEY_RELEASED = 402;
    public static final int VK_ENTER = 10;
    public static final int VK_BACK_SPACE = 8;
    // Buttons
    public final boolean triangle;
    public final boolean circle;
    public final boolean x;
    public final boolean square;

    public final boolean leftTrigger;
    public final boolean rightTrigger;

    public final boolean leftBumper;
    public final boolean rightBumper;

    public final boolean dpadUp;
    public final boolean dpadDown;
    public final boolean dpadLeft;
    public final boolean dpadRight;

    public final boolean select;
    public final boolean start;
    public final boolean ps3Button;
    // Joysticks
    public final double leftXAxis;
    public final double rightXAxis;
    public final double leftYAxis;
    public final double rightYAxis;

    public GamepadState(boolean[] buttonValues, double[] stickValues) {
        if (buttonValues.length == 15 && stickValues.length == 4) {
            // Buttons
            triangle = buttonValues[0];
            circle = buttonValues[1];
            x = buttonValues[2];
            square = buttonValues[3];
            leftTrigger = buttonValues[4];
            rightTrigger = buttonValues[5];
            leftBumper = buttonValues[6];
            rightBumper = buttonValues[7];
            dpadUp = buttonValues[8];
            dpadDown = buttonValues[9];
            dpadLeft = buttonValues[10];
            dpadRight = buttonValues[11];
            select = buttonValues[12];
            start = buttonValues[13];
            ps3Button = buttonValues[14];
            // Joysticks
            leftXAxis = stickValues[0];
            rightXAxis = stickValues[1];
            leftYAxis = stickValues[2];
            rightYAxis = stickValues[3];
        } else {
            // Error
            // Buttons
            triangle = false;
            circle = false;
            x = false;
            square = false;
            leftTrigger = false;
            rightTrigger = false;
            leftBumper = false;
            rightBumper = false;
            dpadUp = false;
            dpadDown = false;
            dpadLeft = false;
            dpadRight = false;
            select = false;
            start = false;
            ps3Button = false;
            // Joysticks
            leftXAxis = 0.0;
            rightXAxis = 0.0;
            leftYAxis = 0.0;
            rightYAxis = 0.0;
        }
    }
}
