package crw.ui.teleop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.Timer;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;
import net.java.games.input.DirectAndRawInputEnvironmentPlugin;

/**
 *
 * @author nbb
 */
public class ControllerDevice implements ControllerListener, Runnable {

    private final static Logger LOGGER = Logger.getLogger(ControllerDevice.class.getName());

    private Controller gamepad;
    // Buttons
    private Component triangle;
    private Component circle;
    private Component x;
    private Component square;
    private Component leftTrigger;
    private Component rightTrigger;
    private Component leftBumper;
    private Component rightBumper;
    private Component dpadUp;
    private Component dpadDown;
    private Component dpadLeft;
    private Component dpadRight;
    private Component select;
    private Component start;
    private Component ps3Button;
    // Joysticks
    private Component leftXAxis;
    private Component rightXAxis;
    private Component leftYAxis;
    private Component rightYAxis;

    ArrayList<GamepadListener> listeners = new ArrayList<GamepadListener>();
    Timer timer;
    public static final int POLL_RATE = 40; // ms
    public static final int POLLS_BEFORE_CONNECT = 10;
    int pollCount = 0;
    private boolean gamepadConnected = false;

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static class ControllerHolder {

        public static final ControllerDevice INSTANCE = new ControllerDevice();
    }

    public static ControllerDevice getInstance() {
        return ControllerHolder.INSTANCE;
    }

    private ControllerDevice() {
        init();
    }

    private void init() {
        // This publisher does not actually work
        ControllerEnvironment.getDefaultEnvironment().addControllerListener(this);

        // This is what we would like to do - check constantly for new gamepads, however
        // The ControllerListener publisher does not work
        // ControllerEnvironment.getDefaultEnvironment().getControllers() never gets refreshed
        // DirectAndRawInputEnvironmentPlugin always returns a blank list
//        timer = new Timer(POLL_RATE, new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent ae) {
//                checkIfConnected();
//                if (!gamepadConnected) {
//                    // If not connected, try to connect
//                    findAndConnectController();
//                } else {
//                    // If connected, send out a GamepadEvent
//                    publishUpdate();
//                }
//            }
//        });
//        timer.start();
        // Instead, just try to connect a controller once
        findAndConnectController();
        if (gamepadConnected) {
            // Poll quickly for data
            timer = new Timer(POLL_RATE, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    checkIfConnected();
                    if (!gamepadConnected) {
                        // If we lose the connection we can't reconnect, so just stop the timer
                        timer.stop();
                        LOGGER.info("Lost connection to controller");
                    } else {
                        // checkIfConnected polls the gamepad
                        publishUpdate();
                    }
                }
            });
            timer.start();
        }
    }

    private void publishUpdate() {
        GamepadState ge = getCurrentState();
        for (GamepadListener listener : listeners) {
            listener.gamepadUpdate(ge);
        }
    }

    private GamepadState getCurrentState() {
        boolean[] buttons = new boolean[]{
            isTrianglePressed(),
            isCirclePressed(),
            isXPressed(),
            isSquarePressed(),
            isLeftTriggerPressed(),
            isRightTriggerPressed(),
            isLeftBumperPressed(),
            isRightBumperPressed(),
            isDpadUpPressed(),
            isDpadDownPressed(),
            isDpadLeftPressed(),
            isDpadRightPressed(),
            isSelectPressed(),
            isStartPressed(),
            isPs3ButtonPressed()
        };
        double[] stickValues = new double[]{
            getLeftXAxis(),
            getRightXAxis(),
            getLeftYAxis(),
            getRightYAxis()
        };
        return new GamepadState(buttons, stickValues);
    }

    private boolean checkIfConnected() {
        try {
            if (!gamepadConnected && gamepad != null && gamepad.poll()) {
                gamepadConnected = true;
            } else if (gamepadConnected && gamepad == null) {
                gamepadConnected = false;
            } else if (gamepadConnected && gamepad != null) {
                // Try to poll, if we can everything is fine
                gamepad.poll();
            }
        } catch (Exception ex) {
            // Poll failed
            gamepadConnected = false;
        }
        return gamepadConnected;
    }

    private boolean findAndConnectController() {
        LOGGER.info("Searching for controller");

        gamepad = null;
        DirectAndRawInputEnvironmentPlugin d = new DirectAndRawInputEnvironmentPlugin();
        for (Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            if (c.getType() == Controller.Type.STICK) {
                LOGGER.info("Found controller: " + c.getName());
                gamepad = c;
            }
        }
        if (gamepad == null) {
            LOGGER.info("No controller found");
            gamepadConnected = false;
        } else {
            LOGGER.info("Connecting to controller " + gamepad.getType().toString());

            triangle = gamepad.getComponent(Component.Identifier.Button._12); //triangle
            circle = gamepad.getComponent(Component.Identifier.Button._13); //circle 
            x = gamepad.getComponent(Component.Identifier.Button._14); //xbutton 
            square = gamepad.getComponent(Component.Identifier.Button._15); //square

            leftXAxis = gamepad.getComponent(Component.Identifier.Axis.X); //x axis for first joy stick (left)
            leftYAxis = gamepad.getComponent(Component.Identifier.Axis.Y); //y axis for second joy stick (left)
            rightXAxis = gamepad.getComponent(Component.Identifier.Axis.Z); //x axis for second joystick (right)
            rightYAxis = gamepad.getComponent(Component.Identifier.Axis.RZ); //y axis for second joystick (right)

            rightTrigger = gamepad.getComponent(Component.Identifier.Button._9); //right trigger
            leftTrigger = gamepad.getComponent(Component.Identifier.Button._8); //left trigger

            leftBumper = gamepad.getComponent(Component.Identifier.Button._10); //left bumper 
            rightBumper = gamepad.getComponent(Component.Identifier.Button._11); //right bumper 

            dpadUp = gamepad.getComponent(Component.Identifier.Button._4); //dpad up
            dpadRight = gamepad.getComponent(Component.Identifier.Button._5); //dpad right
            dpadDown = gamepad.getComponent(Component.Identifier.Button._6); //dpad down
            dpadLeft = gamepad.getComponent(Component.Identifier.Button._7); //dpad left

            select = gamepad.getComponent(Component.Identifier.Button._0); //select button
            start = gamepad.getComponent(Component.Identifier.Button._3); //start button 
            ps3Button = gamepad.getComponent(Component.Identifier.Button._16); //ps3 button (dont use)

            gamepadConnected = true;
        }
        return gamepadConnected;
    }

    public void addGamepadListener(GamepadListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        } else {
            LOGGER.warning("Tried to add a GamepadListener twice");
        }
    }

    public void removeGamepadListener(GamepadListener listener) {
        boolean success = listeners.remove(listener);
        if (!success) {
            LOGGER.warning("Tried to remove a nonexistent GamepadListener");
        }
    }

    private boolean isTrianglePressed() //checks to see if triangle is pressed
    {
        if (triangle.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCirclePressed() //checks to see if circle is pressed
    {
        if (circle.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isXPressed() //checks to see if x is pressed
    {
        if (x.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSquarePressed() //checks to see if square is pressed
    {
        return Float.compare(square.getPollData(), 0.0f) != 0;
    }

    //triggers
    private boolean isRightTriggerPressed() {
        if (rightTrigger.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLeftTriggerPressed() {
        if (leftTrigger.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    //dpad controls
    private boolean isDpadUpPressed() {
        return dpadUp.getPollData() != 0.0f;
    }

    private boolean isDpadRightPressed() {
        if (dpadRight.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDpadDownPressed() {
        if (dpadDown.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDpadLeftPressed() {
        if (dpadLeft.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    //center buttons
    private boolean isSelectPressed() {
        if (select.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isStartPressed() {
        if (start.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isPs3ButtonPressed() {
        if (ps3Button.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    //bumpers
    private boolean isLeftBumperPressed() {
        if (leftBumper.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isRightBumperPressed() {
        if (rightBumper.getPollData() != 0.0f) {
            return true;
        } else {
            return false;
        }
    }

    // joystick
    private double getLeftXAxis() //returns a double with the value of the X-Axis on the left joystick
    {
        return leftXAxis.getPollData();
    }

    private double getLeftYAxis() //returns a double with the value of the Y-Axis on the left joystick
    {
        return leftYAxis.getPollData();
    }

    private double getRightXAxis() //returns a double with the value of the X-Axis on the right joystick
    {
        return rightXAxis.getPollData();
    }

    private double getRightYAxis() //returns a double with the value of the Y-Axis the on right joystick
    {
        return rightYAxis.getPollData();
    }

    @Override
    public void controllerAdded(ControllerEvent ev) {
        // This function does not work
    }

    @Override
    public void controllerRemoved(ControllerEvent ev) {
        // This function does not work
    }
}
