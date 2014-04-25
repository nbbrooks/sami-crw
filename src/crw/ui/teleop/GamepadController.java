package crw.ui.teleop;

import crw.Conversion;
import java.util.logging.Logger;

/**
 * Joystick teleoperation control designed for a PS3 controller
 * 
 * sticks: steer rudder and adjust throttle
 * left trigger: lock thrust
 * left bumper: serpentine
 * right trigger: increase thrust up to 40%
 * right bumper: increase thrust up to 80%
 * 
 * @author nbb
 */
public class GamepadController implements GamepadListener, TeleopSourceInt {

    private static final Logger LOGGER = Logger.getLogger(GamepadController.class.getName());

    // How much to adjust rudder and thrust values for binary user input
    private static final double ACTIVE_AMOUNT = 0.02;
    // How much to adjust rudder and thrust values for timer input when no user input was received
    private static final double PASSIVE_AMOUNT = 0.02;
    // Ranges for this controller for thrust and rudder signals
    private static final double CTLR_THRUST_MIN = 0.0;
    private static final double CTLR_THRUST_MAX = 1.0;
    private static final double CTLR_THRUST_ZERO = 0.5;
    private static final double CTLR_RUDDER_MIN = -1.0;
    private static final double CTLR_RUDDER_MAX = 1.0;
    private static final double CTLR_RUDDER_CENTER = 0.0;
    // Maximum thrust that can be sent using the right trigger
    private static final double JOYSTICK_X_DEAD_ZONE = 0.3;
    private static final double JOYSTICK_Y_DEAD_ZONE = 0.3;
    // Maximum thrust that can be sent using either of the analog joysticks
    private static final double JOYSTICK_MAX_THRUST = 0.4;
    // Maximum thrust that can be sent using the right trigger
    private static final double R_TRIGGER_MAX_THRUST = 0.4;
    // Maximum thrust that can be sent using the right bumper
    private static final double R_BUMPER_MAX_THRUST = 0.8;
    // Which analog joystick is being used for teleoperation
    private boolean useLeftStick = true;
    // Which direction serpentine motion should move in
    private boolean serpentineLeft = false;
    VelocityPanel velocityPanel;
    boolean active = false, enabled = false;
    double telRudderFrac = 0.0, telThrustFrac = 0.0;

    public GamepadController(VelocityPanel velocityPanel) {
        this.velocityPanel = velocityPanel;
    }

    @Override
    public void gamepadUpdate(GamepadState ge) {
        if (!enabled) {
            return;
        }
        boolean receivedThrustInput = false;
        boolean receivedRudderInput = false;

        // Check to see if active joystick has swapped
        if (!useLeftStick && (Math.abs(ge.leftXAxis) > JOYSTICK_X_DEAD_ZONE || Math.abs(ge.leftYAxis) > JOYSTICK_X_DEAD_ZONE)) {
            useLeftStick = true;
        } else if (useLeftStick && (Math.abs(ge.rightXAxis) > JOYSTICK_Y_DEAD_ZONE || Math.abs(ge.rightYAxis) > JOYSTICK_Y_DEAD_ZONE)) {
            useLeftStick = false;
        }

        // Get new thrust value
        if (ge.leftTrigger) {
            // Lock value
            receivedThrustInput = true;
        } else if (ge.rightBumper) {
            // Increase thrust up to 80%
            telThrustFrac = Math.min(telThrustFrac + ACTIVE_AMOUNT, R_BUMPER_MAX_THRUST);
            receivedThrustInput = true;

        } else if (ge.rightTrigger) {
            // Increase thrust up to 40%
            telThrustFrac = Math.min(telThrustFrac + ACTIVE_AMOUNT, R_TRIGGER_MAX_THRUST);
            receivedThrustInput = true;
        } else if (useLeftStick) {
            // Y axis is flipped
            if (ge.leftYAxis > JOYSTICK_Y_DEAD_ZONE) {
                telThrustFrac = Math.max(telThrustFrac - ACTIVE_AMOUNT, CTLR_THRUST_MIN);
                receivedThrustInput = true;
            } else if (ge.leftYAxis < -JOYSTICK_Y_DEAD_ZONE) {
                telThrustFrac = Math.min(telThrustFrac + ACTIVE_AMOUNT, JOYSTICK_MAX_THRUST);
                receivedThrustInput = true;
            }
        } else if (!useLeftStick) {
            // Y axis is flipped
            if (ge.rightYAxis > JOYSTICK_Y_DEAD_ZONE) {
                telThrustFrac = Math.max(telThrustFrac - ACTIVE_AMOUNT, CTLR_THRUST_MIN);
                receivedThrustInput = true;
            } else if (ge.rightYAxis < -JOYSTICK_Y_DEAD_ZONE) {
                telThrustFrac = Math.min(telThrustFrac + ACTIVE_AMOUNT, JOYSTICK_MAX_THRUST);
                receivedThrustInput = true;
            }
        }

        // Get new rudder value
        if (useLeftStick) {
            if (ge.leftXAxis > JOYSTICK_X_DEAD_ZONE) {
                telRudderFrac = Math.min(telRudderFrac + ACTIVE_AMOUNT, CTLR_RUDDER_MAX);
                receivedRudderInput = true;
            } else if (ge.leftXAxis < -JOYSTICK_X_DEAD_ZONE) {
                telRudderFrac = Math.max(telRudderFrac - ACTIVE_AMOUNT, CTLR_RUDDER_MIN);
                receivedRudderInput = true;
            }
        } else if (!useLeftStick) {
            if (ge.rightXAxis > JOYSTICK_X_DEAD_ZONE) {
                telRudderFrac = Math.min(telRudderFrac + ACTIVE_AMOUNT, CTLR_RUDDER_MAX);
                receivedRudderInput = true;
            } else if (ge.rightXAxis < -JOYSTICK_X_DEAD_ZONE) {
                telRudderFrac = Math.max(telRudderFrac - ACTIVE_AMOUNT, CTLR_RUDDER_MIN);
                receivedRudderInput = true;
            }
        }

        // Check for serpentine - only activate if it is the only rudder input
        if (ge.leftBumper && !receivedRudderInput) {
            receivedRudderInput = true;
            if (serpentineLeft) {
                // Turn to the left
                telRudderFrac = Math.max(telRudderFrac - ACTIVE_AMOUNT, CTLR_RUDDER_MIN);
                if (telRudderFrac == -1.0) {
                    serpentineLeft = false;
                }
            } else {
                telRudderFrac = Math.min(telRudderFrac + ACTIVE_AMOUNT, CTLR_RUDDER_MAX);
                if (telRudderFrac == 1.0) {
                    serpentineLeft = true;
                }
            }
        }

        // If nothing has modified thrust, decrement it
        if (!receivedThrustInput) {
            telThrustFrac = Math.max(telThrustFrac - PASSIVE_AMOUNT, CTLR_THRUST_MIN);
        }
        // If nothing has modified rudder, center it
        if (!receivedRudderInput) {
            if (telRudderFrac < 0) {
                // Rudder is steered left
                telRudderFrac = Math.min(telRudderFrac + PASSIVE_AMOUNT, CTLR_RUDDER_CENTER);
            } else if (telRudderFrac > 0) {
                // Rudder is steered right
                telRudderFrac = Math.max(telRudderFrac - PASSIVE_AMOUNT, CTLR_RUDDER_CENTER);
            }
        }
        if (active || (!active && (receivedRudderInput || receivedThrustInput))) {
            double adjTelRudderFrac = Conversion.convertRange(telRudderFrac, CTLR_RUDDER_MIN, CTLR_RUDDER_MAX, VelocityPanel.VEH_RUDDER_MIN, VelocityPanel.VEH_RUDDER_MAX);
            double adjTelThrustFrac = Conversion.convertRange(telThrustFrac, CTLR_THRUST_MIN, CTLR_THRUST_MAX, VelocityPanel.VEH_THRUST_MIN, VelocityPanel.VEH_THRUST_MAX);
            velocityPanel.setVelocityFractions(adjTelRudderFrac, adjTelThrustFrac, this);
        }
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            telRudderFrac = CTLR_RUDDER_CENTER;
            telThrustFrac = CTLR_THRUST_ZERO;
        }
    }

    @Override
    public void enable(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            ControllerDevice.getInstance().addGamepadListener(this);
        } else {
            ControllerDevice.getInstance().removeGamepadListener(this);
            setActive(false);
        }
    }
}
