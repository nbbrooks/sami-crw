package crw.proxy;

import crw.Conversion;
import crw.event.input.proxy.ProxyPathCompleted;
import crw.event.input.proxy.ProxyStationKeepCompleted;
import crw.event.output.proxy.ProxyExecutePath;
import crw.event.output.proxy.ProxyStationKeep;
import crw.sensor.CrwObserverServer;
import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.FunctionObserver.FunctionError;
import edu.cmu.ri.crw.ImageListener;
import edu.cmu.ri.crw.PoseListener;
import edu.cmu.ri.crw.SensorListener;
import edu.cmu.ri.crw.VehicleServer.WaypointState;
import edu.cmu.ri.crw.WaypointListener;
import edu.cmu.ri.crw.data.Twist;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import edu.cmu.ri.crw.udp.UdpVehicleServer;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import robotutils.Pose3D;
import sami.engine.Engine;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;

/**
 * @todo Need a flag for autonomous or under human control
 *
 * @author pscerri
 */
public class BoatProxy extends Thread implements ProxyInt {

    private static final Logger LOGGER = Logger.getLogger(BoatProxy.class.getName());

    public static enum AutonomousSearchAlgorithmOptions {

        RANDOM, LAWNMOWER, MAX_UNCERTAINTY
    };

    public enum StateEnum {

        IDLE, WAYPOINT, PATH, AREA
    };
    // OutputEvents that must occur sequentially (require movement)
    protected OutputEvent curSequentialEvent = null;
    protected ArrayList<OutputEvent> sequentialOutputEvents = new ArrayList<OutputEvent>();
    // The resulting InputEvents from sequentialOutputEvents
    protected ArrayList<InputEvent> sequentialInputEvents = new ArrayList<InputEvent>();
    // @todo Needs to be configurable
    private double atWaypointTolerance = 30.0;
    private final String name;
    final AtomicBoolean isConnected = new AtomicBoolean(false);
    // Tasking stuff
    private Position currLoc = null;
    private UTMCoord currUtm = null;
    // Simulated
    private double fuelLevel = 1.0;
    private double fuelUsageRate = 1.0e-2;
    // ROS update
    PoseListener _stateListener;
    SensorListener _sensorListener;
    WaypointListener _waypointListener;
    ArrayList<ProxyListenerInt> listeners = new ArrayList<ProxyListenerInt>();
    Hashtable<ProxyListenerInt, Integer> listenerCounter = new Hashtable<ProxyListenerInt, Integer>();
    int _boatNo;
    UtmPose _pose;
    volatile boolean _isShutdown = false;
    // Latest image returned from this boat
    private BufferedImage latestImg = null;
    // Tell the boat to go slowly
    private boolean goSlow = false;
    private boolean goSlowExecuting = false;
    // Set this to false to turn off the false safe
    final boolean USE_SOFTWARE_FAIL_SAFE = true;
    UtmPose home = null;
    private StateEnum state = StateEnum.IDLE;
    final Queue<UtmPose> _curWaypoints = new LinkedList<UtmPose>();
    final Queue<UtmPose> _futureWaypoints = new LinkedList<UtmPose>();
    Iterable<Position> _curWaypointsPos = null;
    Iterable<Position> _futureWaypointsPos = null;
    UdpVehicleServer _server;
    private Color color = null;
    //private URI masterURI = null;
    Marker marker = null;
    Marker waypointMarker = null;
    public static AutonomousSearchAlgorithmOptions autonomousSearchAlgorithm = AutonomousSearchAlgorithmOptions.MAX_UNCERTAINTY;
    // Simulated sensor variables
    static double base = 100.0;
    static double distFactor = 0.01;
    static double valueFactor = 10.0;
    static double sigmaIncreaseRate = 0.00;
    static double valueDecreaseRate = 1.00;
    static double addRate = 0.01;
    static ArrayList<Double> xs = new ArrayList<Double>();
    static ArrayList<Double> ys = new ArrayList<Double>();
    static ArrayList<Double> vs = new ArrayList<Double>();
    static ArrayList<Double> sigmas = new ArrayList<Double>();
    static boolean simpleData = false;
    static boolean hysteresis = true;
    // Go slow variables
    Iterator<Position> goSlowWPs = null;
    UtmPose lastSentGoSlowPose = null;
    long timeLastGoSlowSent = 0L;
    long timeLastGoSlowDone = 0L;
    UtmPose goSlowCurrentTarget = null;
    // TO CHANGE
    long goSlowRestTime = 20000L;
    long goSlowToWaypointTime = 20000L;
    double maxWPDist = 30.0;
    StationKeepRunnable stationKeepRunnable;

    // End stuff for simulated data creation
    public BoatProxy(final String name, Color color, final int boatNo, InetSocketAddress addr) {

        LOGGER.log(Level.INFO, "Boat proxy created");

        // this.masterURI = masterURI;
        this.name = name;
        this.color = color;

        //Initialize the boat by initalizing a proxy server for it
        // Connect to boat
        _boatNo = boatNo;

        if (addr == null) {
            LOGGER.severe("$$$$$$$$$$$$$$$$$$$$$ addr falied");
        }

        _server = new UdpVehicleServer(addr);

        _stateListener = new PoseListener() {
            public void receivedPose(UtmPose upwcs) {

                // Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Boat pose update", this);
                _pose = upwcs.clone();

                if (home == null && USE_SOFTWARE_FAIL_SAFE) {
                    home = upwcs.clone();
                }

                // System.out.println("Pose: [" + _pose.pose.position.x + ", " + _pose.pose.position.y + "], zone = " + _pose.utm.zone);
                try {

                    int longZone = _pose.origin.zone;

                    // Convert hemisphere to arbitrary worldwind codes
                    // Notice that there is a "typo" in South that exists in the WorldWind code
                    // String wwHemi = (_pose.origin.isNorth) ? "gov.nasa.worldwind.avkey.North" : "gov.nasa.worldwdind.avkey.South";
                    String wwHemi = (_pose.origin.isNorth) ? AVKey.NORTH : AVKey.SOUTH;

                    // Fill in UTM data structure
                    // System.out.println("Converting from " + longZone + " " + wwHemi + " " + _pose.pose.getX() + " " + _pose.pose.getY());
                    UTMCoord boatPos = UTMCoord.fromUTM(longZone, wwHemi, _pose.pose.getX(), _pose.pose.getY());

                    LatLon latlon = new LatLon(boatPos.getLatitude(), boatPos.getLongitude());

                    // System.out.println("boatPos " + boatPos.getLatitude() + " " +  boatPos.getLongitude() + " latlon " + latlon.latitude.degrees + " " + latlon.longitude.degrees + " " + latlon);
                    Position p = new Position(latlon, 0.0);

                    // Update state variables
                    currLoc = p;
                    currUtm = boatPos;

                    for (ProxyListenerInt boatProxyListener : listeners) {
                        boatProxyListener.poseUpdated();
                    }

                } catch (Exception e) {
                    LOGGER.warning("BoatProxy: Invalid pose received: " + e + " Pose: [" + _pose.pose.getX() + ", " + _pose.pose.getY() + "], zone = " + _pose.origin.zone);
                    // e.printStackTrace();
                }

            }
        };

        LOGGER.log(Level.INFO, "New boat created, boat # " + _boatNo);

        //add Listeners
        _server.addPoseListener(_stateListener, null);

        _server.addWaypointListener(new WaypointListener() {
            public void waypointUpdate(WaypointState ws) {

                if (ws.equals(WaypointState.DONE)) {

                    LOGGER.log(Level.INFO, "BoatProxy " + getName() + " got waypoint update " + ws + " (WaypointState.DONE)");

                    // Handle the go slow
                    if (goSlowExecuting) {
                        timeLastGoSlowDone = System.currentTimeMillis();
                        goSlowRun();
                    }
                    // This is intentionally rechecking, because goSlowRun() will change the boolean
                    // if it is done
                    if (!goSlowExecuting) {
                        // Notify listeners
                        for (ProxyListenerInt boatProxyListener : listeners) {
                            boatProxyListener.waypointsComplete();
                        }
                    }
                    if (sequentialOutputEvents.get(0) instanceof ProxyStationKeep) {
                        // Return now - we don't want completion of a path to trigger station keep to end
                        return;
                    }

                    // Remove the OutputEvent that held this path
                    OutputEvent oe = sequentialOutputEvents.remove(0);
                    // Send out the InputEvent assocaited with this path
                    InputEvent ie = sequentialInputEvents.remove(0);

                    updateWaypoints(true, true);

                    LOGGER.log(Level.INFO, "BoatProxy " + getName() + " completed sequential OE " + oe + ", sending out IE " + ie);
                    for (ProxyListenerInt boatProxyListener : listeners) {
                        boatProxyListener.eventOccurred(ie);
                    }
                    if (!sequentialOutputEvents.isEmpty()) {
                        LOGGER.log(Level.INFO, "Sequential OE list is not empty, DO THE NEXT ONE!");
                        sendCurrentWaypoints();
//                        executeFirstEvent();
                    }
                }

            }
        }, null);

        _server.addImageListener(new ImageListener() {
            public void receivedImage(byte[] ci) {
                // Take a picture, and put the resulting image into the panel
                try {
                    BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(ci));
                    LOGGER.log(Level.INFO, "Got image ... ");

                    if (image != null) {
                        // Flip the image vertically
                        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                        tx.translate(0, -image.getHeight(null));
                        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                        image = op.filter(image, null);

                        if (image == null) {
                            LOGGER.warning("Failed to decode image.");
                        }

                        // ImagePanel.addImage(image, _pose);
                        latestImg = image;
                    } else {
                        LOGGER.warning("Image was null in receivedImage");
                    }
                } catch (IOException ex) {
                    LOGGER.warning("Failed to decode image: " + ex);
                }

            }
        }, null);

        for (int i = 0; i < 3; i++) {
            ((CrwObserverServer) Engine.getInstance().getObserverServer()).createObserver(this, i);
        }

        // startCamera();
    }

    @Override
    public int getProxyId() {
        return _boatNo;
    }

    @Override
    public String getProxyName() {
        return name;
    }

    @Override
    public void addListener(ProxyListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.log(Level.INFO, "First addition of listener " + l);
            listenerCounter.put(l, 1);
            listeners.add(l);
        } else {
            LOGGER.log(Level.INFO, "Count is now " + (listenerCounter.get(l) + 1) + " for " + l);
            listenerCounter.put(l, listenerCounter.get(l) + 1);
        }
    }

    @Override
    public void removeListener(ProxyListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.log(Level.WARNING, "Tried to remove ProxyListener that is not in the list!" + l);
        } else if (listenerCounter.get(l) == 1) {
            LOGGER.log(Level.INFO, "Last count of listener, removing " + l);
            listenerCounter.remove(l);
            listeners.remove(l);
        } else {
            LOGGER.log(Level.INFO, listenerCounter.get(l) - 1 + " counts remaining for " + l);
            listenerCounter.put(l, listenerCounter.get(l) - 1);
        }
    }

    @Override
    public void run() {
        // startCamera();
    }

    @Override
    public void handleEvent(OutputEvent oe) {
        handleEvent(oe, sequentialOutputEvents.size());
    }

    @Override
    public OutputEvent getCurrentEvent() {
        return curSequentialEvent;
    }

    @Override
    public ArrayList<OutputEvent> getEvents() {
        return sequentialOutputEvents;
    }

    public void handleEvent(OutputEvent oe, int index) {
        LOGGER.log(Level.INFO, "BoatProxy " + getName() + " was sent OutputEvent " + oe + " with index " + index);
        if (sequentialOutputEvents.size() > 0
                && sequentialOutputEvents.get(0) instanceof ProxyStationKeep) {
            // We have a new event, cancel station keeping
            OutputEvent stationKeep = sequentialOutputEvents.remove(0);
            // Send out the InputEvent assocaited with this path
            InputEvent stationKeepComplete = sequentialInputEvents.remove(0);
            if (stationKeepRunnable != null) {
                stationKeepRunnable.stopExecuting();
                stationKeepRunnable = null;
            }

            LOGGER.log(Level.INFO, "BoatProxy " + getName() + " completed sequential OE " + stationKeep + ", sending out IE " + stationKeepComplete);
            for (ProxyListenerInt boatProxyListener : listeners) {
                boatProxyListener.eventOccurred(stationKeepComplete);
            }
        }
        if (oe instanceof ProxyExecutePath) {
            sequentialOutputEvents.add(index, oe);
            sequentialInputEvents.add(index, new ProxyPathCompleted(oe.getId(), oe.getMissionId(), this));
        } else if (oe instanceof ProxyStationKeep) {
            sequentialOutputEvents.add(index, oe);
            sequentialInputEvents.add(index, new ProxyStationKeepCompleted(oe.getId(), oe.getMissionId(), this));
            ProxyStationKeep stationKeep = (ProxyStationKeep) oe;
            stationKeepRunnable = new StationKeepRunnable(this, stationKeep.point, stationKeep.radius);
            stationKeepRunnable.run();
        } else {
            LOGGER.severe("Can't handle OutputEvent of class " + oe.getClass().getSimpleName());
        }

        // Update proxy's waypoints
        updateWaypoints(true, true);
        if (sequentialOutputEvents.size() == 1 || index == 0) {
            sendCurrentWaypoints();
        } else {
        }

        updateAndSendWaypoints();
    }

    @Override
    public void abortEvent(UUID eventId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void abortMission(UUID missionId) {
        ArrayList<OutputEvent> outputEventsToRemove = new ArrayList<OutputEvent>();
        ArrayList<InputEvent> inputEventsToRemove = new ArrayList<InputEvent>();
        boolean removeFirst = false;
        for (int i = 0; i < sequentialOutputEvents.size(); i++) {
            if (sequentialOutputEvents.get(i).getMissionId().equals(missionId)) {
                outputEventsToRemove.add(sequentialOutputEvents.get(i));
                inputEventsToRemove.add(sequentialInputEvents.get(i));
                if (i == 0) {
                    // We need to remove the currently executing task
                    removeFirst = true;
                }
            }
        }
        for (OutputEvent outputEvent : outputEventsToRemove) {
            sequentialOutputEvents.remove(outputEvent);
        }
        for (InputEvent inputEvent : inputEventsToRemove) {
            sequentialInputEvents.remove(inputEvent);
        }

        updateAndSendWaypoints();
    }

    public OutputEvent getCurSequentialEvent() {
        return curSequentialEvent;
    }

    public void sample() {
        LOGGER.log(Level.INFO, "Calling sample on server");
        _server.captureImage(100, 100, null);
    }

    static public double computeGTValue(double lat, double lon) {
        double v = base;
        synchronized (xs) {
            for (int i = 0; i < xs.size(); i++) {

                double dx = xs.get(i) - lon;
                double dy = ys.get(i) - lat;
                double distSq = dx * dx + dy * dy;

                double dv = vs.get(i) * (1.0 / Math.sqrt(2.0 * Math.PI * sigmas.get(i) * sigmas.get(i))) * Math.pow(Math.E, -(distSq / (2.0 * sigmas.get(i) * sigmas.get(i))));
                // if (i == 0) System.out.println("Delta at dist " + Math.sqrt(distSq) + " for " + sigmas.get(i) + " is " + dv);
                v += dv;
            }
        }
        return v;
    }

    public int getBoatNo() {
        return _boatNo;
    }

    public boolean isIsShutdown() {
        return _isShutdown;
    }

    public UtmPose getPose() {
        return _pose;
    }

    public Queue<UtmPose> getCurrentWaypoints() {
        return _curWaypoints;
    }

    public Queue<UtmPose> getFutureWaypoints() {
        return _futureWaypoints;
    }

    public Iterable<Position> getCurrentWaypointsAsPositions() {
        return _curWaypointsPos;
    }

    public Position getCurrLoc() {
        return currLoc;
    }

    public Iterable<Position> getFutureWaypointsAsPositions() {
        return _futureWaypointsPos;
    }

    public AtomicBoolean getIsConnected() {
        return isConnected;
    }

    public boolean isGoSlow() {
        return goSlow;
    }

    public void setGoSlow(boolean goSlow) {
        this.goSlow = goSlow;
    }

    public void addSensorListener(int channel, SensorListener l) {
        _server.addSensorListener(channel, l, null);

        // @todo This only allows one sensor, generalize (but I think this is only for the fake data ...)
        _sensorListener = l;

        // System.out.println("Setting SENSOR LISTENER TO: " + l);
    }

    public void addPoseListener(PoseListener l) {
        _server.addPoseListener(l, null);
    }

    public void addWaypointListener(WaypointListener l) {
        _server.addWaypointListener(l, null);
    }

    public void updateWaypoints(boolean updateCurrent, boolean updateFuture) {
        // Current
        if (updateCurrent) {
            _curWaypointsPos = null;
            _curWaypoints.clear();
            curSequentialEvent = null;

            if (sequentialOutputEvents == null || sequentialOutputEvents.size() == 0) {
            } else {
                if (sequentialOutputEvents.get(0) instanceof ProxyExecutePath) {
                    ProxyExecutePath executePath = (ProxyExecutePath) sequentialOutputEvents.get(0);
                    if (executePath.getProxyPaths().containsKey(this)
                            && executePath.getProxyPaths().get(this) instanceof PathUtm) {
                        curSequentialEvent = sequentialOutputEvents.get(0);
                        PathUtm pathUtm = (PathUtm) executePath.getProxyPaths().get(this);
                        ArrayList<Position> positions = new ArrayList<Position>();
                        for (Location waypoint : pathUtm.getPoints()) {
                            positions.add(Conversion.locationToPosition(waypoint));
                        }
                        _curWaypointsPos = positions;
                        for (Position position : positions) {
                            UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                            UtmPose pose = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                            _curWaypoints.add(pose);
                        }
                    } else {
                        if (!executePath.getProxyPaths().containsKey(this)) {
                            LOGGER.severe("Proxy Paths has no entry for this proxy: " + this + ": " + executePath.getProxyPaths());
                        } else {
                            LOGGER.severe("Can't handle Path of class " + executePath.getProxyPaths().get(this).getClass().getSimpleName());
                        }
                    }
                } else {
                    LOGGER.severe("Can't handle OutputEvent of class " + sequentialOutputEvents.get(0).getClass().getSimpleName());
                }
            }
        }

        // Future
        if (updateFuture) {
            _futureWaypoints.clear();
            _futureWaypointsPos = null;

            if (sequentialOutputEvents == null || sequentialOutputEvents.size() < 2) {
            } else {
                ArrayList<Position> positions = new ArrayList<Position>();
                for (int i = 1; i < sequentialOutputEvents.size(); i++) {
                    if (sequentialOutputEvents.get(i) instanceof ProxyExecutePath) {
                        ProxyExecutePath executePath = (ProxyExecutePath) sequentialOutputEvents.get(0);
                        if (executePath.getProxyPaths().containsKey(this)
                                && executePath.getProxyPaths().get(this) instanceof PathUtm) {
                            PathUtm pathUtm = (PathUtm) executePath.getProxyPaths().get(this);
                            for (Location waypoint : pathUtm.getPoints()) {
                                positions.add(Conversion.locationToPosition(waypoint));
                            }
                        } else {
                            if (!executePath.getProxyPaths().containsKey(this)) {
                                LOGGER.severe("Proxy Paths has no entry for this proxy: " + this + ": " + executePath.getProxyPaths());
                            } else {
                                LOGGER.severe("Can't handle Path of class " + executePath.getProxyPaths().get(this).getClass().getSimpleName());
                            }
                        }
                    } else {
                        LOGGER.severe("Can't handle OutputEvent of class " + sequentialOutputEvents.get(i).getClass().getSimpleName());
                    }
                }
                _futureWaypointsPos = positions;
                for (Position position : positions) {
                    UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                    UtmPose pose = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                    _futureWaypoints.add(pose);
                }
            }
        }

        // Notify listeners
        for (ProxyListenerInt boatProxyListener : listeners) {
            boatProxyListener.waypointsUpdated();
        }
    }

    public void updateAndSendWaypoints() {
        updateWaypoints(true, true);
        if (_curWaypoints.isEmpty()) {
            // There weren't any more waypoints - stop the proxy
            // Stop proxy as opposed to sending empty list of waypoints because 
            //  that will make the system think it finished assigned waypoints
            _server.stopWaypoints(null);
            state = StateEnum.IDLE;
        } else {
            // Start the next set of waypoints  
            sendCurrentWaypoints();
        }
    }

    public void startCamera() {

        (new Thread() {
            public void run() {

                try {
                    LOGGER.log(Level.INFO, "SLEEPING BEFORE CAMERA START");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                LOGGER.log(Level.INFO, "DONE SLEEPING BEFORE CAMERA START");

                _server.startCamera(0, 30.0, 640, 480, null);

                LOGGER.log(Level.INFO, "Image listener started");
            }
        }).start();
    }

    public void addImageListener(ImageListener l) {
        _server.addImageListener(l, null);
        startCamera();
    }

    public void cancelCurrentSeqEvent() {
        if (sequentialOutputEvents.isEmpty()) {
            return;
        }
        sequentialOutputEvents.remove(0);
        sequentialInputEvents.remove(0);

        updateAndSendWaypoints();
    }

    /**
     * Takes in an output event: if the current sequential output event has the
     * same UUID, it is replaced with the passed in event: if not, it is
     * inserted at the beginning of the sequential list and the proxy begins
     * executing the event
     *
     * @param updatedEvent The event to replace or insert in front of the
     * current sequential event
     * @return Whether the event was handled
     */
    public boolean updateCurrentSeqEvent(OutputEvent updatedEvent) {
        if (curSequentialEvent != null && curSequentialEvent.getId().equals(updatedEvent.getId())) {
            if (updatedEvent instanceof ProxyExecutePath) {
                // Replace the existing event with the updated event and begin executing it
                sequentialOutputEvents.set(0, updatedEvent);
                curSequentialEvent = updatedEvent;

                updateWaypoints(true, false);

                sendCurrentWaypoints();
//                setPath(((ProxyExecutePath) updatedEvent).getPath());
                return true;
            } else {
                LOGGER.log(Level.INFO, "BoatProxy can't handle OutputEvent of class " + updatedEvent.getClass().getSimpleName());
            }
        } else {
            if (updatedEvent instanceof ProxyExecutePath) {
                // Insert the event and begin executing it
                sequentialOutputEvents.add(0, updatedEvent);
                sequentialInputEvents.add(0, new ProxyPathCompleted(updatedEvent.getId(), updatedEvent.getMissionId(), this));
                curSequentialEvent = updatedEvent;

                updateWaypoints(true, true);

                sendCurrentWaypoints();
//                setPath(((ProxyExecutePath) updatedEvent).getPath());
                return true;
            } else {
                LOGGER.severe("Can't handle OutputEvent of class " + updatedEvent.getClass().getSimpleName());
            }
        }
        return false;
    }

    public void setExternalVelocity(Twist t) {
        _server.setVelocity(t, new FunctionObserver<Void>() {
            public void completed(Void v) {
                LOGGER.log(Level.FINE, "Set velocity succeeded");
            }

            public void failed(FunctionError fe) {
                LOGGER.severe("Set velocity failed");
            }
        });
    }

    public StateEnum getMode() {
        return state;
    }

    public void sendCurrentWaypoints() {
        if (_curWaypoints == null) {
            return;
        }

        if (goSlow && _curWaypointsPos.iterator().hasNext()) {
            LOGGER.log(Level.INFO, "GO SLOW MODE");
            sendCurrentWaypointsGoSlow();
            return;
        }

        _server.setAutonomous(true, null);
        _server.startWaypoints(_curWaypoints.toArray(new UtmPose[_curWaypoints.size()]), "POINT_AND_SHOOT", new FunctionObserver() {
            int completedCounter = 0;

            public void completed(Object v) {
                LOGGER.log(Level.FINE, "Completed called");
            }

            public void failed(FunctionError fe) {
                // @todo Do something when start waypoints fails
                LOGGER.severe("START WAYPOINTS FAILED");
            }
        });
    }

    private void sendCurrentWaypointsGoSlow() {
        goSlowWPs = _curWaypointsPos.iterator();
        goSlowExecuting = true;
        goSlowRun();
    }

    private void goSlowRun() {

        if (System.currentTimeMillis() - timeLastGoSlowSent > goSlowRestTime
                && System.currentTimeMillis() - timeLastGoSlowDone > goSlowRestTime) {

            _curWaypoints.clear();

            try {

                // Get a new target point, if we don't have one
                if (goSlowCurrentTarget == null) {
                    Position position = goSlowWPs.next();
                    UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                    goSlowCurrentTarget = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                }

                // If we are too far from that target point, pick some intermediate point
                double dist = _pose.pose.getEuclideanDistance(goSlowCurrentTarget.pose);
                LOGGER.log(Level.FINE, "DISTANCE IS " + dist);
                UtmPose realTarget = null;

                if (dist > maxWPDist) {
                    LOGGER.log(Level.FINE, "Creating an intermediate point in go slow");
                    double x = _pose.pose.getX() + (maxWPDist / dist) * (goSlowCurrentTarget.pose.getX() - _pose.pose.getX());
                    double y = _pose.pose.getY() + (maxWPDist / dist) * (goSlowCurrentTarget.pose.getY() - _pose.pose.getY());
                    realTarget = new UtmPose(new Pose3D(x, y, _pose.pose.getZ(), _pose.pose.getRotation()), new Utm(currUtm.getZone(), currUtm.getHemisphere().contains("North")));
                    _curWaypoints.add(realTarget);
                } else {
                    // We are done with this point
                    _curWaypoints.add(goSlowCurrentTarget);
                    goSlowCurrentTarget = null;
                }

                timeLastGoSlowSent = System.currentTimeMillis();

                // This thread monitors whether it took too long to get to the waypoint
                (new Thread() {
                    public void run() {
                        try {
                            sleep(goSlowToWaypointTime);
                        } catch (InterruptedException e) {
                        }
                        if (System.currentTimeMillis() - timeLastGoSlowSent > goSlowToWaypointTime) {
                            LOGGER.log(Level.FINE, "Failed to reach waypoint in time, " + goSlowRestTime + " resting");

                            _server.stopWaypoints(new FunctionObserver<Void>() {
                                public void completed(Void v) {
                                    LOGGER.log(Level.FINE, "Resting waypoints due to too long");
                                }

                                public void failed(FunctionError fe) {
                                    LOGGER.log(Level.FINE, "Failed to rest: " + fe);
                                }
                            });

                            goSlowRun();
                        }
                    }
                }).start();

                _server.setAutonomous(true, null);
                _server.startWaypoints(_curWaypoints.toArray(new UtmPose[_curWaypoints.size()]), "POINT_AND_SHOOT", new FunctionObserver() {
                    public void completed(Object v) {
                        System.out.println("Successfully sent a waypoint in Go Slow: " + _curWaypoints.peek());
                    }

                    public void failed(FunctionError fe) {
                        // @todo Do something when start waypoints fails
                        System.out.println("START WAYPOINTS FAILED");
                    }
                });

            } catch (NoSuchElementException e) {
                System.out.println("Go slow is done! " + e);
                goSlowExecuting = false;
            }

        } else {

            System.out.println("Too soon to send next waypoint");

            (new Thread() {
                public void run() {
                    try {
                        sleep(goSlowRestTime / 2);
                        System.out.println("Thread awoke");
                    } catch (InterruptedException e) {
                    }
                    goSlowRun();
                }
            }).start();
        }

    }

    public void asyncGetWaypointStatus(FunctionObserver<WaypointState> fo) {
        _server.getWaypointStatus(fo);
    }

    public Color getColor() {
        return color;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public BufferedImage getLatestImg() {
        return latestImg;
    }

    private boolean at(Position p1, Position p2) {
        UTMCoord utm1 = UTMCoord.fromLatLon(p1.latitude, p1.longitude);
        UTMCoord utm2 = UTMCoord.fromLatLon(p2.latitude, p2.longitude);

        // @todo This only really works for short distances
        double dx = utm1.getEasting() - utm2.getEasting();
        double dy = utm1.getNorthing() - utm2.getNorthing();

        double dist = Math.sqrt(dx * dx + dy * dy);

        //System.out.println("Dist to waypoint now: " + dist);
        if (utm1.getHemisphere().equalsIgnoreCase(utm2.getHemisphere()) && utm1.getZone() == utm2.getZone()) {
            return dist < atWaypointTolerance;
        } else {
            return false;
        }

    }

    public UdpVehicleServer getVehicleServer() {
        return _server;
    }

    @Override
    public String toString() {
        return name + "@" + _server.getVehicleService();
        // (masterURI == null ? "Unknown" : masterURI.toString());
    }

    class StationKeepRunnable implements Runnable {

        private volatile boolean execute;
        private BoatProxy proxy;
        private Position position;
        private double radius;

        public StationKeepRunnable(BoatProxy proxy, Location location, double minRadius) {
            this.proxy = proxy;
            this.position = Conversion.locationToPosition(location);
            this.radius = minRadius;
        }

        @Override
        public void run() {
            execute = true;
            while (execute) {
                ArrayList<Position> line = new ArrayList<Position>();
                line.add(proxy.getCurrLoc());
                line.add(position);
                LengthMeasurer measurer = new LengthMeasurer(line);
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(BoatProxy.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public void stopExecuting() {
            execute = false;
        }
    }
}
