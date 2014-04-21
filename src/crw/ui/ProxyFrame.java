package crw.ui;

import crw.general.FastSimpleBoatSimulator;
import edu.cmu.ri.crw.CrwNetworkUtils;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import edu.cmu.ri.crw.udp.UdpVehicleService;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.InetSocketAddress;
import java.security.AccessControlException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import robotutils.Pose3D;
import sami.engine.Engine;
import sami.uilanguage.UiFrame;

/**
 *
 * @author pscerri
 */
public class ProxyFrame extends UiFrame {

    private final static Logger LOGGER = Logger.getLogger(ProxyFrame.class.getName());
    final String LAST_URI_KEY = "LAST_URI_KEY";
    final String LAST_IMG_DIR_KEY = "LAST_IMG_DIR_KEY";
    final String LAT_SIM = "LAT_SIM_KEY";
    final String LON_SIM = "LON_SIM_KEY";
    private int boatCounter = 0;

    /**
     * Creates new form ConfigureBoatsFrame
     */
    public ProxyFrame() {
        initComponents();
        setVisible(true);

        Color color = randomColor();
        colorB.setBackground(color);
        colorB.setForeground(color);

        try {
            LOGGER.info("SecurityManager: " + System.getSecurityManager());

            physicalServerF.setText(Preferences.userRoot().get(LAST_URI_KEY, "http://168.192.1.X:11411"));
            imagesDirF.setText(Preferences.userRoot().get(LAST_IMG_DIR_KEY, "/tmp"));
            latSimF.setText(Preferences.userRoot().get(LAT_SIM, "25.3"));
            lonSimF.setText(Preferences.userRoot().get(LON_SIM, "51.5333"));
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to access preferences: " + e);

            JOptionPane.showMessageDialog(this, "A network error occurred, please restart", "Error", JOptionPane.OK_OPTION);
            physicalServerF.setText("http://168.192.1.X:11411");
            imagesDirF.setText("/tmp");

        }
    }

    private void initComponents() {

        physicalP = new javax.swing.JPanel();
        physicalServerL = new javax.swing.JLabel("Server");
        physicalServerF = new javax.swing.JTextField();
        createPhysicalB = new javax.swing.JButton();
        eBoxL = new javax.swing.JLabel("Ebox #");
        eBoxT = new javax.swing.JTextField();
        physNoL = new javax.swing.JLabel("Number");
        physNoS = new javax.swing.JSpinner();
        phoneL = new javax.swing.JLabel("Phone #");
        phoneF = new javax.swing.JTextField();
        simulatedP = new javax.swing.JPanel();
        simNoL = new javax.swing.JLabel("Number");
        simNoS = new javax.swing.JSpinner();
        simPortNoL = new javax.swing.JLabel("First port");
        simPortNoS = new javax.swing.JSpinner();
        latSimL = new javax.swing.JLabel("Lat");
        latSimF = new javax.swing.JTextField();
        lonSimL = new javax.swing.JLabel("Lon");
        lonSimF = new javax.swing.JTextField();
        createSimB = new javax.swing.JButton();
        miscP = new javax.swing.JPanel();
        nameL = new javax.swing.JLabel("Name");
        nameF = new javax.swing.JTextField();
        colorB = new javax.swing.JButton();
        imagesDirL = new javax.swing.JLabel("Image storage directory:        ");
        imagesDirF = new javax.swing.JTextField();
        browseDirB = new javax.swing.JButton();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        // Miscellaneous panel
        miscP.setLayout(new GridBagLayout());

        nameF.setText("Boat1");
        nameF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFActionPerformed(evt);
            }
        });
        constraints.gridy = 0;
        constraints.gridx = 0;
        miscP.add(nameL, constraints);
        constraints.gridx = 1;
        miscP.add(nameF, constraints);

        colorB.setBackground(new java.awt.Color(0, 0, 0));
        colorB.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        colorB.setText("Color");
        colorB.setOpaque(true);
        colorB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorBActionPerformed(evt);
            }
        });
        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        miscP.add(colorB, constraints);

        imagesDirF.setText("jTextField1");
        constraints.gridy = 3;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        miscP.add(imagesDirL, constraints);
        constraints.gridy = 4;
        miscP.add(imagesDirF, constraints);

        browseDirB.setText("Browse");
        browseDirB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFBActionPerformed(evt);
            }
        });
        constraints.gridy = 5;
        miscP.add(browseDirB, constraints);

        // Physical panel
        physicalP.setBorder(javax.swing.BorderFactory.createTitledBorder("Physical"));
        physicalP.setLayout(new GridBagLayout());

        physNoS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                boatNoSStateChanged(evt);
            }
        });
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        physicalP.add(physNoS, constraints);

        eBoxT.setText("0");
        constraints.gridy = 1;
        constraints.gridx = 0;
        physicalP.add(eBoxL, constraints);
        constraints.gridx = 1;
        physicalP.add(eBoxT, constraints);

        phoneF.setText("0");
        phoneF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                phoneTActionPerformed(evt);
            }
        });
        constraints.gridy = 2;
        constraints.gridx = 0;
        physicalP.add(phoneL, constraints);
        constraints.gridx = 1;
        physicalP.add(phoneF, constraints);

        physicalServerF.setText("http://192.168.1.149:11411");
        constraints.gridy = 3;
        constraints.gridx = 0;
        physicalP.add(physicalServerL, constraints);
        constraints.gridx = 1;
        physicalP.add(physicalServerF, constraints);

        createPhysicalB.setText("Initialize Real");
        createPhysicalB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createPhysicalBActionPerformed(evt);
                incrementBoatIdInfo();
            }
        });
        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        physicalP.add(createPhysicalB, constraints);

        // Simulated panel
        simulatedP.setBorder(javax.swing.BorderFactory.createTitledBorder("Simulated"));
        simulatedP.setLayout(new GridBagLayout());

        simNoS.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        simulatedP.add(simNoL, constraints);
        constraints.gridx = 1;
        simulatedP.add(simNoS, constraints);

        simPortNoS.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(11411), null, null, Integer.valueOf(1)));
        constraints.gridy = 1;
        constraints.gridx = 0;
        simulatedP.add(simPortNoL, constraints);
        constraints.gridx = 1;
        simulatedP.add(simPortNoS, constraints);

        latSimF.setText("25.3");
        latSimF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latSimActionPerformed(evt);
            }
        });
        constraints.gridy = 2;
        constraints.gridx = 0;
        simulatedP.add(latSimL, constraints);
        constraints.gridx = 1;
        simulatedP.add(latSimF, constraints);

        lonSimF.setText("51.333");
        constraints.gridy = 3;
        constraints.gridx = 0;
        simulatedP.add(lonSimL, constraints);
        constraints.gridx = 1;
        simulatedP.add(lonSimF, constraints);

        createSimB.setText("Create Simulated");
        createSimB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createSimBActionPerformed(evt);
                incrementBoatIdInfo();
            }
        });
        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        simulatedP.add(createSimB, constraints);

        setLayout(new FlowLayout());
        add(miscP);
        add(physicalP);
        add(simulatedP);
        pack();
    }

    private Color randomColor() {
        Random rand = new Random();

        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();

        return new Color(r, g, b);
    }

    private void createPhysicalBActionPerformed(java.awt.event.ActionEvent evt) {
        String server = physicalServerF.getText();
        // @todo connection to physical boat

        // These are done in BoatSimpleProxy
        // URI masterUri = new URI(server);
        // RosVehicleProxy rosServer = new RosVehicleProxy(masterUri, "vehicle_client" + new Random().nextInt(1000000));
        Engine.getInstance().getProxyServer().createProxy(nameF.getText(), colorB.getBackground(), CrwNetworkUtils.toInetSocketAddress(server));
        ImagePanel.setImagesDirectory(imagesDirF.getText());
        try {
            Preferences p = Preferences.userRoot();
            p.put(LAST_URI_KEY, server);
            p.put(LAST_IMG_DIR_KEY, imagesDirF.getText());
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to save preferences");
        }

    }

    private void latSimActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void createSimBActionPerformed(java.awt.event.ActionEvent evt) {
        double lat = Double.parseDouble(latSimF.getText());
        double lon = Double.parseDouble(lonSimF.getText());

        final int port = (Integer) simPortNoS.getValue();
        int count = (Integer) simNoS.getValue();

        for (int i = 0; i < count; i++) {

            // Create a simulated boat and run a ROS server around it
            VehicleServer server = new FastSimpleBoatSimulator();

            // @todo Can specify port?
            UdpVehicleService rosServer = new UdpVehicleService(port + i, server);

            // Create a ROS proxy server that accesses the same object
            // RosVehicleProxy proxyServer = new RosVehicleProxy(masterUri, "vehicle_client");
            LOGGER.info("Initialization of vehicle server complete");
            UTMCoord utm = UTMCoord.fromLatLon(Angle.fromDegrees(lat), Angle.fromDegrees(lon));
            UtmPose p1 = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
            server.setPose(p1);
            Engine.getInstance().getProxyServer().createProxy(nameF.getText(), colorB.getBackground(), new InetSocketAddress("localhost", port + i));
            colorB.setBackground(randomColor());
        }

        simPortNoS.setValue(port + count);

        try {
            Preferences p = Preferences.userRoot();
            p.put(LAT_SIM, latSimF.getText());
            p.put(LON_SIM, lonSimF.getText());
        } catch (AccessControlException e) {
            LOGGER.info("Failed to save preferences");
        }

    }

    private void phoneTActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void nameFActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void boatNoSStateChanged(javax.swing.event.ChangeEvent evt) {
        eBoxT.setText(physNoS.getValue().toString());
        phoneF.setText(physNoS.getValue().toString());
    }

    private void colorBActionPerformed(java.awt.event.ActionEvent evt) {
        Color color = JColorChooser.showDialog(this, "Choose color", colorB.getBackground());
        colorB.setBackground(color);
        colorB.setForeground(color);
    }

    private void browseFBActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //In response to a button click:
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            imagesDirF.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ProxyFrame().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify     
    private javax.swing.JLabel physNoL;
    private javax.swing.JSpinner physNoS;
    private javax.swing.JButton browseDirB;
    private javax.swing.JButton colorB;
    private javax.swing.JButton createPhysicalB;
    private javax.swing.JButton createSimB;
    private javax.swing.JTextField eBoxT;
    private javax.swing.JTextField imagesDirF;
    private javax.swing.JLabel lonSimL;
    private javax.swing.JLabel nameL;
    private javax.swing.JLabel eBoxL;
    private javax.swing.JLabel phoneL;
    private javax.swing.JLabel imagesDirL;
    private javax.swing.JLabel physicalServerL;
    private javax.swing.JLabel simNoL;
    private javax.swing.JLabel simPortNoL;
    private javax.swing.JLabel latSimL;
    private javax.swing.JPanel physicalP;
    private javax.swing.JPanel simulatedP;
    private javax.swing.JPanel miscP;
    private javax.swing.JTextField latSimF;
    private javax.swing.JTextField lonSimF;
    private javax.swing.JTextField nameF;
    private javax.swing.JTextField phoneF;
    private javax.swing.JTextField physicalServerF;
    private javax.swing.JSpinner simNoS;
    private javax.swing.JSpinner simPortNoS;
    // End of variables declaration                   

    private void incrementBoatIdInfo() {
        boatCounter++;
        nameF.setText("Boat" + (boatCounter + 1));
    }
}
