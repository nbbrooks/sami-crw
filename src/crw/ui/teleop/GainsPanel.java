package crw.ui.teleop;

import crw.proxy.BoatProxy;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.FunctionObserver;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import sami.engine.Engine;
import sami.sensor.Observation;
import sami.sensor.ObservationListenerInt;
import sami.sensor.ObserverInt;

/**
 *
 * @author nbb
 */
public class GainsPanel extends JPanel implements ObservationListenerInt {

    private static final Logger LOGGER = Logger.getLogger(GainsPanel.class.getName());
    public static final int THRUST_GAINS_AXIS = 0;
    public static final int RUDDER_GAINS_AXIS = 5;
    public static final int WINCH_GAINS_AXIS = 3;
    public static final int WINCH_PORT = 3;
    private JPanel miscP, thrustPidP, rudderPidP, winchPidP;
    public JTextField velocityMultF, winchF, thrustPF, thrustIF, thrustDF, rudderPF, rudderIF, rudderDF;
    public JLabel winchL;
    public JButton applyB = new JButton("Apply");
    public double velocityMult = 0.12, winch, thrustP, thrustI, thrustD, rudderP, rudderI, rudderD;
    private BoatProxy activeProxy = null;
    private AsyncVehicleServer activeVehicle = null;
    private ObserverInt activeWinchObserver = null;

    public GainsPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        velocityMultF = new JTextField(velocityMult + "");
        velocityMultF.setPreferredSize(new Dimension(50, velocityMultF.getPreferredSize().height));
//        winchF = new JTextField("");
//        winchF.setPreferredSize(new Dimension(50, winchF.getPreferredSize().height));
//        thrustPF = new JTextField("");
//        thrustPF.setPreferredSize(new Dimension(50, thrustPF.getPreferredSize().height));
//        thrustIF = new JTextField("");
//        thrustIF.setPreferredSize(new Dimension(50, thrustIF.getPreferredSize().height));
//        thrustDF = new JTextField("");
//        thrustDF.setPreferredSize(new Dimension(50, thrustDF.getPreferredSize().height));
//        rudderPF = new JTextField("");
//        rudderPF.setPreferredSize(new Dimension(50, rudderPF.getPreferredSize().height));
//        rudderIF = new JTextField("");
//        rudderIF.setPreferredSize(new Dimension(50, rudderIF.getPreferredSize().height));
//        rudderDF = new JTextField("");
//        rudderDF.setPreferredSize(new Dimension(50, rudderDF.getPreferredSize().height));

        miscP = new JPanel();
        miscP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        miscP.add(new JLabel("Velocity multiplier:"));
        miscP.add(velocityMultF);

//        thrustPidP = new JPanel();
//        thrustPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        thrustPidP.add(new JLabel("Thrust "));
//        thrustPidP.add(new JLabel("P:"));
//        thrustPidP.add(thrustPF);
//        thrustPidP.add(new JLabel("I:"));
//        thrustPidP.add(thrustIF);
//        thrustPidP.add(new JLabel("D:"));
//        thrustPidP.add(thrustDF);

//        rudderPidP = new JPanel();
//        rudderPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        rudderPidP.add(new JLabel("Rudder"));
//        rudderPidP.add(new JLabel("P:"));
//        rudderPidP.add(rudderPF);
//        rudderPidP.add(new JLabel("I:"));
//        rudderPidP.add(rudderIF);
//        rudderPidP.add(new JLabel("D:"));
//        rudderPidP.add(rudderDF);

//        winchPidP = new JPanel();
//        winchPidP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        winchL = new JLabel("Winch value: ---");
//        winchPidP.add(winchL);
//        winchPidP.add(winchF);

        add(miscP);
//        add(thrustPidP);
//        add(rudderPidP);
//        add(winchPidP);
        add(applyB);

        applyB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                applyFieldValues();
            }
        });
    }

    public double stringToDouble(String text) {
        double ret = Double.NaN;
        try {
            ret = Double.valueOf(velocityMultF.getText());
        } catch (NumberFormatException ex) {
        }
        return ret;
    }

    public void applyFieldValues() {
        if (activeVehicle == null) {
            LOGGER.warning("Tried to apply field values to a null vehicle server!");
            return;
        }
        boolean velocityMultChanged = false;
        boolean winchChanged = false;
        boolean thrustPidChanged = false;
        boolean rudderPidChanged = false;
        double temp;
        // Velocity multiplier
        temp = stringToDouble(velocityMultF.getText());
        if (temp != velocityMult) {
            velocityMult = temp;
            velocityMultChanged = true;
        }
//        // Winch
//        temp = stringToDouble(winchF.getText());
//        if (temp != winch) {
//            winch = temp;
//            winchChanged = true;
//        }
//        // Thrust PID
//        temp = stringToDouble(thrustPF.getText());
//        if (temp != thrustP) {
//            thrustP = temp;
//            thrustPidChanged = true;
//        }
//        temp = stringToDouble(thrustIF.getText());
//        if (temp != thrustI) {
//            thrustI = temp;
//            thrustPidChanged = true;
//        }
//        temp = stringToDouble(thrustDF.getText());
//        if (temp != thrustD) {
//            thrustD = temp;
//            thrustPidChanged = true;
//        }
//        // Rudder PID
//        temp = stringToDouble(rudderPF.getText());
//        if (temp != rudderP) {
//            rudderP = temp;
//            rudderPidChanged = true;
//        }
//        temp = stringToDouble(rudderIF.getText());
//        if (temp != rudderI) {
//            rudderI = temp;
//            rudderPidChanged = true;
//        }
//        temp = stringToDouble(rudderDF.getText());
//        if (temp != rudderD) {
//            rudderD = temp;
//            rudderPidChanged = true;
//        }

        if (velocityMultChanged) {
            // Nothing to do here, used by input devices
        }
//        if (winchChanged) {
//            // Send value
//            // Send value
//            activeVehicle.setGains(WINCH_GAINS_AXIS, new double[]{winch, winch, winch}, new FunctionObserver<Void>() {
//                public void completed(Void v) {
//                    System.out.println("Set winch gains succeeded");
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    System.out.println("Set winch gains failed");
//                }
//            });
//        }
//        if (thrustPidChanged) {
//            // Send value
//            activeVehicle.setGains(THRUST_GAINS_AXIS, new double[]{thrustP, thrustI, thrustD}, new FunctionObserver<Void>() {
//                public void completed(Void v) {
//                    System.out.println("Set thrust gains succeeded");
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    System.out.println("Set thrust gains failed");
//                }
//            });
//        }
//        if (rudderPidChanged) {
//            // Send value
//            activeVehicle.setGains(RUDDER_GAINS_AXIS, new double[]{rudderP, rudderI, rudderD}, new FunctionObserver<Void>() {
//                public void completed(Void v) {
//                    System.out.println("Set rudder gains succeeded");
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    System.out.println("Set rudder gains failed");
//                }
//            });
//        }
    }

    public void setProxy(BoatProxy boatProxy) {
        if (activeProxy == boatProxy) {
            return;
        }
//        // Stop listening to the old vehicle
//        if (activeWinchObserver != null) {
//            activeWinchObserver.removeListener(this);
//        }

        activeProxy = boatProxy;
        if (activeProxy != null) {
            activeVehicle = boatProxy.getVehicleServer();
//            // Retrieve vehicle specific values
//            // Thrust gains
//            activeVehicle.getGains(WINCH_GAINS_AXIS, new FunctionObserver<double[]>() {
//                public void completed(double[] values) {
//                    LOGGER.log(Level.FINE, "Get thrust gains succeeded");
//                    thrustPF.setText("" + values[0]);
//                    thrustIF.setText("" + values[1]);
//                    thrustDF.setText("" + values[2]);
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    LOGGER.severe("Get thrust gains failed");
//                }
//            });
//            // Rudder gains
//            activeVehicle.getGains(WINCH_GAINS_AXIS, new FunctionObserver<double[]>() {
//                public void completed(double[] values) {
//                    LOGGER.log(Level.FINE, "Get rudder gains succeeded");
//                    rudderPF.setText("" + values[0]);
//                    rudderIF.setText("" + values[1]);
//                    rudderDF.setText("" + values[2]);
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    LOGGER.severe("Get ruddergains failed");
//                }
//            });
//            // Winch
//            activeVehicle.getGains(WINCH_GAINS_AXIS, new FunctionObserver<double[]>() {
//                public void completed(double[] values) {
//                    LOGGER.log(Level.FINE, "Get winch gains succeeded");
//                    winchF.setText("" + values[0]);
//                }
//
//                public void failed(FunctionObserver.FunctionError fe) {
//                    LOGGER.severe("Get winch gains failed");
//                }
//            });
//            activeWinchObserver = Engine.getInstance().getObserverServer().getObserver(activeProxy, WINCH_PORT);

            applyB.setEnabled(true);
        } else {
            activeVehicle = null;
//            // No vehicle selected, blank out text fields
//            thrustPF.setText("");
//            thrustIF.setText("");
//            thrustDF.setText("");
//            rudderPF.setText("");
//            rudderIF.setText("");
//            rudderDF.setText("");
//            winchF.setText("");
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
            winchL.setText("Winch value: " + o.getValue());
        }
    }
}
