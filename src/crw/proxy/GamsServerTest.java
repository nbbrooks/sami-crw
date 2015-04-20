package crw.proxy;

import com.madara.KnowledgeBase;
import com.madara.transport.QoSTransportSettings;
import com.madara.transport.TransportType;
import crw.ui.teleop.*;
import crw.general.FastSimpleBoatSimulator;
import crw.ui.component.WorldWindPanel;
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
import java.util.logging.Logger;
import javax.swing.JFrame;
import robotutils.Pose3D;
import sami.CoreHelper;
import sami.engine.Engine;
import sami.path.UTMCoordinate;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;

/**
 *
 * @author nbb
 */
public class GamsServerTest extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(TeleopTest.class.getName());

    public static void main(String[] args) {

        final JFrame mapFrame = new JFrame();
        mapFrame.getContentPane().setLayout(new BorderLayout());

        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);
        ProxyServerInt proxyServer = Engine.getInstance().getProxyServer();
        KnowledgeBase knowledge = null;
        if (proxyServer instanceof CrwProxyServer) {
            knowledge = ((CrwProxyServer) proxyServer).getKnowledgeBase();
        } else {
            LOGGER.severe("ProxyServer is not a CrwProxyServer");
            System.exit(-1);
        }

        // Add map
        WorldWindPanel wwPanel = new WorldWindPanel();
        wwPanel.createMap();
        mapFrame.getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
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

        int numRobots = 1;
        int portCounter = 0;
        String name = "Boat";
        Color color = Color.YELLOW;
        ArrayList<String> proxyNames = new ArrayList<String>();
        ArrayList<ProxyInt> proxyList = Engine.getInstance().getProxyServer().getProxyListClone();
        for (ProxyInt proxy : proxyList) {
            proxyNames.add(proxy.getProxyName());
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
                UTMCoordinate utmc = new UTMCoordinate(25.3543021007, 51.5283080718);
                UtmPose p1 = new UtmPose(new Pose3D(utmc.getEasting(), utmc.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utmc.getZoneNumber(), utmc.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH)));
                boatProxy.getServer().setPose(p1, null);
                new Thread(new LutraGamsServer(((BoatProxy) proxy).getServer(), boatProxy.getIpAddress(), i)).start();
            }
        }
    }
}
