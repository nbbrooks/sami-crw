/**
 * *******************************************************************
 * Usage of this software requires acceptance of the GAMS-CMU License, which can
 * be found at the following URL:
 *
 * https://code.google.com/p/gams-cmu/wiki/License
 * *******************************************************************
 */
package crw.proxy;

import com.gams.platforms.BasePlatform;
import com.gams.platforms.Status;
import com.gams.utility.Position;
import com.madara.KnowledgeBase;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.ImageListener;
import edu.cmu.ri.crw.PoseListener;
import edu.cmu.ri.crw.SensorListener;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.SensorData;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.util.logging.Logger;
import robotutils.Pose3D;

/**
 * Interface for defining a platform to be used by GAMS. Care must be taken to
 * make all methods non-blocking, to prevent locking up the underlying MADARA
 * context.
 */
public class LutraPlatform extends BasePlatform implements PoseListener, SensorListener, ImageListener {

    private static final Logger LOGGER = Logger.getLogger(LutraPlatform.class.getName());

    com.madara.containers.Vector waypoints;
    com.madara.containers.String wpEventId;
    com.madara.containers.String wpState;
    com.madara.containers.String wpController;
    com.madara.containers.Integer waypointsReceivedAck;
    com.madara.containers.Integer waypointsCompletedAck;
    com.madara.containers.NativeDoubleVector location;
    com.madara.containers.NativeDoubleVector dest;
    com.madara.containers.NativeDoubleVector source;

    // Local reference to vehicle server.
    protected AsyncVehicleServer _server;
    // IP address including port number
    protected String _ipAddress;
    // Device id
    protected final int id;

    public LutraPlatform(AsyncVehicleServer _server, String ipAddress, int id) {
        this._server = _server;
        this._ipAddress = ipAddress;
        this.id = id;

        _server.addImageListener(this, new FunctionObserver<Void>() {

            @Override
            public void completed(Void v) {
                LOGGER.fine("addImageListener call completed");
            }

            @Override
            public void failed(FunctionObserver.FunctionError fe) {
                LOGGER.severe("addImageListener call failed");
            }
        });
        _server.addPoseListener(this, new FunctionObserver<Void>() {

            @Override
            public void completed(Void v) {
                LOGGER.fine("addPoseListener call completed");
            }

            @Override
            public void failed(FunctionObserver.FunctionError fe) {
                LOGGER.severe("addPoseListener call failed");
            }
        });
        _server.addSensorListener(0, this, new FunctionObserver<Void>() {

            @Override
            public void completed(Void v) {
                LOGGER.fine("addSensorListener call completed");
            }

            @Override
            public void failed(FunctionObserver.FunctionError fe) {
                LOGGER.severe("addSensorListener call failed");
            }
        });
    }

    public void init() {
        self.init();
        self.id.set(id);

        waypoints = new com.madara.containers.Vector();
        waypoints.setName(knowledge, _ipAddress + ".waypoints");

        wpEventId = new com.madara.containers.String();
        wpEventId.setName(knowledge, _ipAddress + ".waypoints.eventId");

        wpState = new com.madara.containers.String();
        wpState.setName(knowledge, _ipAddress + ".waypoints.state");

        wpController = new com.madara.containers.String();
        wpController.setName(knowledge, _ipAddress + ".waypoints.controller");

        waypointsReceivedAck = new com.madara.containers.Integer();
        waypointsReceivedAck.setName(knowledge, _ipAddress + ".waypoints.received");

        waypointsCompletedAck = new com.madara.containers.Integer();
        waypointsCompletedAck.setName(knowledge, _ipAddress + ".waypoints.completed");

        location = new com.madara.containers.NativeDoubleVector();
        location.setName(knowledge, "device.{X}.location");
        location.resize(3);

        dest = new com.madara.containers.NativeDoubleVector();
        dest.setName(knowledge, "device.{X}.dest");
        dest.resize(3);

        source = new com.madara.containers.NativeDoubleVector();
        source.setName(knowledge, "device.{X}.source");
        source.resize(3);
    }

    /**
     * Analyzes the platform.
     *
     * @return status information (@see Status)
     *
     */
    public int analyze() {
//    System.out.println("Platform.analyze called");
        return Status.OK.value();
    }

    /**
     * Returns the position accuracy in meters
     *
     * @return position accuracy
     *
     */
    public double getPositionAccuracy() {
//    System.out.println("  Platform.getPositionAccuracy called");
        return 0.0;
    }

    public double getGpsAccuracy() {
//    System.out.println("  Platform.getPositionAccuracy called");
        return 0.0;
    }

    /**
     * Returns the current GPS position
     *
     */
    public Position getPosition() {
//    System.out.println("  Platform.getPosition called");
        Position position = new Position(location.get(0), location.get(1), location.get(2));
        return position;
    }

    /**
     * Returns to the home location. This should be a non-blocking call.
     *
     * @return status information (@see Status)
     *
     */
    public int home() {
//    System.out.println("  Platform.home called");
        return Status.OK.value();
    }

    /**
     * Requests the platform to land. This should be a non-blocking call.
     *
     * @return status information (@see Status)
     *
     */
    public int land() {
//    System.out.println("  Platform.land called");
        return Status.OK.value();
    }

    /**
     * Initializes a move to the target position. This should be a non-blocking
     * call.
     *
     * @param target the new position to move to
     * @param proximity the minimum distance between current position and target
     * position that terminates the move.
     * @return status information (@see Status)
     *
     */
    public int move(Position target, double proximity) {
//        System.out.println(id + " Platform.move called");

        // Update SAMI boat proxy variables
        wpController.set("POINT_AND_SHOOT");
        wpState.set(VehicleServer.WaypointState.GOING.toString());
        waypointsReceivedAck.set(0);
        waypointsCompletedAck.set(0);

        // Send single point as waypoint list to server
        UtmPose[] _wpList = new UtmPose[1];
        UTMCoord utmCoord = UTMCoord.fromLatLon(Angle.fromDegreesLatitude(target.getX()), Angle.fromDegreesLongitude(target.getY()));
        _wpList[0] = new UtmPose(new Pose3D(utmCoord.getEasting(), utmCoord.getNorthing(), target.getZ(), 0.0, 0.0, 0.0), new Utm(utmCoord.getZone(), utmCoord.getHemisphere().contains("North")));
        if (_wpList[0] != null) {
            System.out.println(id + " move: [" + target.getX() + "," + target.getY() + "," + target.getZ() + "], [" + utmCoord.getEasting() + "," + utmCoord.getNorthing() + "," + utmCoord.getZone() + "," + utmCoord.getHemisphere() + "]");
            System.out.println(id + " locn: [" + location.get(0) + "," + location.get(1) + "," + location.get(2) + "]");
//            System.out.println(id + " start waypoints " + _wpList.length);
            _server.startWaypoints(_wpList, wpController.get(), new FunctionObserver<Void>() {

                @Override
                public void completed(Void v) {
                    LOGGER.fine("startWaypoints call completed");
                }

                @Override
                public void failed(FunctionObserver.FunctionError fe) {
                    LOGGER.severe("startWaypoints call failed");
                }
            });
        }

        // Write destination and source to device id path
        dest.set(0, target.getX());
        dest.set(1, target.getY());
        dest.set(2, target.getZ());
        source.set(0, location.get(0));
        source.set(1, location.get(1));
        source.set(2, location.get(2));

        return Status.OK.value();
    }

    /**
     * Get sensor radius
     *
     * @return minimum radius of all available sensors for this platform
     */
    public double getMinSensorRange() {
//    System.out.println("  Platform.getMinSensorRange called");
        return 0.0;
    }

    /**
     * Gets the movement speed
     *
     * @return movement speed
     *
     */
    public double getMoveSpeed() {
//    System.out.println("  Platform.getMoveSpeed called");
        return 0.0;
    }

    /**
     * Gets the unique id of the platform. This should be an alphanumeric id
     * that can be part of a MADARA variable name. Specifically, this is used in
     * the variable expansion of .platform.{yourid}.
     *
     *
     * @return the id of the platform (alphanumeric only: no spaces!)
     *
     */
    public java.lang.String getId() {
        // Get IP address
        return _ipAddress;
    }

    /**
     * Gets the name of the platform
     *
     * @return the name of the platform
     *
     */
    public java.lang.String getName() {
        return "Lutra";
    }

    /**
     * Gets results from the platform's sensors. This should be a non-blocking
     * call.
     *
     * @return 1 if moving, 2 if arrived, 0 if an error occurred
     *
     */
    public int sense() {
//    System.out.println("Platform.sense called");
        return Status.OK.value();
    }

    /**
     * Sets move speed
     *
     * @param speed new speed in meters/second
     *
     */
    public void setMoveSpeed(double speed) {
//    System.out.println("  Platform.setMoveSpeed called with " + speed);
    }

    /**
     * Takes off. This should be a non-blocking call.
     *
     * @return status information (@see Status)
     *
     */
    public int takeoff() {
//    System.out.println("  Platform.takeoff called");
        return Status.OK.value();
    }

    /**
     * Stops moving
     *
     */
    public void stopMove() {
//    System.out.println("  Platform.stopMove called");
        _server.stopWaypoints(
                new FunctionObserver<Void>() {

                    @Override
                    public void completed(Void v) {
                        // Clear waypoints?
                    }

                    @Override
                    public void failed(FunctionObserver.FunctionError fe) {
                        LOGGER.warning("stopWaypoints call failed.");
                    }
                });
    }

    @Override
    public void receivedPose(UtmPose utmPose) {
        setUtmPose(knowledge, _ipAddress + ".pose", utmPose);
        knowledge.sendModifieds();
    }

    /**
     * Conversion method that takes a UTMPose and a variable name in MADARA and
     * set the variable in the provided knowledge base to the given UtmPose.
     *
     * @param knowledge a knowledge base that will be updated
     * @param knowledgePath the name of the variable in the knowledge base that
     * should be updated
     * @param utmPose the UTM pose that the knowledge base will be updated with
     */
    public void setUtmPose(KnowledgeBase knowledge, String knowledgePath, UtmPose utmPose) {
        // @todo Redirect SAMI knowledge path to use device id instead of ip address
        
        // Write pose to ip address path
        knowledge.set(knowledgePath + ".x", utmPose.pose.getX());
        knowledge.set(knowledgePath + ".y", utmPose.pose.getY());
        knowledge.set(knowledgePath + ".z", utmPose.pose.getZ());
        knowledge.set(knowledgePath + ".roll", utmPose.pose.getRotation().toRoll());
        knowledge.set(knowledgePath + ".pitch", utmPose.pose.getRotation().toPitch());
        knowledge.set(knowledgePath + ".yaw", utmPose.pose.getRotation().toYaw());
        knowledge.set(knowledgePath + ".zone", utmPose.origin.zone);
        knowledge.set(knowledgePath + ".hemisphere", utmPose.origin.isNorth ? "North" : "South");

        // Write pose to device location path
        String wwHemi = utmPose.origin.isNorth ? AVKey.NORTH : AVKey.SOUTH;
        UTMCoord utmCoord = UTMCoord.fromUTM(utmPose.origin.zone, wwHemi, utmPose.pose.getX(), utmPose.pose.getY());
        location.set(0, utmCoord.getLatitude().degrees);
        location.set(1, utmCoord.getLongitude().degrees);
        location.set(2, utmPose.pose.getZ());
    }

    @Override
    public void receivedSensor(SensorData sensorData) {
        knowledge.set(".sensor." + sensorData.channel, sensorData.data);
    }

    @Override
    public void receivedImage(byte[] bytes) {
        // TODO: how do you set an IMAGE_JPEG to a variable in the knowledge base?
    }
}
