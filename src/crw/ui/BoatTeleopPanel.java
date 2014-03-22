package crw.ui;

import crw.proxy.BoatProxy;
import crw.ui.teleop.GainsPanel;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.FunctionObserver.FunctionError;
import edu.cmu.ri.crw.VelocityListener;
import edu.cmu.ri.crw.data.Twist;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.awt.font.*;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 *
 * @author Jijun Wang
 */
public class BoatTeleopPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(BoatTeleopPanel.class.getName());
    // shared data in data manager
    protected JButton jAuto;
    protected GainsPanel gainsPanel;
    protected JoystickPanel jJoystick;
    protected JButton autoButton;
    protected BoatProxy activeProxy = null;
    protected AsyncVehicleServer activeVehicle = null;
    public static final int DEFAULT_UPDATE_MS = 750;
    public static final int DEFAULT_COMMAND_MS = 200;
    // Ranges for thrust and rudder signals
    public static final double THRUST_MIN = 0.0;
    public static final double THRUST_MAX = 1.0;
    public static final double RUDDER_MIN = 1.0;
    public static final double RUDDER_MAX = -1.0;
    // Sets up a flag limiting the rate of velocity command transmission
    private AtomicBoolean _sentVelCommand = new AtomicBoolean(false);
    private AtomicBoolean _queuedVelCommand = new AtomicBoolean(false);
    private java.util.Timer _timer = new java.util.Timer();
    private VelocityListener velocityListener = null;

    public BoatTeleopPanel(JButton autoButton) {
        setLayout(new BorderLayout());

        jAuto = new JButton();
        jAuto.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                toggleAutonomous();
                jJoystick.teleLock = false;
            }
        });

        gainsPanel = new GainsPanel();
        jJoystick = new JoystickPanel();
        jJoystick.addMouseListener(jJoystick);
        jJoystick.addMouseMotionListener(jJoystick);
        this.autoButton = autoButton;
        autoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                toggleAutonomous();
            }
        });
        this.add(jJoystick, java.awt.BorderLayout.CENTER);
        this.add(gainsPanel, java.awt.BorderLayout.SOUTH);
        this.addComponentListener(new ResizeListener());
    }

    @Override
    public void setBounds(java.awt.Rectangle r) {
        super.setBounds(r);
        adjustSize(r.getSize());
    }

    public void toggleAutonomous() {
        if (activeVehicle != null) {
            activeVehicle.isAutonomous(new FunctionObserver<Boolean>() {
                @Override
                public void completed(Boolean v) {
                    final boolean enableAutonomy = !v;
                    if (enableAutonomy) {
                        stopTeleop();
                    }
                    activeVehicle.setAutonomous(enableAutonomy, new FunctionObserver<Void>() {
                        public void completed(Void v) {
                            autoButton.setSelected(enableAutonomy);
                        }

                        public void failed(FunctionObserver.FunctionError fe) {
                        }
                    });
                }

                @Override
                public void failed(FunctionError fe) {
                }
            });
        }
    }

    public void setAutonomous(final boolean enableAutonomy) {
        if (activeVehicle != null) {
            activeVehicle.isAutonomous(new FunctionObserver<Boolean>() {
                @Override
                public void completed(Boolean v) {
                    if (enableAutonomy == v) {
                        // Already in this desired mode
                        return;
                    }
                    if (enableAutonomy) {
                        stopTeleop();
                    }
                    activeVehicle.setAutonomous(enableAutonomy, new FunctionObserver<Void>() {
                        public void completed(Void v) {
                            autoButton.setSelected(enableAutonomy);
                        }

                        public void failed(FunctionObserver.FunctionError fe) {
                        }
                    });
                }

                @Override
                public void failed(FunctionError fe) {
                }
            });
        }
    }
    // Callback that handles GUI events that change velocity

    protected void updateVelocity() {
        // Check if there is already a command queued up, if not, queue one up
        if (!_sentVelCommand.getAndSet(true)) {

            // Send one command immediately
            sendVelocity();

            // Queue up a command at the end of the refresh timestep
            _timer.schedule(new UpdateVelTask(), DEFAULT_COMMAND_MS);
        } else {
            _queuedVelCommand.set(true);
        }
    }

    // Simple update task that periodically checks whether velocity needs updating
    class UpdateVelTask extends TimerTask {

        @Override
        public void run() {
            if (_queuedVelCommand.getAndSet(false)) {
                sendVelocity();
                _timer.schedule(new UpdateVelTask(), DEFAULT_COMMAND_MS);
            } else {
                _sentVelCommand.set(false);
            }
        }
    }

    // Sets velocities from sliders to control proxy
    protected void sendVelocity() {
        if (activeVehicle != null) {
            Twist twist = new Twist();
            double dx = fromProgressToRange(jJoystick.telThrustFrac, THRUST_MIN, THRUST_MAX, Double.valueOf(gainsPanel.velocityMultF.getText()));
            double drz = fromProgressToRange(jJoystick.telRudderFrac, RUDDER_MIN, RUDDER_MAX, Double.valueOf(gainsPanel.velocityMultF.getText()));
            constrainToCircle(twist, dx, drz, Double.valueOf(gainsPanel.velocityMult));
//            System.out.println("### Twist: " + twist.toString());
            activeVehicle.setVelocity(twist, new FunctionObserver<Void>() {
                public void completed(Void v) {
                    LOGGER.fine("Set velocity succeeded");
                }

                public void failed(FunctionError fe) {
                    LOGGER.fine("Set velocity failed");
                }
            });
        }
    }

    // Converts from progress bar value to linear scaling between min and max
    private void constrainToCircle(Twist twist, double dx, double drz, double radius) {
        double length = Math.sqrt(Math.pow(dx, 2) + Math.pow(drz, 2));
        double div = length / radius;
        if (div > 1) {
            double angle = Math.atan2(drz, dx);
            double newDx = radius * Math.cos(angle);
            double newDrz = radius * Math.sin(angle);
            twist.dx(newDx);
            twist.drz(newDrz);
        } else {
            twist.dx(dx);
            twist.drz(drz);
        }
    }

    // Converts from progress bar value to linear scaling between min and max
    private double fromProgressToRange(double progress, double min, double max, double gain) {
        return (min + (max - min) * progress) * gain;
    }

    // Converts to progress bar value from linear scaling between min and max
    private double fromRangeToProgress(double value, double min, double max) {
        return ((value - min) / (max - min));
    }

    public void setProxy(BoatProxy boatProxy) {
        if (activeProxy == boatProxy) {
            return;
        }
        if (activeVehicle != null) {
            // Remove velocity listener from previously selected proxy
            activeVehicle.removeVelocityListener(velocityListener, null);
        }

        activeProxy = boatProxy;
        if (activeProxy != null) {
            activeVehicle = boatProxy.getVehicleServer();
            velocityListener = new VelocityListener() {
                public void receivedVelocity(Twist twist) {
                    jJoystick.recThrustFrac = fromRangeToProgress(twist.dx(), THRUST_MIN, THRUST_MAX);
                    jJoystick.recRudderFrac = fromRangeToProgress(twist.drz(), RUDDER_MIN, RUDDER_MAX);
                    jJoystick.repaint();
                }
            };
            activeVehicle.addVelocityListener(velocityListener, null);
            activeVehicle.isAutonomous(new FunctionObserver<Boolean>() {
                @Override
                public void completed(Boolean v) {
                    autoButton.setSelected(v);
                }

                @Override
                public void failed(FunctionError fe) {
                }
            });
        } else {
            activeVehicle = null;
            autoButton.setSelected(false);
        }
        gainsPanel.setProxy(boatProxy);
    }

    public void stopTeleop() {
        jJoystick.teleLock = false;
        jJoystick.telRudderFrac = 0.5;
        jJoystick.telThrustFrac = 0;
        if (activeVehicle != null) {
            sendVelocity();
        }
        jJoystick.repaint();
    }

    public void adjustSize(Dimension dim) {
        Dimension d = new java.awt.Dimension(dim.width - 10, dim.height - 10);
        jJoystick.setPreferredSize(d);
        jJoystick.setSize(d);
        jJoystick.updateDims = true;
    }

    public class JoystickPanel extends JPanel implements MouseListener, MouseMotionListener {

        double telRudderFrac = 0.5, telThrustFrac = 0, radius = -1, radius1 = 0, radius2 = 0;
        double recRudderFrac, recThrustFrac;
        double scale = 1, leftVel = 0, rightVel = 0;
        Point origin = new Point();
        Point btmLeft = new Point();
        double xAxisHalfWidth = -1, xAxisWidth = -1, yAxisHeight = -1;
        boolean teleActive = false, teleLock = false;
        String message = null;
        Hashtable map = new Hashtable();
        boolean updateDims = true;

        public JoystickPanel() {
            map.put(TextAttribute.SIZE, new Float(18.0));
            map.put(TextAttribute.FOREGROUND, Color.RED);
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
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
            g.setStroke(new BasicStroke(6.0f));
            g.drawLine(btmLeft.x + (int) (telRudderFrac * xAxisWidth), btmLeft.y,
                    btmLeft.x + (int) (telRudderFrac * xAxisWidth), btmLeft.y - (int) (telThrustFrac * yAxisHeight));
            if (!teleLock) {
                g.setPaint(Color.RED);
            } else {
                g.setPaint(Color.BLUE);
            }
            g.setStroke(new BasicStroke(2.4f));
            g.drawOval(btmLeft.x + (int) (telRudderFrac * xAxisWidth) - 6,
                    btmLeft.y - (int) (telThrustFrac * yAxisHeight) - 6,
                    12, 12);
            // Received value
            g.setPaint(Color.BLACK);
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(btmLeft.x + (int) (recRudderFrac * xAxisWidth), btmLeft.y,
                    btmLeft.x + (int) (recRudderFrac * xAxisWidth), btmLeft.y - (int) (telThrustFrac * yAxisHeight));
            g.drawOval(btmLeft.x + (int) (recRudderFrac * xAxisWidth) - 3,
                    btmLeft.y - (int) (recThrustFrac * yAxisHeight) - 3,
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
            if (autoButton.isSelected()) {
                setAutonomous(false);
            }
            teleActive = true;
            teleLock = false;
            telRudderFrac = Math.max(Math.min((e.getX() - btmLeft.x) / xAxisWidth, 1.0), 0.0);
            telThrustFrac = Math.max(Math.min((btmLeft.y - e.getY()) / yAxisHeight, 1.0), 0.0);
            sendVelocity();
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (autoButton.isSelected()) {
                return;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                teleLock = true;
            } else {
                teleActive = false;
                telRudderFrac = 0.5;
                telThrustFrac = 0;
                sendVelocity();
            }
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (autoButton.isSelected()) {
                return;
            }
            teleActive = true;
            telRudderFrac = Math.max(Math.min((e.getX() - btmLeft.x) / xAxisWidth, 1.0), 0.0);
            telThrustFrac = Math.max(Math.min((btmLeft.y - e.getY()) / yAxisHeight, 1.0), 0.0);
            sendVelocity();
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }

    class ResizeListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            jJoystick.updateDims = true;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.add(new BoatTeleopPanel(new JButton()));
        frame.setVisible(true);

//        double temp = -1;
//        System.out.println(temp);
//        try {
//            temp = Double.valueOf("");
//        } catch (NumberFormatException ex) {
//            System.out.println("Invalid #");
//        }
//        System.out.println(temp);
    }
}
