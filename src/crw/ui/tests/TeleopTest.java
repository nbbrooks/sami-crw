package crw.ui.tests;

import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleService;
import crw.general.FastSimpleBoatSimulator;
import crw.ui.component.WorldWindPanel;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.RobotWidget.ControlMode;
import crw.ui.widget.SensorDataWidget;
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

/**
 *
 * @author nbb
 */
public class TeleopTest extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(TeleopTest.class.getName());

    public static void main(String[] args) {
        final JFrame mapFrame = new JFrame();
        mapFrame.getContentPane().setLayout(new BorderLayout());

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

        int numRobots = 2;
        int portCounter = 0;
        String name = "Boat";
        Color color = Color.YELLOW;
        ArrayList<ProxyInt> relevantProxyList = new ArrayList<ProxyInt>();
        ArrayList<String> proxyNames = new ArrayList<String>();
        ArrayList<ProxyInt> proxyList = Engine.getInstance().getProxyServer().getProxyListClone();
        for (ProxyInt proxy : proxyList) {
            proxyNames.add(proxy.getProxyName());
        }
        for (int i = 0; i < numRobots; i++) {
            // Create a simulated boat and run a ROS server around it
            VehicleServer server = new FastSimpleBoatSimulator();
            UdpVehicleService rosServer = new UdpVehicleService(11411 + portCounter, server);
//            UTMCoordinate utmc = new UTMCoordinate(37.274274, -107.871281);
            UTMCoordinate utmc = new UTMCoordinate(25.354484741711, 51.5283418997116);
//            UTMCoordinate utmc = new UTMCoordinate(40.44515205369163, -80.01877404355538);
            UtmPose p1 = new UtmPose(new Pose3D(utmc.getEasting(), utmc.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utmc.getZoneNumber(), utmc.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH)));
            server.setPose(p1);
            name = CoreHelper.getUniqueName(name, proxyNames);
            proxyNames.add(name);
            ProxyInt proxy = Engine.getInstance().getProxyServer().createProxy(name, color, new InetSocketAddress("localhost", 11411 + portCounter));
            portCounter++;

            if (proxy != null) {
                relevantProxyList.add(proxy);
            } else {
                LOGGER.severe("Failed to create simulated proxy");
            }
        }

//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                while (true) {
//                    System.out.println("JFrame.getFocusOwner(): " + mapFrame.getFocusOwner());
////                .getClass().getSimpleName()+ "-" + mapFrame.getFocusOwner().getName());
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(TeleopTest.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }).start();
    }
}
