package crw.ui.teleop;

import crw.Conversion;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.SwingUtilities;

/**
 * Mouse teleoperation control by dragging mouse on VelocityPanel visualization
 * 
 * mouse drag: steer rudder and adjust throttle
 * CMD + mouse click: lock rudder and thrust value with clicked value
 * 
 * @author nbb
 */
public class MouseController implements MouseListener, MouseMotionListener, TeleopSourceInt {

    // Ranges for this controller for thrust and rudder signals
    private static final double CTLR_THRUST_MIN = 0.0;
    private static final double CTLR_THRUST_MAX = 1.0;
    private static final double CTLR_THRUST_ZERO = 0.5;
    private static final double CTLR_RUDDER_MIN = 0.0;
    private static final double CTLR_RUDDER_MAX = 1.0;
    private static final double CTLR_RUDDER_CENTER = 0.5;
    VelocityPanel velocityPanel;
    double telRudderFrac = 0.5, telThrustFrac = 0;
    boolean active = false, enabled = false;

    public MouseController(VelocityPanel joystickPanel) {
        this.velocityPanel = joystickPanel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!enabled) {
            return;
        }
        telRudderFrac = Math.max(Math.min((e.getX() - velocityPanel.btmLeft.x) / velocityPanel.xAxisWidth, CTLR_RUDDER_MAX), CTLR_RUDDER_MIN);
        telThrustFrac = Math.max(Math.min((velocityPanel.btmLeft.y - e.getY()) / velocityPanel.yAxisHeight, CTLR_THRUST_MAX), CTLR_THRUST_MIN);
        double adjTelRudderFrac = Conversion.convertRange(telRudderFrac, CTLR_RUDDER_MIN, CTLR_RUDDER_MAX, VelocityPanel.VEH_RUDDER_MIN, VelocityPanel.VEH_RUDDER_MAX);
        double adjTelThrustFrac = Conversion.convertRange(telThrustFrac, CTLR_THRUST_MIN, CTLR_THRUST_MAX, VelocityPanel.VEH_THRUST_MIN, VelocityPanel.VEH_THRUST_MAX);
        velocityPanel.setVelocityFractions(adjTelRudderFrac, adjTelThrustFrac, this);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!enabled) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            velocityPanel.setVelocityFractions(telRudderFrac, telThrustFrac, true, this);
        } else {
            telRudderFrac = CTLR_RUDDER_CENTER;
            telThrustFrac = CTLR_THRUST_MIN;
            double adjTelRudderFrac = Conversion.convertRange(telRudderFrac, CTLR_RUDDER_MIN, CTLR_RUDDER_MAX, VelocityPanel.VEH_RUDDER_MIN, VelocityPanel.VEH_RUDDER_MAX);
            double adjTelThrustFrac = Conversion.convertRange(telThrustFrac, CTLR_THRUST_MIN, CTLR_THRUST_MAX, VelocityPanel.VEH_THRUST_MIN, VelocityPanel.VEH_THRUST_MAX);
            velocityPanel.setVelocityFractions(adjTelRudderFrac, adjTelThrustFrac, this);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!enabled) {
            return;
        }
        telRudderFrac = Math.max(Math.min((e.getX() - velocityPanel.btmLeft.x) / velocityPanel.xAxisWidth, CTLR_RUDDER_MAX), CTLR_RUDDER_MIN);
        telThrustFrac = Math.max(Math.min((velocityPanel.btmLeft.y - e.getY()) / velocityPanel.yAxisHeight, CTLR_THRUST_MAX), CTLR_THRUST_MIN);
        double adjTelRudderFrac = Conversion.convertRange(telRudderFrac, CTLR_RUDDER_MIN, CTLR_RUDDER_MAX, VelocityPanel.VEH_RUDDER_MIN, VelocityPanel.VEH_RUDDER_MAX);
        double adjTelThrustFrac = Conversion.convertRange(telThrustFrac, CTLR_THRUST_MIN, CTLR_THRUST_MAX, VelocityPanel.VEH_THRUST_MIN, VelocityPanel.VEH_THRUST_MAX);
        velocityPanel.setVelocityFractions(adjTelRudderFrac, adjTelThrustFrac, this);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
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
        this.enabled = enabled;
        if (!enabled) {
            setActive(false);
        }
    }
}
