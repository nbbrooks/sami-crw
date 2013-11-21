package crw.general;

import edu.cmu.ri.crw.SensorListener;
import edu.cmu.ri.crw.SimpleBoatController;
import edu.cmu.ri.crw.SimpleBoatSimulator;
import edu.cmu.ri.crw.VehicleController;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.Twist;
import edu.cmu.ri.crw.data.UtmPose;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Logger;
import robotutils.Pose3D;

/**
 * @todo Allow some of these things to be configured
 * @author pscerri
 */
public class FastSimpleBoatSimulator extends SimpleBoatSimulator {

    protected long LOCAL_UPDATE_INTERVAL = 1000L;
    private static final Logger logger = Logger.getLogger(SimpleBoatSimulator.class.getName());
    
    @Override
    public void startWaypoints(final UtmPose[] waypoints, final String controller) {
//        logger.log(Level.INFO, "Starting waypoints: {0}", Arrays.toString(waypoints));

        // Create a waypoint navigation task
        TimerTask newNavigationTask = new TimerTask() {
            final double dt = (double) UPDATE_INTERVAL_MS / 1000.0;

            /*
             // Retrieve the appropriate controller in initializer
             VehicleController vc = SimpleBoatController.POINT_AND_SHOOT.controller;
             {
             try {
             vc = SimpleBoatController.valueOf(controller).controller;
             } catch (IllegalArgumentException e) {
             logger.log(Level.WARNING, "Unknown controller specified (using {0} instead): {1}", new Object[]{vc, controller});
             // System.out.println("Unknown controller specified (using {0} instead): {1}");
             }
             }
             */
            @Override
            public void run() {
                synchronized (_navigationLock) {
                    if (!_isAutonomous.get()) {
                        // If we are not autonomous, do nothing
                        sendWaypointUpdate(WaypointState.PAUSED);
                        return;
                    } else if (_waypoints.length == 0) {
                        // If we are finished with waypoints, stop in place
                        sendWaypointUpdate(WaypointState.DONE);
                        setVelocity(new Twist());
                        this.cancel();
                        _navigationTask = null;
                    } else {
                        // If we are still executing waypoints, use a 
                        // controller to figure out how to get to waypoint
                        // TODO: measure dt directly instead of approximating
                        fvc.update(FastSimpleBoatSimulator.this, dt);
                        sendWaypointUpdate(WaypointState.GOING);
                    }
                }
            }
        };

        synchronized (_navigationLock) {
            // Change waypoints to new set of waypoints
            _waypoints = Arrays.copyOf(waypoints, waypoints.length);

            // Cancel any previous navigation tasks
            if (_navigationTask != null) {
                _navigationTask.cancel();
            }

            // Schedule this task for execution
            _navigationTask = newNavigationTask;
            _timer.scheduleAtFixedRate(_navigationTask, 0, UPDATE_INTERVAL_MS);
        }
    }
    VehicleController fvc = new VehicleController() {
        @Override
        public void update(VehicleServer server, double dt) {
            Twist twist = new Twist();            

            // Get the position of the vehicle 
            UtmPose state = server.getPose();
            Pose3D pose = state.pose;

            // Get the current waypoint, or return if there are none
            UtmPose[] waypoints = server.getWaypoints();
            if (waypoints == null || waypoints.length <= 0) {
                // This is zero
                server.setVelocity(twist);
                return;
            }
            Pose3D waypoint = waypoints[0].pose;

            // TODO: handle different UTM zones!

            // Compute the distance and angle to the waypoint
            double distanceSq = planarDistanceSq(pose, waypoint);
            double angle = angleBetween(pose, waypoint) - pose.getRotation().toYaw();
            angle = normalizeAngle(angle);

            // Choose driving behavior depending on direction and where we are
            if (Math.abs(angle) > 1.0) {

                // If we are facing away, turn around first
                twist.dx(0.5);
                twist.drz(Math.max(Math.min(angle / 1.0, 5.0), -5.0));
            } else if (distanceSq >= 3.0) {

                // If we are far away, drive forward and turn
                twist.dx(Math.min(distanceSq / 10.0, 10.0));
                twist.drz(Math.max(Math.min(angle / 3.0, 5.0), -5.0));
            } else {

                // If we are "at" the destination, de-queue a waypoint
                server.startWaypoints(Arrays.copyOfRange(waypoints, 1, waypoints.length),
                        SimpleBoatController.POINT_AND_SHOOT.toString());
            }

            // Set the desired velocity
            server.setVelocity(twist);
        }

        @Override
        public String toString() {
            return SimpleBoatController.POINT_AND_SHOOT.toString();
        }
    };

    public static double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    public static double planarDistanceSq(Pose3D a, Pose3D b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    public static double angleBetween(Pose3D src, Pose3D dest) {
        return Math.atan2((dest.getY() - src.getY()), (dest.getX() - src.getX()));
    }

    @Override
    public void addSensorListener(int channel, SensorListener l) {
        System.out.println("IGNORING SENSOR LISTENER ADD");
    }
}
