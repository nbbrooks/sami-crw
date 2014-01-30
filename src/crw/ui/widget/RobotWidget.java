package crw.ui.widget;

import crw.ui.component.WorldWindPanel;
import crw.Helper;
import crw.event.output.proxy.ProxyExecutePath;
import crw.proxy.BoatProxy;
import crw.ui.BoatMarker;
import crw.ui.BoatTeleopPanel;
import crw.ui.VideoFeedPanel;
import crw.ui.component.UiWidget;
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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.event.GeneratedEventListenerInt;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.markup.Attention;
import sami.markup.RelevantProxy;
import sami.path.Location;
import sami.path.Path;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;
import sami.proxy.ProxyServerListenerInt;

/**
 *
 * @author nbb
 */
public class RobotWidget extends UiWidget implements WorldWindWidgetInt, ProxyServerListenerInt {

//    static {
//        computeUiComponent();
//    }
    public enum ControlMode {

        TELEOP, POINT, PATH, NONE
    };
    // This increases the "grab" radius for proxy markers to make selecting a proxy easier
    private final int CLICK_RADIUS = 25;
    private boolean visible = true;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Position> selectedPositions = new ArrayList<Position>();
    private BoatProxy selectedProxy = null;
    private BoatTeleopPanel teleopP;
    private ControlMode controlMode = ControlMode.NONE;
    private Hashtable<GeneratedEventListenerInt, UUID> listenerTable = new Hashtable<GeneratedEventListenerInt, UUID>();
    private Hashtable<BoatProxy, BoatMarker> proxyToMarker = new Hashtable<BoatProxy, BoatMarker>();
    private Hashtable<BoatMarker, BoatProxy> markerToProxy = new Hashtable<BoatMarker, BoatProxy>();
    private Hashtable<BoatProxy, UUID> proxyToWpEventId = new Hashtable<BoatProxy, UUID>();
    private JButton teleopButton, pointButton, pathButton, cancelButton, autoButton;
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
            final BoatMarker bm = new BoatMarker(boatProxy, boatProxy.getCurrLoc(), new BasicMarkerAttributes(new Material(boatProxy.getColor()), BasicMarkerShape.ORIENTED_SPHERE, 0.9));
            bm.getAttributes().setHeadingMaterial(UNSELECTED_MAT);
            bm.setPosition(boatProxy.getCurrLoc());

            // Create listener to update marker pose
            boatProxy.addListener(new ProxyListenerInt() {
                boolean first = true;

                public void poseUpdated() {
                    bm.setPosition(bm.getProxy().getCurrLoc());
                    bm.setHeading(Angle.fromRadians(Math.PI / 2.0 - bm.getProxy().getPose().pose.getRotation().toYaw()));

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
        teleopP = new BoatTeleopPanel(autoButton);
        videoP.setVisible(false);
        teleopP.setVisible(false);
        topPanel.add(videoP);
        topPanel.add(teleopP);
        wwPanel.buttonPanels.revalidate();
    }

    public void hideExpandables() {
        videoP.setVisible(false);
        teleopP.setVisible(false);
        cancelButton.setText("Cancel");
        wwPanel.revalidate();
    }

    public void showExpandables() {
        Dimension mapDim = wwPanel.wwCanvas.getSize();
        int height = Math.min(mapDim.width, mapDim.height) / 4;
        teleopP.setPreferredSize(new Dimension(mapDim.width / 2, height));
        videoP.setPreferredSize(new Dimension(mapDim.width / 2, height));
        videoP.setVisible(true);
        teleopP.setVisible(true);
        cancelButton.setText("Collapse");
        wwPanel.revalidate();
    }

    public void setControlMode(ControlMode controlMode) {
        clearPath();
        teleopP.stopTeleop();
        this.controlMode = controlMode;
        switch (controlMode) {
            case TELEOP:
                cancelAssignedWaypoints();
                showExpandables();
                break;
            case POINT:
                hideExpandables();
                break;
            case PATH:
                hideExpandables();
                break;
            case NONE:
                hideExpandables();
                break;
        }
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
            teleopP.stopTeleop();
        }
        BoatProxy boatProxy = null;
        boolean enabled = false;
        if (boatMarker != null) {
            boatProxy = markerToProxy.get(boatMarker);
            enabled = true;
            // Update teleop panel's proxy 
            teleopP.setVehicle(boatProxy.getVehicleServer());
        } else {
            // Remove teleop panel's proxy and hide teleop panel
            teleopP.setVehicle(null);
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

//    public static void computeUiComponent() {
//        // Markups
//        supportedMarkups.add(Attention.AttentionTarget.ALL_PROXIES);
//        supportedMarkups.add(Attention.AttentionTarget.RELEVANT_PROXIES);
//        supportedMarkups.add(Attention.AttentionTarget.SELECTED_PROXIES);
//        // blink?
//        
//        supportedMarkups.add(RelevantProxy.Proxies.ALL_PROXIES);
//        supportedMarkups.add(RelevantProxy.Proxies.RELEVANT_PROXIES);
//        supportedMarkups.add(RelevantProxy.Proxies.SELECTED_PROXIES);
//    }
}
