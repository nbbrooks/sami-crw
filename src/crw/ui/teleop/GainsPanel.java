package crw.ui.teleop;

import crw.proxy.BoatProxy;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.FunctionObserver;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import sami.engine.Engine;
import sami.sensor.Observation;
import sami.sensor.ObservationListenerInt;
import sami.sensor.ObserverInt;

/**
 *
 * @author nbb
 */
public class GainsPanel extends JScrollPane implements ObservationListenerInt {

    private static final Logger LOGGER = Logger.getLogger(GainsPanel.class.getName());
    public static final int THRUST_GAINS_AXIS = 0;
    public static final int RUDDER_GAINS_AXIS = 5;
    public static final int WINCH_GAINS_AXIS = 3;
    private JPanel contentP, velMultP, thrustPidP, rudderPidP, winchPidP;
    public JTextField velocityMultF, winchTF, thrustPTF, thrustITF, thrustDTF, rudderPTF, rudderITF, rudderDTF;
    public JLabel winchL;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    public JButton applyB;
    public double velocityMult = 0.12, winch, thrustP, thrustI, thrustD, rudderP, rudderI, rudderD;
    private BoatProxy activeProxy = null;
    private AsyncVehicleServer activeVehicle = null;
    private ObserverInt activeWinchObserver = null;

    public GainsPanel() {
        super();
        velocityMultF = new JTextField(velocityMult + "");
        velocityMultF.setPreferredSize(new Dimension(50, velocityMultF.getPreferredSize().height));
        winchTF = new JTextField("");
        winchTF.setPreferredSize(new Dimension(50, winchTF.getPreferredSize().height));
        thrustPTF = new JTextField("");
        thrustPTF.setPreferredSize(new Dimension(50, thrustPTF.getPreferredSize().height));
        thrustITF = new JTextField("");
        thrustITF.setPreferredSize(new Dimension(50, thrustITF.getPreferredSize().height));
        thrustDTF = new JTextField("");
        thrustDTF.setPreferredSize(new Dimension(50, thrustDTF.getPreferredSize().height));
        rudderPTF = new JTextField("");
        rudderPTF.setPreferredSize(new Dimension(50, rudderPTF.getPreferredSize().height));
        rudderITF = new JTextField("");
        rudderITF.setPreferredSize(new Dimension(50, rudderITF.getPreferredSize().height));
        rudderDTF = new JTextField("");
        rudderDTF.setPreferredSize(new Dimension(50, rudderDTF.getPreferredSize().height));

        velMultP = new JPanel();
        velMultP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        velMultP.add(new JLabel("Velocity multiplier:"));
        velMultP.add(velocityMultF);

        thrustPidP = new JPanel();
        thrustPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        thrustPidP.add(new JLabel("Thrust "));
        thrustPidP.add(new JLabel("P:"));
        thrustPidP.add(thrustPTF);
        thrustPidP.add(new JLabel("I:"));
        thrustPidP.add(thrustITF);
        thrustPidP.add(new JLabel("D:"));
        thrustPidP.add(thrustDTF);

        rudderPidP = new JPanel();
        rudderPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        rudderPidP.add(new JLabel("Rudder"));
        rudderPidP.add(new JLabel("P:"));
        rudderPidP.add(rudderPTF);
        rudderPidP.add(new JLabel("I:"));
        rudderPidP.add(rudderITF);
        rudderPidP.add(new JLabel("D:"));
        rudderPidP.add(rudderDTF);

        winchPidP = new JPanel();
        winchPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        winchL = new JLabel("Winch value: ---");
        winchPidP.add(winchL);
        winchPidP.add(winchTF);

        applyB = new JButton("Apply");
        applyB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                applyFieldValues();
            }
        });

        contentP = new JPanel();
        contentP.setLayout(new BoxLayout(contentP, BoxLayout.Y_AXIS));
//        contentP.add(velMultP);
        contentP.add(thrustPidP);
        contentP.add(rudderPidP);
        contentP.add(winchPidP);
        contentP.add(applyB);
        getViewport().add(contentP);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }

    public double stringToDouble(String text) {
        double ret = Double.NaN;
        try {
            ret = Double.valueOf(text);
        } catch (NumberFormatException ex) {
        }
        return ret;
    }

    public void applyFieldValues() {
        if (activeVehicle == null) {
            LOGGER.warning("Tried to apply field values to a null vehicle server!");
            return;
        }
        boolean winchChanged = false;
        boolean thrustPidChanged = false;
        boolean rudderPidChanged = false;
        double temp;

        // Thrust PID
        temp = stringToDouble(thrustPTF.getText());
        if (temp != thrustP) {
            thrustP = temp;
            thrustPidChanged = true;
        }
        temp = stringToDouble(thrustITF.getText());
        if (temp != thrustI) {
            thrustI = temp;
            thrustPidChanged = true;
        }
        temp = stringToDouble(thrustDTF.getText());
        if (temp != thrustD) {
            thrustD = temp;
            thrustPidChanged = true;
        }
        // Rudder PID
        temp = stringToDouble(rudderPTF.getText());
        if (temp != rudderP) {
            rudderP = temp;
            rudderPidChanged = true;
        }
        temp = stringToDouble(rudderITF.getText());
        if (temp != rudderI) {
            rudderI = temp;
            rudderPidChanged = true;
        }
        temp = stringToDouble(rudderDTF.getText());
        if (temp != rudderD) {
            rudderD = temp;
            rudderPidChanged = true;
        }
        // Winch
        temp = stringToDouble(winchTF.getText());
        if (temp != winch) {
            winch = temp;
            winchChanged = true;
        }

        if (thrustPidChanged) {
            // Send value
            activeVehicle.setGains(THRUST_GAINS_AXIS, new double[]{thrustP, thrustI, thrustD}, new FunctionObserver<Void>() {
                public void completed(Void v) {
                    System.out.println("Set thrust gains succeeded");
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    System.out.println("Set thrust gains failed");
                }
            });
        }
        if (rudderPidChanged) {
            // Send value
            activeVehicle.setGains(RUDDER_GAINS_AXIS, new double[]{rudderP, rudderI, rudderD}, new FunctionObserver<Void>() {
                public void completed(Void v) {
                    System.out.println("Set rudder gains succeeded");
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    System.out.println("Set rudder gains failed");
                }
            });
        }
        if (winchChanged) {
            // Send value
            activeVehicle.setGains(WINCH_GAINS_AXIS, new double[]{winch, winch, winch}, new FunctionObserver<Void>() {
                public void completed(Void v) {
                    System.out.println("Set winch gains succeeded");
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    System.out.println("Set winch gains failed");
                }
            });
        }
    }

    public void setProxy(BoatProxy boatProxy) {
        if (activeProxy == boatProxy) {
            return;
        }
        // Stop listening to the old vehicle
        if (activeWinchObserver != null) {
            activeWinchObserver.removeListener(this);
        }

        activeProxy = boatProxy;
        if (activeProxy != null) {
            activeVehicle = boatProxy.getVehicleServer();
            // Retrieve vehicle specific values
            //@todo Ideally we would only do this if the teleop panel is opened
            // Thrust gains
            activeVehicle.getGains(THRUST_GAINS_AXIS, new FunctionObserver<double[]>() {
                public void completed(double[] values) {
                    LOGGER.log(Level.FINE, "Get thrust gains succeeded");
                    thrustPTF.setText("" + values[0]);
                    thrustITF.setText("" + values[1]);
                    thrustDTF.setText("" + values[2]);
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    LOGGER.severe("Get thrust gains failed");
                }
            });
            // Rudder gains
            activeVehicle.getGains(RUDDER_GAINS_AXIS, new FunctionObserver<double[]>() {
                public void completed(double[] values) {
                    LOGGER.log(Level.FINE, "Get rudder gains succeeded");
                    rudderPTF.setText("" + values[0]);
                    rudderITF.setText("" + values[1]);
                    rudderDTF.setText("" + values[2]);
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    LOGGER.severe("Get rudder gains failed");
                }
            });
            // Winch
            activeVehicle.getGains(WINCH_GAINS_AXIS, new FunctionObserver<double[]>() {
                public void completed(double[] values) {
                    LOGGER.log(Level.FINE, "Get winch gains succeeded");
                    winchTF.setText("" + values[0]);
                }

                public void failed(FunctionObserver.FunctionError fe) {
                    LOGGER.severe("Get winch gains failed");
                }
            });
            winchL.setText("Winch value: ---");
            activeWinchObserver = Engine.getInstance().getObserverServer().getObserver(activeProxy, WINCH_GAINS_AXIS);
            activeWinchObserver.addListener(this);

            applyB.setEnabled(true);
        } else {
            activeVehicle = null;
            // No vehicle selected, blank out text fields
            thrustPTF.setText("");
            thrustITF.setText("");
            thrustDTF.setText("");
            rudderPTF.setText("");
            rudderITF.setText("");
            rudderDTF.setText("");
            winchTF.setText("");
            applyB.setEnabled(false);
        }
    }

    @Override
    public void eventOccurred(sami.event.InputEvent ie) {
    }

    @Override
    public void newObservation(Observation o) {
        if (!o.getSource().equals(activeProxy.getName())) {
            LOGGER.warning("Received observation from proxy other than active proxy!");
            return;
        } else {
            winchL.setText("Winch value: " + decimalFormat.format(o.getValue()));
        }
    }
}
