package crw.ui.teleop;

import crw.Conversion;
import crw.ui.widget.RobotWidget;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.VelocityListener;
import edu.cmu.ri.crw.data.Twist;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author nbb
 */
public class VelocityPanel extends JPanel implements VelocityListener, FocusListener, MouseMotionListener {

    private static final Logger LOGGER = Logger.getLogger(VelocityPanel.class.getName());
    // Last velocity received from the boat
    double recRudderFrac, recThrustFrac, vizRecRudderFrac, vizRecThrustFrac;
    Point origin = new Point();
    Point btmLeft = new Point();
    double xAxisHalfWidth = -1, xAxisWidth = -1, yAxisHeight = -1;
    boolean teleLock = false;
    String message = null;
    Hashtable map = new Hashtable();
    boolean updateDims = true;

    // Vehicle server variables
    protected AsyncVehicleServer _vehicle = null;
    public static final int DEFAULT_UPDATE_MS = 750;
    public static final int DEFAULT_COMMAND_MS = 200;
    // Ranges for thrust and rudder signals to send to vehicle server
    public static final double VEH_THRUST_MIN = 0.0;
    public static final double VEH_THRUST_MAX = 1.0;
    public static final double VEH_THRUST_ZERO = 0.0;
    public static final double VEH_RUDDER_MIN = 1.0; // Reversed to match +Z rotation.
    public static final double VEH_RUDDER_MAX = -1.0;
    public static final double VEH_RUDDER_CENTER = 0.0;
    // Ranges for thrust and rudder signals used for visualization
    public static final double VIZ_THRUST_MIN = 0.0;
    public static final double VIZ_THRUST_MAX = 1.0;
    public static final double VIZ_RUDDER_MIN = 0.0;
    public static final double VIZ_RUDDER_MAX = 1.0;
    // Sets up a flag limiting the rate of velocity command transmission
    public AtomicBoolean _sentVelCommand = new AtomicBoolean(false);
    public AtomicBoolean _queuedVelCommand = new AtomicBoolean(false);
    protected java.util.Timer _timer = new java.util.Timer();

    boolean teleopEnabled;
    private MouseController mouseController;
    private KeyboardController keyboardController;
    private GamepadController gamepadController;
    private RobotWidget robotWidget;

    private TeleopSourceInt activeTeleopSource = null;
    private ArrayList<TeleopSourceInt> teleopSources = new ArrayList<TeleopSourceInt>();

    public VelocityPanel(RobotWidget robotWidget) {
//    public VelocityPanel(RBBak robotWidget) {
        this.robotWidget = robotWidget;
        mouseController = new MouseController(this);
        teleopSources.add(mouseController);
        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        keyboardController = new KeyboardController(this);
        teleopSources.add(keyboardController);
        addKeyListener(keyboardController);
        gamepadController = new GamepadController(this);
        teleopSources.add(gamepadController);
        map.put(TextAttribute.SIZE, new Float(18.0));
        map.put(TextAttribute.FOREGROUND, Color.RED);
        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        addMouseMotionListener(this);
        setFocusable(true);
    }

    @Override
    public void paint(java.awt.Graphics g) {
        super.paint(g);
        if (xAxisHalfWidth < 0 || xAxisWidth < 0 || yAxisHeight < 0 || updateDims) {
            updateDims();
        }
        Graphics2D g2d = (Graphics2D) g;
        if (message == null) {
            paintBackground(g2d);
            paintForce(g2d);
        } else {
            paintText(g2d);
        }
    }

    @Override
    public void setSize(Dimension d) {
        super.setSize(d);
    }

    public void updateDims() {
        Dimension d = getSize();

        int border = 30;
        origin.x = Math.max(d.width / 2, 0);
        origin.y = Math.max(d.height - border / 2, 0);
        btmLeft.x = Math.max(border / 2, 0);
        btmLeft.y = Math.max(d.height - border / 2, 0);
        xAxisHalfWidth = Math.max((d.width - border) / 2, 2);
        xAxisWidth = Math.max(d.width - border, 2);
        yAxisHeight = Math.max(d.height - border, 2);
        updateDims = false;
    }

    private void paintBackground(Graphics2D g) {
        g.setPaint(Color.RED);
        g.setStroke(new BasicStroke(.3f));
        g.drawLine(btmLeft.x, origin.y,
                btmLeft.x + (int) (xAxisWidth), origin.y);
        g.drawLine(origin.x - (int) (xAxisHalfWidth), origin.y,
                origin.x - (int) (xAxisHalfWidth), origin.y - (int) yAxisHeight);
        g.drawLine(origin.x + (int) (xAxisHalfWidth), origin.y,
                origin.x + (int) (xAxisHalfWidth), origin.y - (int) yAxisHeight);
        g.drawLine(origin.x, origin.y,
                origin.x, origin.y - (int) yAxisHeight);
    }

    private void paintForce(Graphics2D g) {
        // Operator value
        if (!teleLock) {
            g.setPaint(Color.RED);
        } else {
            g.setPaint(Color.BLUE);
        }
        double vizSendThrustFrac = Conversion.convertRange(robotWidget.telThrustFrac, VEH_THRUST_MIN, VEH_THRUST_MAX, VIZ_THRUST_MIN, VIZ_THRUST_MAX);
        double vizSendRudderFrac = Conversion.convertRange(robotWidget.telRudderFrac, VEH_RUDDER_MIN, VEH_RUDDER_MAX, VIZ_RUDDER_MIN, VIZ_RUDDER_MAX);
        g.setStroke(new BasicStroke(6.0f));
        g.drawLine(btmLeft.x + (int) (vizSendRudderFrac * xAxisWidth), btmLeft.y,
                btmLeft.x + (int) (vizSendRudderFrac * xAxisWidth), btmLeft.y - (int) (vizSendThrustFrac * yAxisHeight));
        if (!teleLock) {
            g.setPaint(Color.RED);
        } else {
            g.setPaint(Color.BLUE);
        }
        g.setStroke(new BasicStroke(2.4f));
        g.drawOval(btmLeft.x + (int) (vizSendRudderFrac * xAxisWidth) - 6,
                btmLeft.y - (int) (vizSendThrustFrac * yAxisHeight) - 6,
                12, 12);
        // Received value
        g.setPaint(Color.BLACK);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(btmLeft.x + (int) (vizRecRudderFrac * xAxisWidth), btmLeft.y,
                btmLeft.x + (int) (vizRecRudderFrac * xAxisWidth), btmLeft.y - (int) (vizRecThrustFrac * yAxisHeight));
        g.drawOval(btmLeft.x + (int) (vizRecRudderFrac * xAxisWidth) - 3,
                btmLeft.y - (int) (vizRecThrustFrac * yAxisHeight) - 3,
                6, 6);
    }

    private void paintText(Graphics2D g) {
        if (message == null) {
            return;
        }
        java.text.AttributedString mas = new java.text.AttributedString(message, map);
        Point pen = new Point(10, 20);
        FontRenderContext frc = g.getFontRenderContext();
        LineBreakMeasurer measurer = new LineBreakMeasurer(mas.getIterator(), frc);
        float wrappingWidth = getSize().width - 25;
        while (measurer.getPosition() < message.length()) {
            TextLayout layout = measurer.nextLayout(wrappingWidth);
            pen.y += (layout.getAscent());
            layout.draw(g, pen.x, pen.y);
            pen.y += layout.getDescent() + layout.getLeading();
        }
    }

    public void setVelocityFractions(double telRudderFrac, double telThrustFrac, TeleopSourceInt teleopSource) {
        setVelocityFractions(telRudderFrac, telThrustFrac, false, teleopSource);
    }

    public void setVelocityFractions(double telRudderFrac, double telThrustFrac, boolean teleLock, TeleopSourceInt teleopSource) {
        if (teleopSource != activeTeleopSource) {
            for (TeleopSourceInt source : teleopSources) {
                source.setActive(source == teleopSource);
            }
            LOGGER.fine("Changed teleop source from " + activeTeleopSource + " to " + teleopSource);
            activeTeleopSource = teleopSource;
        }
        this.teleLock = teleLock;
        robotWidget.telRudderFrac = telRudderFrac;
        robotWidget.telThrustFrac = telThrustFrac;
        robotWidget.updateVelocity();
        repaint();
    }

    /**
     * If we have a vehicle, send a zero velocity command
     */
    public void stopBoat() {
        teleLock = false;
        robotWidget.stopBoat();
        repaint();
    }

    @Override
    public void receivedVelocity(Twist twist) {
        recThrustFrac = twist.dx();
        recRudderFrac = twist.drz();
        vizRecThrustFrac = Conversion.convertRange(twist.dx(), VEH_THRUST_MIN, VEH_THRUST_MAX, VIZ_THRUST_MIN, VIZ_THRUST_MAX);
        vizRecRudderFrac = Conversion.convertRange(twist.drz(), VEH_RUDDER_MIN, VEH_RUDDER_MAX, VIZ_RUDDER_MIN, VIZ_RUDDER_MAX);
        repaint();
    }

    public void setVehicle(AsyncVehicleServer vehicle) {
        if (_vehicle != null) {
            // Remove velocity listener from previously selected proxy
            _vehicle.removeVelocityListener(this, null);
        }
        _vehicle = vehicle;
        if (_vehicle != null) {
            vehicle.addVelocityListener(this, null);
            vehicle.isAutonomous(new FunctionObserver<Boolean>() {
                @Override
                public void completed(Boolean v) {
                    enableTeleop(!v);
                }

                @Override
                public void failed(FunctionObserver.FunctionError fe) {
                    enableTeleop(false);
                }
            });
        } else {
            enableTeleop(false);
        }
    }

    public void enableTeleop(boolean enable) {
        if (this.teleopEnabled == enable) {
            // Return now to avoid adding/removing listeners multiple times
            return;
        }
        LOGGER.fine("Changed enableTeleop from " + teleopEnabled + " to " + enable);
        this.teleopEnabled = enable;
        if (teleopEnabled) {
            requestFocus();
            // Reset variables
            teleLock = false;
            robotWidget.telRudderFrac = VEH_RUDDER_CENTER;
            robotWidget.telThrustFrac = VEH_THRUST_ZERO;
            // Start listening to input devices again
            for (TeleopSourceInt source : teleopSources) {
                source.enable(true);
            }
//            if (mouseController != null) {
//                addMouseListener(mouseController);
//                addMouseMotionListener(mouseController);
//            }
//            if (keyboardController != null) {
//                // @todo window focus listener
//                addKeyListener(keyboardController);
//                keyboardController.enable(true);
//            }
//            if (gamepadController != null) {
//                ControllerDevice.getInstance().addGamepadListener(gamepadController);
//            }
        } else if (!teleopEnabled) {
            // Stop listening to input devices
            for (TeleopSourceInt source : teleopSources) {
                source.enable(false);
            }
//            if (mouseController != null) {
//                removeMouseListener(mouseController);
//                removeMouseMotionListener(mouseController);
//            }
//            if (keyboardController != null) {
//                // @todo window focus listener
//                removeKeyListener(keyboardController);
//                keyboardController.enable(false);
//            }
//            if (gamepadController != null) {
//                ControllerDevice.getInstance().removeGamepadListener(gamepadController);
//            }
            // Reset variables and send stop command
            teleLock = false;
            robotWidget.stopBoat();
        }
    }

    @Override
    public void focusGained(FocusEvent fe) {
    }

    @Override
    public void focusLost(FocusEvent fe) {
    }

    @Override
    public void mouseDragged(MouseEvent me) {
    }

    @Override
    public void mouseMoved(MouseEvent me) {
    }
}
