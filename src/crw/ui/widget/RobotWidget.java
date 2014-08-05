package crw.ui.widget;

import crw.Coordinator;
import crw.ui.worldwind.WorldWindWidgetInt;
import crw.ui.component.WorldWindPanel;
import crw.CrwHelper;
import crw.event.output.proxy.ProxyExecutePath;
import crw.Conversion;
import crw.proxy.BoatProxy;
import crw.ui.BoatMarker;
import crw.ui.VideoFeedPanel;
import crw.ui.teleop.GainsPanel;
import crw.ui.teleop.VelocityPanel;
import edu.cmu.ri.crw.AsyncVehicleServer;
import edu.cmu.ri.crw.data.Twist;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.event.GeneratedEventListenerInt;
import sami.event.InputEvent;
import sami.event.InterruptEventIE;
import sami.event.OutputEvent;
import sami.event.ProxyInterruptEventIE;
import sami.markup.Markup;
import sami.markup.RelevantInformation;
import sami.markup.RelevantProxy;
import sami.mission.InterruptType;
import sami.mission.MissionPlanSpecification;
import sami.mission.Token;
import sami.path.Location;
import sami.path.Path;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;
import sami.proxy.ProxyServerListenerInt;
import sami.ui.MissionDisplay;
import sami.ui.MissionMonitor;
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

        TELEOP, POINT, PATH, NONE, COORD
    };
    private static final Logger LOGGER = Logger.getLogger(RobotWidget.class.getName());
    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    public enum CoordType {

        EASY, COST_BASED
    };
    // This increases the "grab" radius for proxy markers to make selecting a proxy easier
    private final int CLICK_RADIUS = 25;
    private boolean visible = true;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Position> selectedPositions = new ArrayList<Position>();
    private ArrayList<Position> posWorking = new ArrayList<Position>();
    private BoatProxy selectedProxy = null;
    private VelocityPanel velocityP;
    private GainsPanel gainsP;
    private ControlMode controlMode = ControlMode.NONE;
    private CoordType coordType = CoordType.EASY;
    private Hashtable<GeneratedEventListenerInt, UUID> listenerTable = new Hashtable<GeneratedEventListenerInt, UUID>();
    private Hashtable<BoatProxy, BoatMarker> proxyToMarker = new Hashtable<BoatProxy, BoatMarker>();
    private Hashtable<BoatMarker, BoatProxy> markerToProxy = new Hashtable<BoatMarker, BoatProxy>();
    private Hashtable<BoatProxy, UUID> proxyToWpEventId = new Hashtable<BoatProxy, UUID>();
    private HashMap<BoatMarker, ArrayList<Position>> decisions = new HashMap<BoatMarker, ArrayList<Position>>();
    private JButton teleopButton, pointButton, pathButton, cancelButton, autoButton, coordButton;
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
    // BoatMarker selection
    private final Object highlightedLock = new Object();
    private boolean highlightedBoatMarkerChanged = false;
    private BoatMarker highlightedBoatMarker = null;

    private Coordinator coordinator = new Coordinator();

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

    private Boolean working = false;

    public RobotWidget() {
        populateLists();
    }

    public RobotWidget(WorldWindPanel wwPanel) {
        this(wwPanel, null);
    }

    public RobotWidget(final WorldWindPanel wwPanel, List<ControlMode> enabledModes) {
        this.wwPanel = wwPanel;
        this.enabledModes = enabledModes;
        if (enabledModes == null) {
            this.enabledModes = new ArrayList<ControlMode>();
        }

        for (BoatMarker b : markerToProxy.keySet()) {
            decisions.put(b, new ArrayList<Position>());
        }

        coordinator = new Coordinator();
        coordinator.setMarkerToProxy(markerToProxy);
        coordinator.initDecisions();

        initRenderableLayer();
        initButtons();
        initExpandables();
        Engine.getInstance().getProxyServer().addListener(this);

        // Set up a SelectListener to know when the cursor is over a BoatMarker
        wwPanel.wwCanvas.addSelectListener(new SelectListener() {

            @Override
            public void selected(SelectEvent event) {
                // Click type SelectEvents are only generated if an event is generated, and
                //  are handled before MouseEvents, which makes deselecting a BoatMarker impossible from a SelectEvent
                // Instead, keep track of if the cursor is over a BoatMarker and let the MouseListener handle selection/deselection

                if (event.getEventAction().equals(SelectEvent.ROLLOVER)
                        && event.hasObjects()
                        && event.getTopObject() instanceof BoatMarker) {
                    if (event.getTopObject() != highlightedBoatMarker) {
                        synchronized (highlightedLock) {
                            highlightedBoatMarker = (BoatMarker) event.getTopObject();
                            highlightedBoatMarkerChanged = true;
                        }
                    }
                } else if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
                    if (highlightedBoatMarker != null) {
                        synchronized (highlightedLock) {
                            highlightedBoatMarker = null;
                            highlightedBoatMarkerChanged = true;
                        }
                    }
                }
            }
        });
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
        if (wwd.getCurrentPosition() == null) {
            LOGGER.warning("wwd.getCurrentPosition() is NULL");
            return false;
        }

        Position clickPosition = wwd.getView().computePositionFromScreenPoint(evt.getX(), evt.getY());
//        System.out.println("click : " + clickPosition.toString() + " controlMode: " + controlMode);

        working = false;

        if (evt.isShiftDown()) {
            cancelAssignedWaypoints();
            controlMode = ControlMode.COORD;
            working = true;
            clickPosition = wwd.getView().computePositionFromScreenPoint(evt.getX(), evt.getY());
//            System.out.println("Clicked with shift");
        }
        if (evt.getButton() == MouseEvent.BUTTON3) {
//            System.out.println("Interrput!!!! :D");
//            Token t = Engine.getInstance().createToken(new TokenSpecification("Interrupt", TokenSpecification.TokenType.Interrupt, null));
            PlanManager pm = Engine.getInstance().getPlanManager();
            
//            System.out.println("plans???: "+Engine.getInstance().getPlans());
            pm = Engine.getInstance().getPlans().get(0);
            pm.enterInterruptPlace(InterruptType.GENERAL, null);
//            pm.eventGenerated(new InterruptEventIE());

//            BoatProxy b = new BoatProxy(null, Color.yellow, CLICK_RADIUS, null);

        }

        switch (controlMode) {
            case POINT:
                // Calculate elevation above sea level at click position
                doPoint(CrwHelper.getPositionAsl(wwd.getView().getGlobe(), wwd.getCurrentPosition()));
                setControlMode(ControlMode.NONE);
                return true;
            case PATH:
                // Calculate elevation above sea level at click position
                selectedPositions.add(CrwHelper.getPositionAsl(wwd.getView().getGlobe(), wwd.getCurrentPosition()));
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
            case COORD:

                if (clickPosition != null) {
                    if (markers.size() == 0) {
//                        System.out.println("no boats");
                        return true;
                    }

                    selectedPositions.add(clickPosition);
                    // Update temporary path

                    SurfaceCircle circle = new SurfaceCircle(new LatLon(clickPosition.getLatitude(), clickPosition.getLongitude()), 3);
                    ShapeAttributes attributes = new BasicShapeAttributes();
                    attributes.setInteriorMaterial(Material.YELLOW);
                    attributes.setInteriorOpacity(0.5);
                    attributes.setOutlineMaterial(Material.YELLOW);
                    attributes.setOutlineOpacity(0.5);
                    attributes.setOutlineWidth(1);
                    circle.setAttributes(attributes);
                    renderableLayer.addRenderable(circle);

                    wwd.redraw();
                    if (evt.getClickCount() > 1 || working) {

                        switch (coordType) {
                            case EASY:

                                coordinator.setMethod(Coordinator.Method.EASY);
                                executePath(new ArrayList<BoatMarker>(markerToProxy.keySet()), coordinator.taskAssignment(selectedPositions));

                                break;
                            case COST_BASED:

                                coordinator.setMethod(Coordinator.Method.COST);
                                executePath(new ArrayList<BoatMarker>(markerToProxy.keySet()), coordinator.taskAssignment(selectedPositions));

                                break;
                        }

//                        System.out.println("Coord executed");

                        working = false;
                        clearPath();
                        setControlMode(ControlMode.NONE);
                    }
                    return true;
                }
                return true;
            case NONE:
                // Selected or deselected a proxy?
                synchronized (highlightedLock) {
                    if (highlightedBoatMarkerChanged) {
                        selectMarker(highlightedBoatMarker);

                        highlightedBoatMarkerChanged = false;
                        return true;
                    }
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
                                coordinator.setMarkerToProxy(markerToProxy);
                                coordinator.initDecisions();
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
                coordinator.setMarkerToProxy(markerToProxy);
                proxyToMarker.remove(proxy);
                synchronized (markers) {
                    markers.remove(boatMarker);
                }
                coordinator.initDecisions();
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
//        markerLayer.setElevation(10d);
        markerLayer.setKeepSeparated(false);
        markerLayer.setPickEnabled(true);
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

        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
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
        if (enabledModes.contains(ControlMode.COORD)) {
            coordButton = new JButton("Coord");
            coordButton.setEnabled(true);
            btmPanel.add(coordButton);
            coordButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    cancelAssignedWaypoints();
                    for (BoatMarker b : markerToProxy.keySet()) {
                        decisions.put(b, new ArrayList<Position>());
                    }
                    final int numButtons = 2;
                    JRadioButton[] radioButtons = new JRadioButton[numButtons];
                    final ButtonGroup group = new ButtonGroup();

                    JButton showItButton = null;

//                    final String defaultMessageCommand = "default";
//                    final String yesNoCommand = "yesno";
                    radioButtons[0] = new JRadioButton("Easy Coord");
//                    radioButtons[0].setActionCommand(defaultMessageCommand);

                    radioButtons[1] = new JRadioButton("Sequential System Item Auctions");
//                    radioButtons[1].setActionCommand(yesNoCommand);

                    for (int i = 0; i < numButtons; i++) {
                        group.add(radioButtons[i]);
                    }
                    radioButtons[0].setSelected(true);

                    int numChoices = radioButtons.length;
                    JPanel box = new JPanel(new GridLayout(2, 1));
                    for (int i = 0; i < numChoices; i++) {
                        box.add(radioButtons[i]);
                    }

                    JOptionPane.showMessageDialog(topPanel, box);

                    for (int i = 0; i < numChoices; i++) {
                        if (radioButtons[i].isSelected() && i == 0) {
                            coordType = CoordType.EASY;
                        }
                        if (radioButtons[i].isSelected() && i == 1) {
                            coordType = CoordType.COST_BASED;
                        }
                    }

                    selectedProxy = null;
                    setControlMode(ControlMode.COORD);
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
                    coordinator.clearDecisions();
                    posWorking.clear();
                    for (BoatMarker b : markerToProxy.keySet()) {
                        decisions.put(b, new ArrayList<Position>());
                    }
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
        videoP.setVisible(false);
        velocityP = new VelocityPanel(this);
        velocityP.setVisible(false);
        gainsP = new GainsPanel();
        gainsP.setVisible(false);
        topPanel.add(videoP);
        topPanel.add(velocityP);
        topPanel.add(gainsP);
        wwPanel.buttonPanels.revalidate();
    }

    public void hideExpandables() {
        videoP.setVisible(false);
        velocityP.setVisible(false);
        gainsP.setVisible(false);
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
        gainsP.setVisible(true);
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
            case COORD:
                hideExpandables();
                disableTeleop();
//                cancelAssignedWaypoints();
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
            gainsP.setProxy(boatProxy);
        } else {
            // Remove teleop panel's proxy and hide teleop panel
            _vehicle = null;
            velocityP.setVehicle(null);
            gainsP.setProxy(null);
            setControlMode(ControlMode.NONE);
            hideExpandables();
        }
        selectedProxy = boatProxy;

        PlanManager pm = Engine.getInstance().getPlanManager();

//        pm.eventGenerated(new ProxyInterruptEvent(pm.missionId, pm.missionId, boatProxy));
//        pm.eventGenerated(new ProxyInterruptEventIE(null, null, (ProxyInt)boatProxy));
        
        // Disable buttons if selected proxy is null
        if (teleopButton != null) {
            teleopButton.setEnabled(enabled);
        }
        if (coordButton != null) {
            coordButton.setEnabled(enabled);
        }
        if (pointButton != null) {
            pointButton.setEnabled(enabled);
        }
        if (pathButton != null) {
            pathButton.setEnabled(enabled);
        }
//        if (stationKeepButton != null) {
//            stationKeepButton.setEnabled(enabled);
//        }
        if (cancelButton != null) {
            cancelButton.setEnabled(enabled);
        }
        if (autoButton != null) {
            autoButton.setEnabled(enabled);
        }
    }

//    public void doEasyCoord(ArrayList<Position> positions) {
//
//        //ArrayList<ArrayList<Position>> 
////        HashMap<BoatMarker, ArrayList<Position>> decisions = new HashMap<BoatMarker, ArrayList<Position>>();
//        ArrayList<Position> dummyPos = new ArrayList<Position>(positions);
//        ArrayList<BoatMarker> boats = new ArrayList<BoatMarker>(markerToProxy.keySet());
//
////        for (BoatMarker b : boats) {
////            decisions.put(b, new ArrayList<Position>());
////        }
//
//        Double min = Double.MAX_VALUE;
//
//        do {
//            HashMap<BoatMarker, Position> nearest = new HashMap<BoatMarker, Position>();
//
//            for (int i = 0; i < boats.size(); i++) {
//                BoatMarker b = boats.get(i);
//
//                final Position boatPos = (decisions.get(b).isEmpty()) ? b.getPosition() : decisions.get(b).get(decisions.get(b).size() - 1);
//
//                Collections.sort(dummyPos, new Comparator<Position>() {
//                    @Override
//                    public int compare(Position t, Position t1) {
//                        if (computeDistance(boatPos, t) < computeDistance(boatPos, t1)) {
//                            return -1;
//                        } else if (computeDistance(boatPos, t) > computeDistance(boatPos, t1)) {
//                            return 1;
//                        }
//
//                        return 0;
//
//                    }
//                });
//
//                Position insert = dummyPos.isEmpty() ? null : dummyPos.get(0);
//
//                if (insert == null) {
//                    break;
//                }
//
//                nearest.put(b, insert);
//                System.out.println("near boat: " + b + " pos: " + nearest.get(b));
//            }
//
//            HashMap<BoatMarker, Double> best = new HashMap<BoatMarker, Double>();
//
//            for (Map.Entry<BoatMarker, Position> entry : nearest.entrySet()) {
//
//                Position comparing = (decisions.get(entry.getKey()).isEmpty()) ? entry.getKey().getPosition() : decisions.get(entry.getKey()).get(decisions.get(entry.getKey()).size() - 1);
//
//                best.put(entry.getKey(), computeDistance(comparing, entry.getValue()));
//                System.out.println("distance: " + best.get(entry.getKey()) + " for " + entry.getKey() + " and pos: " + entry.getValue());
//            }
//
//            Entry<BoatMarker, Double> bestPos = Collections.min(best.entrySet(), new Comparator<Entry<BoatMarker, Double>>() {
//                public int compare(Entry<BoatMarker, Double> entry1, Entry<BoatMarker, Double> entry2) {
//                    return entry1.getValue().compareTo(entry2.getValue());
//                }
//            });
//
//            System.out.println("boat: " + bestPos.getKey() + " pos: " + bestPos.getValue());
//
//            decisions.get(bestPos.getKey()).add(nearest.get(bestPos.getKey()));
//            dummyPos.remove(nearest.get(bestPos.getKey()));
//
//        } while (!dummyPos.isEmpty());
//
//        System.out.println("Positions: " + positions.toString());
//
//        for (BoatMarker b : boats) {
//            System.out.println("lista for " + b.toString() + ": " + decisions.get(b).toString());
//        }
//
//        cancelAssignedWaypoints();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(RobotWidget.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        executePath(boats, decisions);
//
//    }
    private void executePath(ArrayList<BoatMarker> boats, final HashMap<BoatMarker, ArrayList<Position>> pathExec) {

//        System.out.println("sel proxy: "+selectedProxy);
//        System.out.println("Execute Path");

        for (BoatProxy b : markerToProxy.values()) {
//                System.out.println("boat: "+b);
            for (OutputEvent e : b.getEvents()) {
                b.abortEvent(e.getId());
            }
        }

        for (BoatMarker b : boats) {
//            System.out.println("dec paths: " + pathExec.get(b));
            ArrayList<Location> waypoints = new ArrayList<Location>();
            for (int i = 0; i < pathExec.get(b).size(); i++) {
                waypoints.add(new Location(pathExec.get(b).get(i).latitude.degrees, pathExec.get(b).get(i).longitude.degrees, 0.0));
            }

            PathUtm path = new PathUtm(waypoints);

            UUID eventId = null;
            if (proxyToWpEventId.containsKey(markerToProxy.get(b))) {
                eventId = proxyToWpEventId.get(markerToProxy.get(b));
            } else {
                eventId = UUID.randomUUID();
                proxyToWpEventId.put(markerToProxy.get(b), eventId);
            }
            Hashtable<ProxyInt, Path> proxyPath = new Hashtable<ProxyInt, Path>();
            proxyPath.put(markerToProxy.get(b), path);
            ProxyExecutePath proxyEvent = new ProxyExecutePath(eventId, null, proxyPath);
            for (Path p : proxyEvent.getProxyPaths().values()) {
//                System.out.println("proxy exec path: " + p.toString());
            }

            markerToProxy.get(b).updateCurrentSeqEvent(proxyEvent);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(true);
        }

        (new Thread() {
            @Override
            public void run() {
                boolean guard;
                do {
                    guard = false;
                    for (BoatMarker b : markerToProxy.keySet()) {
                        ArrayList<Position> copy = new ArrayList<Position>(coordinator.getDecisions().get(b));
                        for (Position p : copy) {
                            Double d = Coordinator.computeDistance(b.getProxy().getPosition(), p);
//                            System.out.println("distance: "+d);
                            if (d == 0.0) {
                                coordinator.getDecisions().get(b).remove(p);
//                                System.out.println("distance equals: " + d);
                                LatLon m = new LatLon(p.getLatitude(), p.getLongitude());

                            } else if (Math.abs(d) < 3) {
                                coordinator.getDecisions().get(b).remove(p);
//                                System.out.println("distance error: " + d);

                            }
                        }
                    }

                    for (BoatMarker b : markerToProxy.keySet()) {
                        if (!coordinator.getDecisions().get(b).isEmpty()) {
                            guard = true;

                        }
                    }

                } while (guard);

            }
        }).start();
    }

//    public void doCostBasedCoord(ArrayList<Position> positions) {
//
//        System.out.println("Cost based");
//        HashMap<BoatMarker, ArrayList<Position>> dummyDec = new HashMap<>();
//
//        ArrayList<Position> dummyPos = new ArrayList<Position>(positions);
//        ArrayList<BoatMarker> boats = new ArrayList<BoatMarker>(markerToProxy.keySet());
//
//        do {
//
//            HashMap<BoatMarker, ArrayList<Position>> possibilities = new HashMap<>();
//            HashMap<BoatMarker, Double> possibilitiesCost = new HashMap<>();
//
//            Random r = new Random();
//            Position test = dummyPos.get(r.nextInt(dummyPos.size()));
//
//            for (BoatMarker b : boats) {
//                ArrayList<Position> tmp;
//                if(decisions.get(b).isEmpty()){
//                    tmp = new ArrayList<>();
//                }else{
//                    tmp = new ArrayList<>(decisions.get(b));
//                }
//
//                tmp.add(test);
//                
//                HashMap<Double, ArrayList<Position>> allPathsCost;
//                
//                if(decisions.get(b).isEmpty()){
//                    allPathsCost = computeBestPath(b, tmp);
//                }else if(working){
//                    allPathsCost = computeNewInsertPath(b, tmp);
//                }else{
//                    allPathsCost = computeBestPath(b, tmp);
//                }
//
//                ArrayList<Entry<Double, ArrayList<Position>>> listPaths = new ArrayList<>(allPathsCost.entrySet());
//                Entry<Double, ArrayList<Position>> min = Collections.min(listPaths, new Comparator<Entry<Double, ArrayList<Position>>>() {
//
//                    @Override
//                    public int compare(Entry<Double, ArrayList<Position>> t, Entry<Double, ArrayList<Position>> t1) {
//                        return t.getKey().compareTo(t1.getKey());
//                    }
//                });
//                
//                possibilities.put(b, min.getValue());
//                possibilitiesCost.put(b, min.getKey());
//
//            }
//
//            ArrayList<Entry<BoatMarker, Double>> listCosts = new ArrayList<>(possibilitiesCost.entrySet());
//            Entry<BoatMarker, Double> min = Collections.min(listCosts, new Comparator<Entry<BoatMarker, Double>>() {
//
//                @Override
//                public int compare(Entry<BoatMarker, Double> t, Entry<BoatMarker, Double> t1) {
//                    return t.getValue().compareTo(t1.getValue());
//                }
//            });
//
//            decisions.put(min.getKey(), possibilities.get(min.getKey()));
//            dummyPos.remove(test);
//
//        } while (!dummyPos.isEmpty());
//
//        cancelAssignedWaypoints();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(RobotWidget.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        executePath(boats, decisions);
//
//    }
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

//    public void stationKeep(Position point) {
//        Location location = new Location(point.latitude.degrees, point.longitude.degrees, 0);
//
//        UUID eventId = null;
//        if (proxyToWpEventId.containsKey(selectedProxy)) {
//            eventId = proxyToWpEventId.get(selectedProxy);
//        } else {
//            eventId = UUID.randomUUID();
//            proxyToWpEventId.put(selectedProxy, eventId);
//        }
//        Hashtable<ProxyInt, Location> proxyPoint = new Hashtable<ProxyInt, Location>();
//        proxyPoint.put(selectedProxy, location);
//        ProxyStationKeep proxyEvent = new ProxyStationKeep(eventId, null, proxyPoint, 20.0);
//        selectedProxy.updateCurrentSeqEvent(proxyEvent);
//        autoButton.setSelected(true);
//    }
//
    public void clearPath() {
        if (polyline != null) {
            renderableLayer.removeRenderable(polyline);
        }

        for (Object r : renderableLayer.getRenderables()) {
            if (r instanceof SurfaceCircle) {
                renderableLayer.removeRenderable((SurfaceCircle) r);
            }
        }

        selectedPositions = new ArrayList<Position>();
    }

    public void cancelAssignedWaypoints() {
//        coordinator.clearDecisions();
//        System.out.println("Assigned waypoints");
        if (selectedProxy == null) {
            for (BoatProxy b : markerToProxy.values()) {
                for (UUID event : proxyToWpEventId.values()) {
                    if (event != null) {
                        OutputEvent curEvent = b.getCurSequentialEvent();
                        if (curEvent != null && curEvent.getId().equals(event)) {
                            b.cancelCurrentSeqEvent();
                        }
                    }
                }
            }
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
    
    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        return (ArrayList<Class>)supportedCreationClasses.clone();
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
