package crw.ui.widget;

import crw.Conversion;
import crw.ui.worldwind.WorldWindWidgetInt;
import crw.ui.component.WorldWindPanel;
import crw.Helper;
import crw.event.output.proxy.ProxyExecutePath;
import crw.event.output.proxy.ProxyStationKeep;
import crw.proxy.BoatProxy;
import crw.ui.BoatMarker;
import crw.ui.VideoFeedPanel;
import crw.ui.teleop.VelocityPanel;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.data.Twist;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.event.GeneratedEventListenerInt;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.markup.Markup;
import sami.markup.RelevantInformation;
import sami.markup.RelevantProxy;
import sami.path.Location;
import sami.path.Path;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;
import sami.proxy.ProxyServerListenerInt;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author nbb
 */
public class RobotWidget implements MarkupComponentWidget, WorldWindWidgetInt, ProxyServerListenerInt {

    public enum ControlMode {

        TELEOP, POINT, PATH, NONE
    };
    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    // This increases the "grab" radius for proxy markers to make selecting a proxy easier
    private final int CLICK_RADIUS = 25;
    private boolean visible = true;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Position> selectedPositions = new ArrayList<Position>();
    private BoatProxy selectedProxy = null;
    private VelocityPanel velocityP;
    private ControlMode controlMode = ControlMode.NONE;
    private Hashtable<GeneratedEventListenerInt, UUID> listenerTable = new Hashtable<GeneratedEventListenerInt, UUID>();
    private Hashtable<BoatProxy, BoatMarker> proxyToMarker = new Hashtable<BoatProxy, BoatMarker>();
    private Hashtable<BoatMarker, BoatProxy> markerToProxy = new Hashtable<BoatMarker, BoatProxy>();
    private Hashtable<BoatProxy, UUID> proxyToWpEventId = new Hashtable<BoatProxy, UUID>();
    private JButton teleopButton, pointButton, pathButton, stationKeepButton, cancelButton, autoButton;
    private JPanel topPanel, btmPanel, buttonPanel;
    private List<ControlMode> enabledModes;
    private Marker selectedMarker = null;
    private MarkerLayer markerLayer;
    private final Material UNSELECTED_MAT = new Material(new Color(25, 0, 0));
    private final Material SELECTED_MAT = new Material(new Color(255, 255, 50));
    private Polyline polyline = null;
    private RenderableLayer renderableLayer;
    private VideoFeedPanel videoP;
    private WorldWindPanel wwPanel;
    
    
    // new stuff
    // Velocity to be sent to the boat
    public double telRudderFrac = 0.0, telThrustFrac = 0;
    protected AsyncVehicleServer _vehicle = null;
    // Sets up a flag limiting the rate of velocity command transmission
    public AtomicBoolean _sentVelCommand = new AtomicBoolean(false);
    public AtomicBoolean _queuedVelCommand = new AtomicBoolean(false);
    protected java.util.Timer _timer = new java.util.Timer();
    public static final int DEFAULT_UPDATE_MS = 750;
    public static final int DEFAULT_COMMAND_MS = 200;
    // Ranges for thrust and rudder signals
    public static final double THRUST_MIN = 0.0;
    public static final double THRUST_MAX = 1.0;
    public static final double RUDDER_MIN = 1.0;
    public static final double RUDDER_MAX = -1.0;

    public RobotWidget() {
        populateLists();
    }

    public RobotWidget(WorldWindPanel wwPanel) {
        this(wwPanel, null);
    }

    public RobotWidget(WorldWindPanel wwPanel, List<ControlMode> enabledModes) {
        this.wwPanel = wwPanel;
        this.enabledModes = enabledModes;
        if (enabledModes == null) {
            this.enabledModes = new ArrayList<ControlMode>();
        }
        initRenderableLayer();
        initButtons();
        initExpandables();
        Engine.getInstance().getProxyServer().addListener(this);
    }

    @Override
    public void setMap(WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void paint(Graphics2D g2d) {
    }

    @Override
    public boolean mouseClicked(MouseEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean mousePressed(MouseEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseEvent evt, WorldWindow wwd) {
        Position clickPosition = wwd.getView().computePositionFromScreenPoint(evt.getX(), evt.getY());
        switch (controlMode) {
            case POINT:
                if (clickPosition != null) {
                    doPoint(clickPosition);
                    setControlMode(ControlMode.NONE);
                    return true;
                }
                break;
            case PATH:
                if (clickPosition != null) {
                    selectedPositions.add(clickPosition);
                    // Update temporary path
                    if (polyline != null) {
                        renderableLayer.removeRenderable(polyline);
                    }
                    polyline = new Polyline(selectedPositions);
                    polyline.setColor(Color.yellow);
                    polyline.setLineWidth(8);
                    polyline.setFollowTerrain(true);
                    renderableLayer.addRenderable(polyline);

                    wwd.redraw();
                    if (evt.getClickCount() > 1) {
                        // Finish path
                        doPath(selectedPositions);
                        clearPath();
                        setControlMode(ControlMode.NONE);
                    }
                    return true;
                }
                break;
            case NONE:
                // Selected a proxy?
                ArrayList<Marker> markersCopy;
                synchronized (markers) {
                    markersCopy = (ArrayList<Marker>) markers.clone();
                }
                boolean clickedProxy = false;
                double minDistanceFromClick = Double.MAX_VALUE;
                Marker closestMarker = null;
                for (Marker marker : markersCopy) {
                    Point clickPoint = evt.getPoint();
                    // Do this because we want grabbing to be pixel based and not m based
                    Position tlPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x - CLICK_RADIUS, clickPoint.y - CLICK_RADIUS);
                    Position brPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x + CLICK_RADIUS, clickPoint.y + CLICK_RADIUS);
                    // Do this so we can then select the closest proxy to the click in case there are multiple proxies within the above area
                    Position clickPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x, clickPoint.y);
                    Position markerPos = marker.getPosition();
                    if (Helper.isPositionBetween(markerPos, tlPos, brPos)) {
                        clickedProxy = true;
                        double distanceFromClick = Helper.calculateDistance(wwd.getView().getGlobe(), markerPos, clickPos);
                        if (distanceFromClick < minDistanceFromClick) {
                            minDistanceFromClick = distanceFromClick;
                            closestMarker = marker;
                        }
                    }
                }
                if (clickedProxy) {
                    // Select the marker
                    selectMarker(closestMarker);
                    return true;
                } else if (selectedMarker != null) {
                    // Click away from the previously selected marker, deselect it
                    selectMarker(null);
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean mouseMoved(MouseEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean mouseWheelMoved(MouseWheelEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e, WorldWindow wwd) {
        return false;
    }

    @Override
    public void proxyAdded(ProxyInt proxy) {
        if (proxy instanceof BoatProxy) {
            final BoatProxy boatProxy = (BoatProxy) proxy;
            // Create marker
            final BoatMarker bm = new BoatMarker(boatProxy, boatProxy.getPosition(), new BasicMarkerAttributes(new Material(boatProxy.getColor()), BasicMarkerShape.ORIENTED_SPHERE, 0.9));
            bm.getAttributes().setHeadingMaterial(UNSELECTED_MAT);
            bm.setPosition(boatProxy.getPosition());

            // Create listener to update marker pose
            boatProxy.addListener(new ProxyListenerInt() {
                boolean first = true;

                public void poseUpdated() {
                    bm.setPosition(bm.getProxy().getPosition());
                    bm.setHeading(Angle.fromRadians(Math.PI / 2.0 - bm.getProxy().getUtmPose().pose.getRotation().toYaw()));

                    if (first) {
                        // Add marker to lists
                        synchronized (markers) {
                            if (!markers.contains(bm)) {
                                markers.add(bm);
                                proxyToMarker.put(boatProxy, bm);
                                markerToProxy.put(bm, boatProxy);
                            }
                        }
                        first = false;
                    }

                    wwPanel.wwCanvas.redraw();
                }

                @Override
                public void waypointsComplete() {
                }

                @Override
                public void eventOccurred(InputEvent e) {
                }

                @Override
                public void waypointsUpdated() {
                }
            });
        }
    }

    @Override
    public void proxyRemoved(ProxyInt proxy) {
        if (proxy instanceof BoatProxy) {
            BoatProxy boatProxy = (BoatProxy) proxy;
            if (proxyToMarker.containsKey(boatProxy)) {
                BoatMarker boatMarker = proxyToMarker.get(boatProxy);
                markerToProxy.remove(boatMarker);
                proxyToMarker.remove(proxy);
                synchronized (markers) {
                    markers.remove(boatMarker);
                }
                wwPanel.wwCanvas.redraw();
            }
        }
    }

    protected void initRenderableLayer() {
        if (wwPanel == null) {
            return;
        }

        markerLayer = new MarkerLayer();
        markerLayer.setOverrideMarkerElevation(true);
        markerLayer.setElevation(10d);
//        markerLayer.setKeepSeparated(false);
        markerLayer.setPickEnabled(false);
        markerLayer.setMarkers(markers);
        wwPanel.wwCanvas.getModel().getLayers().add(markerLayer);
        renderableLayer = new RenderableLayer();
        renderableLayer.setPickEnabled(false);
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
    }

    protected void initButtons() {
        if (wwPanel == null) {
            return;
        }

        topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel = new JPanel(new BorderLayout());

        if (enabledModes.contains(ControlMode.TELEOP)) {
            teleopButton = new JButton("Teleop");
            teleopButton.setEnabled(false);
            btmPanel.add(teleopButton);
            teleopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setControlMode(ControlMode.TELEOP);
                }
            });
        }
        if (enabledModes.contains(ControlMode.POINT)) {
            pointButton = new JButton("Point");
            pointButton.setEnabled(false);
            btmPanel.add(pointButton);
            pointButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setControlMode(ControlMode.POINT);
                }
            });
        }
        if (enabledModes.contains(ControlMode.PATH)) {
            pathButton = new JButton("Path");
            pathButton.setEnabled(false);
            btmPanel.add(pathButton);
            pathButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setControlMode(ControlMode.PATH);
                }
            });
        }
        if (enabledModes.size() > 0) {
            cancelButton = new JButton("Cancel");
            cancelButton.setEnabled(false);
            btmPanel.add(cancelButton);
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    cancelAssignedWaypoints();
                    setControlMode(ControlMode.NONE);
                    hideExpandables();
                }
            });
        }

        autoButton = new JButton("Auto");
        autoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                // Do graphical stuff here - BoatTeleopPanel will actually handle the switch to Autonomous mode
                setControlMode(ControlMode.NONE);
                hideExpandables();
            }
        });
        autoButton.setEnabled(false);
        btmPanel.add(autoButton);

        buttonPanel.add(topPanel, BorderLayout.NORTH);
        buttonPanel.add(btmPanel, BorderLayout.SOUTH);
        wwPanel.buttonPanels.add(buttonPanel, BorderLayout.SOUTH);
        wwPanel.buttonPanels.revalidate();
    }

    public void initExpandables() {
        videoP = new VideoFeedPanel();
        velocityP = new VelocityPanel(this);
        videoP.setVisible(false);
        velocityP.setVisible(false);
        topPanel.add(videoP);
        topPanel.add(velocityP);
        wwPanel.buttonPanels.revalidate();
    }

    public void hideExpandables() {
        videoP.setVisible(false);
        velocityP.setVisible(false);
        cancelButton.setText("Cancel");
        wwPanel.revalidate();
    }

    public void showExpandables() {
        Dimension mapDim = wwPanel.wwCanvas.getSize();
        int height = Math.min(mapDim.width, mapDim.height) / 4;
        velocityP.setPreferredSize(new Dimension(mapDim.width / 2, height));
        videoP.setPreferredSize(new Dimension(mapDim.width / 2, height));
        videoP.setVisible(true);
        velocityP.setVisible(true);
        cancelButton.setText("Collapse");
        wwPanel.revalidate();
    }

    public void setControlMode(ControlMode controlMode) {
        clearPath();
        velocityP.stopBoat();
        this.controlMode = controlMode;
        switch (controlMode) {
            case TELEOP:
                cancelAssignedWaypoints();
                showExpandables();
                enableTeleop();
                break;
            case POINT:
                hideExpandables();
                disableTeleop();
                break;
            case PATH:
                hideExpandables();
                disableTeleop();
                break;
            case NONE:
                hideExpandables();
                disableTeleop();
                break;
        }
    }

    public void enableTeleop() {
        velocityP.enableTeleop(true);
    }

    public void disableTeleop() {
        velocityP.enableTeleop(false);
    }

    public void selectMarker(Marker boatMarker) {
        // Marker stuff
        if (selectedMarker != null) {
            // Unselect previous marker
            selectedMarker.getAttributes().setHeadingMaterial(UNSELECTED_MAT);
            // Remove marker from renderables list and re-add it so it is rendered last 
            //  (low opacity renderables above it will still hide it)
            // Doing the below to avoid concurrent modification exception
            ArrayList<Marker> markersClone = null;
            synchronized (markers) {
                markerLayer.getMarkers();
                markersClone = (ArrayList<Marker>) markers.clone();
                markersClone.remove(selectedMarker);
                markersClone.add(selectedMarker);
                markerLayer.setMarkers(markersClone);
                markers = markersClone;
            }
            wwPanel.wwCanvas.redrawNow();
        }
        selectedMarker = boatMarker;
        if (boatMarker != null) {
            boatMarker.getAttributes().setHeadingMaterial(SELECTED_MAT);
        }

        // Proxy stuff
        if (selectedProxy != null) {
            // Stop previous boat if it is locked in teleoperation mode
            disableTeleop();
        }
        BoatProxy boatProxy = null;
        boolean enabled = false;
        if (boatMarker != null) {
            boatProxy = markerToProxy.get(boatMarker);
            enabled = true;
            // Update teleop panel's proxy 
            _vehicle = boatProxy.getVehicleServer();
            velocityP.setVehicle(boatProxy.getVehicleServer());
        } else {
            // Remove teleop panel's proxy and hide teleop panel
            _vehicle = null;
            velocityP.setVehicle(null);
            setControlMode(ControlMode.NONE);
            hideExpandables();
        }
        selectedProxy = boatProxy;
        // Disable buttons if selected proxy is null
        if (teleopButton != null) {
            teleopButton.setEnabled(enabled);
        }
        if (pointButton != null) {
            pointButton.setEnabled(enabled);
        }
        if (pathButton != null) {
            pathButton.setEnabled(enabled);
        }
        if (stationKeepButton != null) {
            stationKeepButton.setEnabled(enabled);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(enabled);
        }
        if (autoButton != null) {
            autoButton.setEnabled(enabled);
        }
    }

    public void doPoint(Position point) {
        ArrayList<Location> waypoints = new ArrayList<Location>();
        waypoints.add(new Location(point.latitude.degrees, point.longitude.degrees, 0));
        PathUtm path = new PathUtm(waypoints);

        UUID eventId = null;
        if (proxyToWpEventId.containsKey(selectedProxy)) {
            eventId = proxyToWpEventId.get(selectedProxy);
        } else {
            eventId = UUID.randomUUID();
            proxyToWpEventId.put(selectedProxy, eventId);
        }
        Hashtable<ProxyInt, Path> proxyPath = new Hashtable<ProxyInt, Path>();
        proxyPath.put(selectedProxy, path);
        ProxyExecutePath proxyEvent = new ProxyExecutePath(eventId, null, proxyPath);
        selectedProxy.updateCurrentSeqEvent(proxyEvent);
        autoButton.setSelected(true);
    }

    public void doPath(ArrayList<Position> positions) {
        ArrayList<Location> waypoints = new ArrayList<Location>();
        for (int i = 0; i < positions.size(); i++) {
            waypoints.add(new Location(positions.get(i).latitude.degrees, positions.get(i).longitude.degrees, 0.0));
        }
        PathUtm path = new PathUtm(waypoints);

        UUID eventId = null;
        if (proxyToWpEventId.containsKey(selectedProxy)) {
            eventId = proxyToWpEventId.get(selectedProxy);
        } else {
            eventId = UUID.randomUUID();
            proxyToWpEventId.put(selectedProxy, eventId);
        }
        Hashtable<ProxyInt, Path> proxyPath = new Hashtable<ProxyInt, Path>();
        proxyPath.put(selectedProxy, path);
        ProxyExecutePath proxyEvent = new ProxyExecutePath(eventId, null, proxyPath);
        selectedProxy.updateCurrentSeqEvent(proxyEvent);
        autoButton.setSelected(true);
    }

    public void stationKeep(Position point) {
        Location location = new Location(point.latitude.degrees, point.longitude.degrees, 0);

        UUID eventId = null;
        if (proxyToWpEventId.containsKey(selectedProxy)) {
            eventId = proxyToWpEventId.get(selectedProxy);
        } else {
            eventId = UUID.randomUUID();
            proxyToWpEventId.put(selectedProxy, eventId);
        }
        Hashtable<ProxyInt, Location> proxyPoint = new Hashtable<ProxyInt, Location>();
        proxyPoint.put(selectedProxy, location);
        ProxyStationKeep proxyEvent = new ProxyStationKeep(eventId, null, proxyPoint, 20.0);
        selectedProxy.updateCurrentSeqEvent(proxyEvent);
        autoButton.setSelected(true);
    }

    public void clearPath() {
        if (polyline != null) {
            renderableLayer.removeRenderable(polyline);
        }
        selectedPositions = new ArrayList<Position>();
    }

    public void cancelAssignedWaypoints() {
        if (selectedProxy == null) {
            return;
        }
        // Get currently executing waypoints: if it is what we previously assigned, cancel them
        UUID eventId = proxyToWpEventId.get(selectedProxy);
        if (eventId != null) {
            OutputEvent curEvent = selectedProxy.getCurSequentialEvent();
            if (curEvent != null && curEvent.getId().equals(eventId)) {
                selectedProxy.cancelCurrentSeqEvent();
            }
        }
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        //
        // Markups
        supportedMarkups.add(RelevantProxy.Proxies.ALL_PROXIES);
        supportedMarkups.add(RelevantProxy.Proxies.RELEVANT_PROXIES);
    }

    @Override
    public int getCreationWidgetScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationWidgetScore(supportedCreationClasses, supportedMarkups, type, markups);
    }

    @Override
    public int getSelectionWidgetScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionWidgetScore(supportedSelectionClasses, supportedMarkups, type, markups);
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        return MarkupComponentHelper.getMarkupWidgetScore(supportedMarkups, markups);
    }

    @Override
    public MarkupComponentWidget addCreationWidget(MarkupComponent component, Type type, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = null;
        for (Markup markup : markups) {
            if (markup instanceof RelevantProxy) {
                RelevantProxy relevantProxy = (RelevantProxy) markup;
                if (relevantProxy.proxies == RelevantProxy.Proxies.ALL_PROXIES) {
                    //@todo What control modes?
                    widget = new RobotWidget((WorldWindPanel) component);
                } else if (relevantProxy.proxies == RelevantProxy.Proxies.RELEVANT_PROXIES) {
                    //@todo Need to pass in token list for this
                    //@todo What control modes?
                    widget = new RobotWidget((WorldWindPanel) component);
                }
            }
        }
        return widget;
    }

    @Override
    public MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object object, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = null;
        for (Markup markup : markups) {
            if (markup instanceof RelevantInformation) {
                RelevantInformation info = (RelevantInformation) markup;
                if (info.information == RelevantInformation.Information.SPECIFY) {
                    if (info.visualization == RelevantInformation.Visualization.HEATMAP) {
                        widget = new SensorDataWidget((WorldWindPanel) component);
                    }
                }
            }
        }
        return widget;
    }

    public MarkupComponentWidget handleCreationHashtable(ParameterizedType type) {
        MarkupComponentWidget widget = null;
        return widget;
    }

    public MarkupComponentWidget handleSelectionHashtable(Hashtable hashtable, Object keyObject, Object valueObject) {
        MarkupComponentWidget widget = null;
        if (keyObject == null || valueObject == null) {
            return null;
        }
        return widget;
    }

    @Override
    public Object getComponentValue(Field field) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean setComponentValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleMarkups(ArrayList<Markup> markups, MarkupManager manager) {
    }

    @Override
    public void disableMarkup(Markup markup) {
    }

    // Callback that handles GUI events that change velocity
    public void updateVelocity() {
        // Check if there is already a command queued up, if not, queue one up
        if (!_sentVelCommand.getAndSet(true)) {
            // Send one command immediately
            sendVelocity();
            // Queue up a command at the end of the refresh timestep
            _timer.schedule(new UpdateVelTask(), DEFAULT_COMMAND_MS);
        } else {
            _queuedVelCommand.set(true);
        }
    }

    // Simple update task that periodically checks whether velocity needs updating
    class UpdateVelTask extends TimerTask {

        @Override
        public void run() {
            if (_queuedVelCommand.getAndSet(false)) {
                sendVelocity();
                _timer.schedule(new UpdateVelTask(), DEFAULT_COMMAND_MS);
            } else {
                _sentVelCommand.set(false);
            }
        }
    }

    // Sets velocities from sliders to control proxy
    protected void sendVelocity() {
        if (_vehicle != null) {
            Twist twist = new Twist();
            twist.dx(telThrustFrac);
            twist.drz(telRudderFrac);
            _vehicle.setVelocity(twist, null);
        }
    }

    public void stopBoat() {
        if (_vehicle != null) {
            telRudderFrac = 0.0;
            telThrustFrac = 0;
            updateVelocity();
        }
    }
}
