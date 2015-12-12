package crw.ui.tests;

import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleService;
import crw.CrwHelper;
import crw.general.FastSimpleBoatSimulator;
import crw.ui.CommFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Logger;
import robotutils.Pose3D;
import sami.CoreHelper;
import sami.engine.Engine;
import sami.path.UTMCoordinate;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyLabelTest {

    private static final Logger LOGGER = Logger.getLogger(ProxyLabelTest.class.getName());

    public static void main(String[] args) {
        CommFrame cf = new CommFrame();

        int numRobots = 50;
        int portCounter = 0;
        String name = "Boat";
        Color color;
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
            color = CrwHelper.randomColor();
            ProxyInt proxy = Engine.getInstance().getProxyServer().createProxy(name, color, new InetSocketAddress("localhost", 11411 + portCounter));
            portCounter++;

            if (proxy != null) {
                relevantProxyList.add(proxy);
            } else {
                LOGGER.severe("Failed to create simulated proxy");
            }
        }

        cf.setPreferredSize(new Dimension(800, 600));
        cf.setSize(cf.getPreferredSize());
        cf.repaint();
    }
}
