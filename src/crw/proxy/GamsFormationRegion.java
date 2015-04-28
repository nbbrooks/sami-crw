package crw.proxy;

import com.madara.EvalSettings;
import com.madara.KnowledgeBase;
import com.madara.MadaraLog;
import com.madara.transport.QoSTransportSettings;
import com.madara.transport.TransportType;
import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.RobotWidget.ControlMode;
import crw.ui.widget.SensorDataWidget;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import sami.CoreHelper;
import sami.engine.Engine;
import sami.engine.Mediator;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;

/**
 *
 * @author nbb
 */
public class GamsFormationRegion extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(GamsFormationRegion.class.getName());

    public static final boolean DETAILED_TRACE = true;
    public static final int TEAM_SIZE = 3;

    static KnowledgeBase knowledge = null;
//    ArrayList<LutraGamsServer> gamsServers = new ArrayList<LutraGamsServer>();

    final double regionWidth;
    final double spacingMultiplier;

    public static void main(String[] args) {
        new GamsFormationRegion(100, 10);
    }

    /**
     *
     * @param numRobots Number of robots to spawn and use in the formation
     * @param regionWidth Region width to be explored (m)
     * @param spacingMultiplier Minimum distance between robots in the
     * cylindrical coordinate formation configuration
     */
    public GamsFormationRegion(double regionWidth, double spacingMultiplier) {
        this.regionWidth = regionWidth;
        this.spacingMultiplier = spacingMultiplier;

        final JFrame mapFrame = new JFrame();
        mapFrame.getContentPane().setLayout(new BorderLayout());

        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);

        EvalSettings evalSettings = new EvalSettings();
        evalSettings.setDelaySendingModifieds(true);

        ProxyServerInt proxyServer = Engine.getInstance().getProxyServer();

        if (proxyServer instanceof CrwProxyServer) {
            knowledge = ((CrwProxyServer) proxyServer).getKnowledgeBase();
        } else {
            LOGGER.severe("ProxyServer is not a CrwProxyServer");
            System.exit(-1);
        }

        if (DETAILED_TRACE) {
            // Set Madara log level
            com.madara.MadaraLog.setLogLevel(MadaraLog.MadaraLogLevel.MADARA_LOG_DETAILED_TRACE);
        }

        // Add map
        WorldWindPanel wwPanel = new WorldWindPanel();
        wwPanel.createMap(800, 600, null);
        // Add widgets
        mapFrame.getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        AnnotationWidget annotation = new AnnotationWidget(wwPanel);
        wwPanel.addWidget(annotation);
        SensorDataWidget data = new SensorDataWidget(wwPanel);
        wwPanel.addWidget(data);
        ArrayList<ControlMode> controlModes = new ArrayList<ControlMode>();
        controlModes.add(ControlMode.TELEOP);
        controlModes.add(ControlMode.POINT);
        controlModes.add(ControlMode.PATH);
        controlModes.add(ControlMode.NONE);
        RobotWidget robot = new RobotWidget(wwPanel, controlModes);
        wwPanel.addWidget(robot);
        RobotTrackWidget robotTrack = new RobotTrackWidget(wwPanel);
        wwPanel.addWidget(robotTrack);
        mapFrame.pack();
        mapFrame.setVisible(true);

        // Try to load the last used EPF file
        LOGGER.info("Load EPF");
        boolean success = Mediator.getInstance().openLatestEnvironment();
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load previous environment, opening new environment");
            Mediator.getInstance().newEnvironment();
        }

        int portCounter = 0;
        String name = "Boat";
        Color color = Color.YELLOW;
        ArrayList<String> proxyNames = new ArrayList<String>();
        ArrayList<ProxyInt> proxyList = Engine.getInstance().getProxyServer().getProxyListClone();
        for (ProxyInt proxy : proxyList) {
            proxyNames.add(proxy.getProxyName());
        }

        // Create boat proxies
        LOGGER.info("Create boat proxies");
        ArrayList<BoatProxy> boatProxies = new ArrayList<BoatProxy>();
        String teamString = TEAM_SIZE + "";
        for (int i = 0; i < TEAM_SIZE; i++) {
            teamString += "," + i;
        }
        for (int i = 0; i < TEAM_SIZE; i++) {
            name = CoreHelper.getUniqueName(name, proxyNames);
            proxyNames.add(name);
            InetSocketAddress socketAddress = new InetSocketAddress("localhost", 11411 + portCounter);
            ProxyInt proxy = Engine.getInstance().getProxyServer().createProxy(name, color, socketAddress);
            portCounter++;

            BoatProxy boatProxy = null;
            if (proxy instanceof BoatProxy) {
                boatProxy = (BoatProxy) proxy;
                boatProxies.add(boatProxy);
            }
        }
        for (BoatProxy boatProxy : boatProxies) {
            boatProxy.startListeners();
        }
        LOGGER.info("Create boat proxies done");

        // Create a ~square region
        // Center of region
        double latCenter = 25.3550180223;
        double lonCenter = 51.5276055616;
        // Half the width of the square
        final double LON_D_PER_M = 1.0 / 90000.0;
        final double LAT_D_PER_M = 1.0 / 110000.0;
        double halfSquareWidth = regionWidth * LON_D_PER_M;
        LOGGER.info("Set region");
        knowledge.set("region.0.type", 0, evalSettings);
        knowledge.set("region.0.size", 4, evalSettings);
        com.madara.containers.NativeDoubleVector doubleArray;
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.0");
        doubleArray.resize(2);
        doubleArray.set(0, latCenter + halfSquareWidth);
        doubleArray.set(1, lonCenter + halfSquareWidth);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.1");
        doubleArray.resize(2);
        doubleArray.set(0, latCenter + halfSquareWidth);
        doubleArray.set(1, lonCenter - halfSquareWidth);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.2");
        doubleArray.resize(2);
        doubleArray.set(0, latCenter - halfSquareWidth);
        doubleArray.set(1, lonCenter - halfSquareWidth);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.3");
        doubleArray.resize(2);
        doubleArray.set(0, latCenter - halfSquareWidth);
        doubleArray.set(1, lonCenter + halfSquareWidth);

        System.out.println("Region is:\nA\n" + (latCenter + halfSquareWidth) + "," + (lonCenter + halfSquareWidth) + "\n"
                + (latCenter + halfSquareWidth) + "," + (lonCenter - halfSquareWidth) + "\n"
                + (latCenter - halfSquareWidth) + "," + (lonCenter - halfSquareWidth) + "\n"
                + (latCenter - halfSquareWidth) + "," + (lonCenter + halfSquareWidth) + "\n");
        // Write region to file importable by AnnotationWidget for visualization
        FileWriter areaWriter;
        try {
            areaWriter = new FileWriter("formation-region.txt");
            areaWriter.write("A\n" + (latCenter + halfSquareWidth) + "," + (lonCenter + halfSquareWidth) + "\n"
                    + (latCenter + halfSquareWidth) + "," + (lonCenter - halfSquareWidth) + "\n"
                    + (latCenter - halfSquareWidth) + "," + (lonCenter - halfSquareWidth) + "\n"
                    + (latCenter - halfSquareWidth) + "," + (lonCenter + halfSquareWidth) + "\n");
            areaWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GamsFormationRegion.class.getName()).log(Level.SEVERE, null, ex);
        }
        knowledge.sendModifieds();

        // Set formation commands
        LOGGER.info("Set formation commands");
        // Produce cylindrical spacing string "x,y,z" (in meters) for each boat
        //  Space robots using cardinal and ordinal compass directions, incrementing the distance from the center of the formation with each cycle through the compass

        // Modulus counter for choosing compass direction
        int direction;
        // Meters to move in chosen compass direction
        int magnitude;
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (i == 0) {
                // Leader robot is at center of formation 0,0,0
                knowledge.set("device.0.command", "formation coverage", evalSettings);  // command selection
                knowledge.set("device.0.command.size", 6, evalSettings);                // number of parameters
                knowledge.set("device.0.command.0", 0, evalSettings);                   // lead agent ID
                knowledge.set("device.0.command.1", "0,0,0", evalSettings);             // location in formation
                knowledge.set("device.0.command.2", teamString, evalSettings);         // agents in formation (<num agents>,<comma separated agent list>)
                knowledge.set("device.0.command.3", "default", evalSettings);           // formation modifier ("rotation" or "default")
                knowledge.set("device.0.command.4", "urec", evalSettings);              // area coverage algorithm selection
                knowledge.set("device.0.command.5", "region.0", evalSettings);          // area coverage parameters
            } else {
                knowledge.set("device." + i + ".command", "formation coverage", evalSettings);  // command selection
                knowledge.set("device." + i + ".command.size", 6, evalSettings);                // number of parameters
                knowledge.set("device." + i + ".command.0", 0, evalSettings);                   // lead agent ID

                direction = (i - 1) % 8;
                magnitude = (int) (((i - 1) / 8 + 1) * spacingMultiplier);
                String formationLocation;
                switch (direction) {
                    case 0:
                        //  0: N
                        formationLocation = magnitude + ",0,0";
                        break;
                    case 1:
                        //  1: NE
                        formationLocation = magnitude + "," + magnitude + ",0";
                        break;
                    case 2:
                        //  2: E
                        formationLocation = "0," + magnitude + ",0";
                        break;
                    case 3:
                        //  3: SE
                        formationLocation = -magnitude + "," + magnitude + ",0";
                        break;
                    case 4:
                        //  4: S
                        formationLocation = -magnitude + ",0,0";
                        break;
                    case 5:
                        //  5: SW
                        formationLocation = -magnitude + "," + -magnitude + ",0";
                        break;
                    case 6:
                        //  6: W
                        formationLocation = "0," + -magnitude + ",0";
                        break;
                    case 7:
                        //  7: NW
                        formationLocation = magnitude + "," + -magnitude + ",0";
                        break;
                    default:
                        LOGGER.severe("Hit default case in formation location");
                        formationLocation = "0,0,0";
                        break;
                }
                knowledge.set("device." + i + ".command.1", formationLocation, evalSettings);             // location in formation
                knowledge.set("device." + i + ".command.2", teamString, evalSettings);         // agents in formation (<num agents>,<comma separated agent list>)
                knowledge.set("device." + i + ".command.3", "default", evalSettings);           // formation modifier ("rotation" or "default")
                knowledge.set("device." + i + ".command.4", "urec", evalSettings);              // area coverage algorithm selection
                knowledge.set("device." + i + ".command.5", "region.0", evalSettings);          // area coverage parameters
            }
        }

        knowledge.sendModifieds();
    }
}
