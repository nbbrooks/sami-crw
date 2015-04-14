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
import edu.cmu.ri.crw.data.SensorData;
import edu.cmu.ri.crw.data.UtmPose;
import java.util.logging.Logger;

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

    /**
     * Local reference to vehicle server.
     */
    protected AsyncVehicleServer _server;

    protected String _ipAddress;

    /**
     * Default constructor
     *
     */
    public LutraPlatform(AsyncVehicleServer _server, String ipAddress) {
        this._server = _server;
        this._ipAddress = ipAddress;

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

    /**
     * Returns the current GPS position
     *
     */
    public Position getPosition() {
//    System.out.println("  Platform.getPosition called");
        Position position = new Position(knowledge.get(".pose.x").toDouble(), knowledge.get(".pose.y").toDouble(), knowledge.get(".pose.z").toDouble());
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
        //    System.out.println("  Platform.move called");

        // Write new waypoints
        if (knowledge.get(".pose.zone").toString().isEmpty() || knowledge.get(".pose.hemisphere").toString().isEmpty()) {
            LOGGER.warning("Called move, but zone and hemisphere are unknown.");
        }

        waypoints.resize(1);
        waypoints.set(0, target.getX() + "," + target.getX() + "," + target.getX() + "," + knowledge.get(".pose.zone").toString() + "," + knowledge.get(".pose.hemisphere").toString());
//        // What to set this to?
//        wpState.set("");
        wpController.set("POINT_AND_SHOOT");
        // Mark that new waypoints have been set
        waypointsReceivedAck.set(1);
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
    public static void setUtmPose(KnowledgeBase knowledge, String knowledgePath, UtmPose utmPose) {
        knowledge.set(knowledgePath + ".x", utmPose.pose.getX());
        knowledge.set(knowledgePath + ".y", utmPose.pose.getY());
        knowledge.set(knowledgePath + ".z", utmPose.pose.getZ());
        knowledge.set(knowledgePath + ".roll", utmPose.pose.getRotation().toRoll());
        knowledge.set(knowledgePath + ".pitch", utmPose.pose.getRotation().toPitch());
        knowledge.set(knowledgePath + ".yaw", utmPose.pose.getRotation().toYaw());
        knowledge.set(knowledgePath + ".zone", utmPose.origin.zone);
        knowledge.set(knowledgePath + ".hemisphere", utmPose.origin.isNorth ? "North" : "South");
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
