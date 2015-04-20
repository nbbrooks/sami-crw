package crw.proxy;

import com.madara.KnowledgeBase;
import com.madara.transport.QoSTransportSettings;
import com.madara.transport.TransportType;
import crw.general.FastSimpleBoatSimulator;
import crw.ui.teleop.*;
import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.RobotWidget.ControlMode;
import crw.ui.widget.SensorDataWidget;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import edu.cmu.ri.crw.udp.UdpVehicleService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import robotutils.Pose3D;
import sami.CoreHelper;
import sami.engine.Engine;
import sami.engine.Mediator;
import sami.path.UTMCoordinate;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;

/**
 *
 * @author nbb
 */
public class GamsFormationTest extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(TeleopTest.class.getName());
    static KnowledgeBase knowledge = null;
    ArrayList<LutraGamsServer> gamsServers = new ArrayList<LutraGamsServer>();

    public static void main(String[] args) {
        new GamsFormationTest();
    }

    public GamsFormationTest() {
        final JFrame mapFrame = new JFrame();
        mapFrame.getContentPane().setLayout(new BorderLayout());

        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);
        ProxyServerInt proxyServer = Engine.getInstance().getProxyServer();

        if (proxyServer instanceof CrwProxyServer) {
            knowledge = ((CrwProxyServer) proxyServer).getKnowledgeBase();
        } else {
            LOGGER.severe("ProxyServer is not a CrwProxyServer");
            System.exit(-1);
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

        int numRobots = 3;
        int portCounter = 0;
        String name = "Boat";
        Color color = Color.YELLOW;
        ArrayList<String> proxyNames = new ArrayList<String>();
        ArrayList<ProxyInt> proxyList = Engine.getInstance().getProxyServer().getProxyListClone();
        for (ProxyInt proxy : proxyList) {
            proxyNames.add(proxy.getProxyName());
        }

        // Spawn team
        LOGGER.info("Spawn team");
        String teamString = numRobots + "";
        for (int i = 0; i < numRobots; i++) {
            teamString += "," + i;
        }
        for (int i = 0; i < numRobots; i++) {
            // Create a simulated boat and run a ROS server around it
            VehicleServer server = new FastSimpleBoatSimulator();
            UdpVehicleService rosServer = new UdpVehicleService(11411 + portCounter, server);

            name = CoreHelper.getUniqueName(name, proxyNames);
            proxyNames.add(name);
            InetSocketAddress socketAddress = new InetSocketAddress("localhost", 11411 + portCounter);
            ProxyInt proxy = Engine.getInstance().getProxyServer().createProxy(name, color, socketAddress);
            portCounter++;
            String ipAddress = socketAddress.toString().substring(socketAddress.toString().indexOf("/") + 1);

            BoatProxy boatProxy = null;
            if (proxy instanceof BoatProxy) {
                boatProxy = (BoatProxy) proxy;
                // Set initial pose
                UTMCoordinate utmc = new UTMCoordinate(25.3543021007 + i *.002, 51.5283080718 + i *.002);
                UtmPose p1 = new UtmPose(new Pose3D(utmc.getEasting(), utmc.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utmc.getZoneNumber(), utmc.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH)));
                boatProxy.getServer().setPose(p1, null);
                LutraGamsServer gamsServer = new LutraGamsServer(((BoatProxy) proxy).getServer(), boatProxy.getIpAddress(), i);
                gamsServers.add(gamsServer);
                new Thread(gamsServer).start();
            }
        }

//        knowledge.print();
        // Set region
        LOGGER.info("Set region");
        knowledge.set("region.0.type", 0);
        knowledge.set("region.0.size", 5);
        com.madara.containers.NativeDoubleVector doubleArray;
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.0");
        doubleArray.resize(2);
        doubleArray.set(0, 25.3541773876);
        doubleArray.set(1, 51.5278304934);
//        knowledge.set("region.0.0", "25.3541773876, 51.5278304934");
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.1");
        doubleArray.resize(2);
        doubleArray.set(0, 25.3541374508);
        doubleArray.set(1, 51.5288937134);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.2");
        doubleArray.resize(2);
        doubleArray.set(0, 25.3546160196);
        doubleArray.set(1, 51.5289057336);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.3");
        doubleArray.resize(2);
        doubleArray.set(0, 25.3546017749);
        doubleArray.set(1, 51.5278422742);
        doubleArray = new com.madara.containers.NativeDoubleVector();
        doubleArray.setName(knowledge, "region.0.4");
        doubleArray.resize(2);
        doubleArray.set(0, 25.3546017749);
        doubleArray.set(1, 51.5278422742);
        knowledge.sendModifieds();

//        knowledge.print();
        // Set formation commands
        LOGGER.info("Set formation commands");
        for (int i = 0; i < numRobots; i++) {
            if (i == 0) {
                // Leader robot
                knowledge.set("device.0.command", "formation coverage");  // command selection
                knowledge.set("device.0.command.size", 6);                // number of parameters
                knowledge.set("device.0.command.0", 0);                   // lead agent ID
                knowledge.set("device.0.command.1", "0,0,0");             // location in formation
                knowledge.set("device.0.command.2", teamString);         // agents in formation (<num agents>,<comma separated agent list>)
                knowledge.set("device.0.command.3", "default");           // formation modifier ("rotation" or "default")
                knowledge.set("device.0.command.4", "urec");              // area coverage algorithm selection
                knowledge.set("device.0.command.5", "region.0");          // area coverage parameters
            } else {
                knowledge.set("device." + i + ".command", "formation coverage");  // command selection
                knowledge.set("device." + i + ".command.size", 6);                // number of parameters
                knowledge.set("device." + i + ".command.0", 0);                   // lead agent ID

                int direction = (i - 1) % 8;
                int magnitude = (i - 1) / 8 + 1;
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
                knowledge.set("device." + i + ".command.1", formationLocation);             // location in formation
                knowledge.set("device." + i + ".command.2", teamString);         // agents in formation (<num agents>,<comma separated agent list>)
                knowledge.set("device." + i + ".command.3", "default");           // formation modifier ("rotation" or "default")
                knowledge.set("device." + i + ".command.4", "urec");              // area coverage algorithm selection
                knowledge.set("device." + i + ".command.5", "region.0");          // area coverage parameters
            }
        }

        knowledge.sendModifieds();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(GamsFormationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        printAllKbs();
    }

    public void printAllKbs() {
        knowledge.print();
        for (int i = 0; i < gamsServers.size(); i++) {
            gamsServers.get(i)._knowledge.print();
        }

    }
}
