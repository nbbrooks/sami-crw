package crw.ui.teleop;

import crw.Conversion;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 * Keyboard teleoperation control using arrow keys with passive return to no
 * movement
 *
 * up arrow: increase thrust down arrow: decrease thrust left arrow: steer more
 * left right arrow: steer more right no input: slowly center steering and zero
 * thrust
 *
 * @author nbb
 */
public class KeyboardController implements KeyListener, TeleopSourceInt {

    private static final Logger LOGGER = Logger.getLogger(KeyboardController.class.getName());

    // How much to adjust rudder and thrust values for binary user input
    private static final double ACTIVE_AMOUNT = 0.02;
    // How much to adjust rudder and thrust values for timer input when no user input was received
    private static final double PASSIVE_AMOUNT = 0.02;
    // Ranges for this controller for thrust and rudder signals
    private static final double CTLR_THRUST_MIN = 0.0;
    private static final double CTLR_THRUST_MAX = 1.0;
    private static final double CTLR_THRUST_ZERO = 0.5;
    private static final double CTLR_RUDDER_MIN = 0.0;
    private static final double CTLR_RUDDER_MAX = 1.0;
    private static final double CTLR_RUDDER_CENTER = 0.5;
    // Used to tell the timer that a key event has already triggered a velocity update in this time period
    boolean alreadyUpdated = false;
    // Timer settings
    Timer timer;
    public static final int UPDATE_RATE = 40; // ms
    // What keys are depressed
    private final boolean[] keyArray = new boolean[4];
    VelocityPanel velocityPanel;
    double telRudderFrac = 0.5, telThrustFrac = 0;
    boolean active = false, enabled = false;

    public KeyboardController(VelocityPanel velocityPanel) {
        this.velocityPanel = velocityPanel;
        timer = new Timer(UPDATE_RATE, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                updateVelocity();
            }
        });
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        boolean modified = false;
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_UP:
                keyArray[0] = true;
                modified = true;
                break;
            case KeyEvent.VK_DOWN:
                keyArray[1] = true;
                modified = true;
                break;
            case KeyEvent.VK_LEFT:
                keyArray[2] = true;
                modified = true;
                break;
            case KeyEvent.VK_RIGHT:
                keyArray[3] = true;
                modified = true;
                break;
        }
        if (!active && modified) {
            updateVelocity();
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        boolean modified = false;
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_UP:
                keyArray[0] = false;
                modified = true;
                break;
            case KeyEvent.VK_DOWN:
                keyArray[1] = false;
                modified = true;
                break;
            case KeyEvent.VK_LEFT:
                keyArray[2] = false;
                modified = true;
                break;
            case KeyEvent.VK_RIGHT:
                keyArray[3] = false;
                modified = true;
                break;
        }
        if (!active && modified) {
            updateVelocity();
        }
    }

    public void updateVelocity() {
        // Update thrust
        if (keyArray[0] && !keyArray[1]) {
            // Up: increase velocity
            telThrustFrac = Math.min(telThrustFrac + ACTIVE_AMOUNT, CTLR_THRUST_MAX);
        } else if (keyArray[1] && !keyArray[0]) {
            // Down: Decrease velocity
            telThrustFrac = Math.max(telThrustFrac - ACTIVE_AMOUNT, CTLR_THRUST_MIN);
        } else if (!keyArray[0] && !keyArray[1]) {
            // Neither up nor down: slowly decrease velocity
            telThrustFrac = Math.max(telThrustFrac - PASSIVE_AMOUNT, CTLR_THRUST_MIN);
        }
        // Update rudder
        if (keyArray[2] && !keyArray[3]) {
            // Left: steer more to the left
            telRudderFrac = Math.max(telRudderFrac - ACTIVE_AMOUNT, CTLR_RUDDER_MIN);
        } else if (keyArray[3] && !keyArray[2]) {
            // Right: steer more to the right
            telRudderFrac = Math.min(telRudderFrac + ACTIVE_AMOUNT, CTLR_RUDDER_MAX);
        } else if (!keyArray[2] && !keyArray[3]) {
            // Neither left nor right: slowly center steering
            if (telRudderFrac < CTLR_RUDDER_CENTER) {
                telRudderFrac = Math.min(telRudderFrac + PASSIVE_AMOUNT, CTLR_RUDDER_CENTER);
            } else if (telRudderFrac > CTLR_RUDDER_CENTER) {
                telRudderFrac = Math.max(telRudderFrac - PASSIVE_AMOUNT, CTLR_RUDDER_CENTER);
            }
        }
        // Convert values to Vehicle Server range
        double adjTelRudderFrac = Conversion.convertRange(telRudderFrac, CTLR_RUDDER_MIN, CTLR_RUDDER_MAX, VelocityPanel.VEH_RUDDER_MIN, VelocityPanel.VEH_RUDDER_MAX);
        double adjTelThrustFrac = Conversion.convertRange(telThrustFrac, CTLR_THRUST_MIN, CTLR_THRUST_MAX, VelocityPanel.VEH_THRUST_MIN, VelocityPanel.VEH_THRUST_MAX);
        velocityPanel.setVelocityFractions(adjTelRudderFrac, adjTelThrustFrac, this);
    }

    @Override
    public void setActive(boolean active) {
        if (active && !timer.isRunning()) {
            timer.start();
        } else if (!active) {
            telRudderFrac = CTLR_RUDDER_CENTER;
            telThrustFrac = CTLR_THRUST_ZERO;
            if (timer.isRunning()) {
                timer.stop();
            }
        }
        this.active = active;
    }

    @Override
    public void enable(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (!enabled) {
            timer.stop();
            setActive(false);
        }
    }
}
