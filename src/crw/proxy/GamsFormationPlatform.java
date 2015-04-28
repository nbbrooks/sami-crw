package crw.proxy;

import crw.general.FastSimpleBoatSimulator;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import edu.cmu.ri.crw.udp.UdpVehicleService;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import javax.swing.JFrame;
import robotutils.Pose3D;
import sami.path.UTMCoordinate;

/**
 *
 * @author nbb
 */
public class GamsFormationPlatform extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(GamsFormationPlatform.class.getName());

    public static void main(String[] args) {
        new GamsFormationPlatform(0);
    }

    /**
     *
     * @param numRobots Number of robots to spawn and use in the formation
     * @param regionWidth Region width to be explored (m)
     * @param spacingMultiplier Minimum distance between robots in the
     * cylindrical coordinate formation configuration
     */
    public GamsFormationPlatform(int id) {

        // Spawn team
        LOGGER.info("Spawn platform");
        // Create a simulated boat and run a ROS server around it
        VehicleServer server = new FastSimpleBoatSimulator();
        UdpVehicleService rosServer = new UdpVehicleService(11411 + id, server);

        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 11411 + id);
        String ipAddress = socketAddress.toString().substring(socketAddress.toString().indexOf("/") + 1);

        // Set initial pose, offset each progressive proxy by 1.2m north east
        UTMCoordinate utmc = new UTMCoordinate(25.3543021007, 51.5283080718);
        UtmPose p1 = new UtmPose(new Pose3D(utmc.getEasting() + id, utmc.getNorthing() + id, 0.0, 0.0, 0.0, 0.0), new Utm(utmc.getZoneNumber(), utmc.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH)));
        server.setPose(p1);
        LutraGamsServer gamsServer = new LutraGamsServer(AsyncVehicleServer.Util.toAsync(server), ipAddress, id, GamsFormationRegion.TEAM_SIZE);
        new Thread(gamsServer).start();
        LOGGER.info("Spawn platform done");
    }
}
