package crw.ui.component;

import sami.Conversion;
import crw.proxy.BoatProxy;
import sami.uilanguage.MarkupComponent;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.PathWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.SelectGeometryWidget;
import crw.ui.widget.SelectGeometryWidget.SelectMode;
import crw.ui.widget.SensorDataWidget;
import crw.ui.worldwind.WorldWindWidgetInt;
import gov.nasa.worldwind.awt.MouseInputActionHandler;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import java.awt.BorderLayout;
import java.util.Vector;
import javax.swing.JPanel;
import crw.ui.worldwind.WorldWindInputAdapter;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import sami.area.Area2D;
import sami.engine.Engine;
import sami.map.ViewArea;
import sami.engine.Mediator;
import sami.engine.PlanManager;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
import sami.map.ViewPoint;
import sami.markup.Markup;
import sami.markup.RelevantArea;
import sami.mission.MissionPlanSpecification;
import sami.path.Location;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author pscerri
 */
public class WorldWindPanel implements MarkupComponent, EnvironmentListenerInt {

    // MarkupComponent variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses = new Hashtable<Class, ArrayList<Class>>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses = new Hashtable<Class, ArrayList<Class>>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    public final ArrayList<Class> widgetClasses = new ArrayList<Class>();
    public JComponent component = null;
    //
    private final static Logger LOGGER = Logger.getLogger(WorldWindPanel.class.getName());
    public static final double SPHERE_SIZE = 1;
    public WorldWindowGLCanvas wwCanvas = null;
    public JPanel buttonPanels;
    protected WorldWindInputAdapter mouseHandler;
    protected Vector<WorldWindWidgetInt> widgetList;
    protected MouseInputActionHandler handler;
    protected BorderLayout borderLayout;
    // Multiplier for setting view area based on proxy list so WorldWind renderables are drawn and fully visible
    //  Roughly works, although an inverted exponential function would be more appropriate
    private static final double PROXY_VIEW_MULTIPLIER = 2.0;

    // Useful GPS locations
//    // Doha Corniche
//    Configuration.setValue(AVKey.INITIAL_LATITUDE, 25.29636);
//    Configuration.setValue(AVKey.INITIAL_LONGITUDE, 51.52699);
//    Configuration.setValue(AVKey.INITIAL_ALTITUDE, 5000.0);
//    // Katara Beach
//    Configuration.setValue(AVKey.INITIAL_LATITUDE, 25.354484741711);
//    Configuration.setValue(AVKey.INITIAL_LONGITUDE, 51.5283418997116);
//    Configuration.setValue(AVKey.INITIAL_ALTITUDE, 5000.0);
//    // Pittsburgh
//    Configuration.setValue(AVKey.INITIAL_LATITUDE, 40.44515205369163);
//    Configuration.setValue(AVKey.INITIAL_LONGITUDE, -80.01877404355538);
//    Configuration.setValue(AVKey.INITIAL_ALTITUDE, 30000.0);
    public WorldWindPanel() {
        populateLists();
    }

    public void createMap(ArrayList<Markup> markups) {
        ArrayList<String> layerNames = new ArrayList<String>();
        layerNames.add("Blue Marble May 2004");
        layerNames.add("Scale bar");
        layerNames.add("Place Names");
        layerNames.add("Bing Imagery");
        for (final Markup markup : markups) {
            if (markup instanceof RelevantArea) {
                RelevantArea relevantArea = (RelevantArea) markup;
                if (relevantArea.mapType == RelevantArea.MapType.POLITICAL) {
                    // Remove satellite layer, add street layer
                    layerNames.add("Open Street Map");
                    layerNames.remove("Bing Imagery");
                } else if (relevantArea.mapType == RelevantArea.MapType.SATELLITE) {
                    // Remove street  layer, add satellite layer
                    layerNames.add("Bing Imagery");
                    layerNames.remove("Open Street Map");
                }
            }
        }
        createMap(400, 300, null);
    }

    public void createMap() {
        createMap(400, 300, null);
    }

    public void createMap(int width, int height, ArrayList<String> layerNames) {
        widgetList = new Vector<WorldWindWidgetInt>();
        // Use flat Earth
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());
        // Change mouse handler
        Configuration.setValue(AVKey.VIEW_INPUT_HANDLER_CLASS_NAME, WorldWindInputAdapter.class.getName());
        // Set this when offline
        Configuration.setValue(AVKey.OFFLINE_MODE, "false");

        wwCanvas = new WorldWindowGLCanvas();
        wwCanvas.setPreferredSize(new java.awt.Dimension(width, height));
        wwCanvas.setModel(new BasicModel());

        // Virtual Earth
        if (layerNames == null) {
            layerNames = new ArrayList<String>();
            layerNames.add("Bing Imagery");
            layerNames.add("Blue Marble May 2004");
            layerNames.add("Scale bar");
            layerNames.add("Place Names");
        }
        for (Layer layer : wwCanvas.getModel().getLayers()) {
            if (layerNames.contains(layer.getName())) {
                layer.setEnabled(true);
            } else {
                layer.setEnabled(false);
            }
        }

        mouseHandler = (WorldWindInputAdapter) wwCanvas.getView().getViewInputHandler();
        mouseHandler.setWidgetList(widgetList);
        borderLayout = new BorderLayout(0, 0);
        component = new JPanel();
        component.setLayout(borderLayout);
        // World Wind Canvas
        component.add(wwCanvas, BorderLayout.CENTER);
        // Button panel
        buttonPanels = new JPanel();
        buttonPanels.setLayout(new BoxLayout(buttonPanels, BoxLayout.Y_AXIS));
        buttonPanels.revalidate();
        component.add(buttonPanels, BorderLayout.SOUTH);

        component.setMinimumSize(new Dimension(0, 0));
        component.setMaximumSize(new java.awt.Dimension(width, height));
        component.setPreferredSize(new java.awt.Dimension(width, height));

        Mediator.getInstance().addEnvironmentListener(this);
        environmentUpdated();
    }

    public BorderLayout getLayout() {
        return borderLayout;
    }

    public JPanel getControl() {
        return null;
    }

    public WorldWindowGLCanvas getCanvas() {
        return wwCanvas;
    }

    public void revalidate() {
        component.revalidate();
    }

    /**
     * Adds a new widget to be displayed in the frame.
     *
     * @param w the widget to be added.
     */
    public void addWidget(WorldWindWidgetInt w) {
        if (w == null) {
            LOGGER.severe("Tried to add NULL WorlWindWidgetInt");
            return;
        }
        widgetList.add(w);
    }

    public boolean hasWidget(Class widgetClassname) {
        for (WorldWindWidgetInt widget : widgetList) {
            if (widget.getClass().equals(widgetClassname)) {
                return true;
            }
        }
        return false;
    }

    public WorldWindWidgetInt getWidget(Class widgetClassname) {
        for (WorldWindWidgetInt widget : widgetList) {
            if (widget.getClass().equals(widgetClassname)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * Removes a widget that is displayed in the frame.
     *
     * @param w the widget to be removed.
     */
    public void removeWidget(WorldWindWidgetInt w) {
        widgetList.remove(w);
    }

    private void populateLists() {
        // Widgets
        widgetClasses.add(RobotTrackWidget.class);
        widgetClasses.add(PathWidget.class);
        widgetClasses.add(RobotWidget.class);
        widgetClasses.add(SelectGeometryWidget.class);
        widgetClasses.add(SensorDataWidget.class);
//        widgetClasses.add(SensorDataQuadtreeWidget.class);
        // Creation
        supportedCreationClasses.add(ViewArea.class);
        supportedCreationClasses.add(ViewPoint.class);
        // Visualization
        supportedCreationClasses.add(ViewArea.class);
        supportedCreationClasses.add(ViewPoint.class);
        // Markups
        supportedMarkups.add(RelevantArea.AreaSelection.AREA);
        supportedMarkups.add(RelevantArea.AreaSelection.POINT);
        supportedMarkups.add(RelevantArea.AreaSelection.ALL_PROXIES);
        supportedMarkups.add(RelevantArea.AreaSelection.RELEVANT_PROXIES);
        supportedMarkups.add(RelevantArea.MapType.POLITICAL);
        supportedMarkups.add(RelevantArea.MapType.SATELLITE);
    }

    @Override
    public int getCreationComponentScore(Type type, Field field, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationComponentScore(supportedCreationClasses, supportedHashtableCreationClasses, supportedMarkups, widgetClasses, type, field, markups);
    }

    @Override
    public int getSelectionComponentScore(Type type, Object object, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionComponentScore(supportedSelectionClasses, supportedHashtableSelectionClasses, supportedMarkups, widgetClasses, type, object, markups);
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        return MarkupComponentHelper.getMarkupComponentScore(supportedMarkups, widgetClasses, markups);
    }

    @Override
    public MarkupComponent useCreationComponent(Type type, Field field, ArrayList<Markup> markups, MissionPlanSpecification mSpecScope, PlanManager pmScope) {
        if (wwCanvas == null) {
            createMap(markups);
        }
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget widgetInstance = (MarkupComponentWidget) widgetClass.newInstance();
                int widgetCreationScore = widgetInstance.getCreationWidgetScore(type, field, markups);
                int widgetMarkupScore = widgetInstance.getMarkupScore(markups);
                if (widgetCreationScore >= 0 || widgetMarkupScore > 0) {
                    MarkupComponentWidget widget = ((MarkupComponentWidget) widgetInstance).addCreationWidget(this, type, markups);
                    addWidget((WorldWindWidgetInt) widget);
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    @Override
    public MarkupComponent useSelectionComponent(Object object, ArrayList<Markup> markups, MissionPlanSpecification mSpecScope, PlanManager pmScope) {
        if (wwCanvas == null) {
            createMap(markups);
        }
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget widgetInstance = (MarkupComponentWidget) widgetClass.newInstance();
                int widgetSelectionScore = widgetInstance.getSelectionWidgetScore(object.getClass(), object, markups);
                int widgetMarkupScore = widgetInstance.getMarkupScore(markups);
                if (widgetSelectionScore >= 0 || widgetMarkupScore > 0) {
                    MarkupComponentWidget widget = ((MarkupComponentWidget) widgetInstance).addSelectionWidget(this, object, markups);
                    addWidget((WorldWindWidgetInt) widget);
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public Object getComponentValue(Class componentClass) {
        Object value = null;
        if (componentClass.equals(Location.class)) {
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof MarkerLayer) {
                    MarkerLayer layer = (MarkerLayer) tempLayer;
                    Marker position = null;
                    for (Marker marker : layer.getMarkers()) {
                        if (marker instanceof Marker) {
                            position = (Marker) marker;
                        }
                    }
                    if (position != null) {
                        value = Conversion.positionToLocation(position.getPosition());
                    }
                }
            }
        } else if (componentClass.equals(PathUtm.class)) {
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof RenderableLayer) {
                    RenderableLayer layer = (RenderableLayer) tempLayer;

                    // Polyline method
                    Polyline polyline = null;
                    Path path = null;
                    for (Renderable renderable : layer.getRenderables()) {
                        if (renderable instanceof Polyline) {
                            polyline = (Polyline) renderable;
                        } else if (renderable instanceof Path) {
                            path = (Path) renderable;
                        }
                    }
                    if (polyline != null) {
                        ArrayList<Location> locationList = new ArrayList<Location>();
                        for (Position position : polyline.getPositions()) {
                            locationList.add(Conversion.positionToLocation(position));
                        }
                        value = new PathUtm(locationList);
                    } else if (path != null) {
                        ArrayList<Location> locationList = new ArrayList<Location>();
                        for (Position position : path.getPositions()) {
                            locationList.add(Conversion.positionToLocation(position));
                        }
                        value = new PathUtm(locationList);
                    }
                }
            }
        } else if (componentClass.equals(Area2D.class)) {
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof RenderableLayer) {
                    RenderableLayer layer = (RenderableLayer) tempLayer;
                    SurfacePolygon area = null;
                    for (Renderable renderable : layer.getRenderables()) {
                        if (renderable instanceof SurfacePolygon) {
                            area = (SurfacePolygon) renderable;
                        }
                    }
                    if (area != null) {
                        ArrayList<Location> locationList = new ArrayList<Location>();
                        for (LatLon latLon : area.getLocations()) {
                            locationList.add(Conversion.latLonToLocation(latLon));
                        }
                        value = new Area2D(locationList);
                    }
                }
            }
        } else if (componentClass.equals(ViewArea.class)) {
            Sector sector = getCurrentSector();
            LatLon[] corners = sector.getCorners();
            ArrayList<Location> locations = new ArrayList<Location>();
            for (LatLon corner : corners) {
                locations.add(Conversion.latLonToLocation(corner));
            }
            value = new Area2D(locations);
        } else if (componentClass.equals(ViewPoint.class)) {
            value = (ViewPoint) Conversion.positionToLocation(wwCanvas.getView().getCurrentEyePosition());
        }
        return value;
    }

    @Override
    public boolean setComponentValue(Object value) {
        if (value == null) {
            LOGGER.severe("Tried to set component value to NULL");
            return false;
        }
        boolean success = false;
        if (value.getClass().equals(Location.class)) {
            // Clear out any already existing BasicMarkers
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof RenderableLayer) {
                    RenderableLayer layer = (RenderableLayer) tempLayer;
                    ArrayList<Renderable> toRemove = new ArrayList<Renderable>();
                    Iterator<Renderable> iterator = layer.getRenderables().iterator();
                    while (iterator.hasNext()) {
                        Renderable renderable = iterator.next();
                        if (renderable instanceof BasicMarker) {
//                            // WorldWind iterator causes UnsupportedOperationException here..
//                            iterator.remove();
                            toRemove.add(renderable);
                        }
                    }
                    for (Renderable renderable : toRemove) {
                        layer.removeRenderable(renderable);
                    }
                }
            }

            // Grab or create the geometry widget
            SelectGeometryWidget selectWidget;
            if (hasWidget(SelectGeometryWidget.class)) {
                selectWidget = (SelectGeometryWidget) getWidget(SelectGeometryWidget.class);
            } else {
                selectWidget = new SelectGeometryWidget(this, new ArrayList<SelectMode>(), SelectMode.NONE);
                addWidget(selectWidget);
            }
            // Add the marker of position
            Location location = (Location) value;
            Position position = Conversion.locationToPosition(location);
            BasicMarkerAttributes attributes = new BasicMarkerAttributes();
            attributes.setShapeType(BasicMarkerShape.SPHERE);
            attributes.setMinMarkerSize(SPHERE_SIZE);
            attributes.setMaterial(Material.YELLOW);
            attributes.setOpacity(1);
            BasicMarker circle = new BasicMarker(position, attributes);
            selectWidget.addMarker(circle);
            success = true;
        } else if (value.getClass().equals(PathUtm.class)) {
            // Clear out any already existing Poylines and Paths
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof RenderableLayer) {
                    RenderableLayer layer = (RenderableLayer) tempLayer;
                    ArrayList<Renderable> toRemove = new ArrayList<Renderable>();
                    Iterator<Renderable> iterator = layer.getRenderables().iterator();
                    while (iterator.hasNext()) {
                        Renderable renderable = iterator.next();
                        if (renderable instanceof Polyline || renderable instanceof Path) {
//                            // WorldWind iterator causes UnsupportedOperationException here..
//                            iterator.remove();
                            toRemove.add(renderable);
                        }
                    }
                    for (Renderable renderable : toRemove) {
                        layer.removeRenderable(renderable);
                    }
                }
            }

            // Grab or create the geometry widget
            SelectGeometryWidget selectWidget;
            if (hasWidget(SelectGeometryWidget.class)) {
                selectWidget = (SelectGeometryWidget) getWidget(SelectGeometryWidget.class);
            } else {
                selectWidget = new SelectGeometryWidget(this, new ArrayList<SelectMode>(), SelectMode.NONE);
                addWidget(selectWidget);
            }
            // Add surface polygon of area
            List<Location> waypoints = ((PathUtm) value).getPoints();
            List<Position> positions = new ArrayList<Position>();
            for (Location waypoint : waypoints) {
                positions.add(Conversion.locationToPosition(waypoint));
            }
//            Path path = new Path(positions);
//            ShapeAttributes attributes = new BasicShapeAttributes();
//            attributes.setOutlineWidth(8);
//            attributes.setOutlineMaterial(Material.YELLOW);
//            attributes.setDrawOutline(true);
//            path.setAttributes(attributes);
//            path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
//            selectWidget.addRenderable(path);
            Polyline polyline = new Polyline(positions);
            polyline.setColor(Color.yellow);
            polyline.setLineWidth(8);
            polyline.setFollowTerrain(true);
            selectWidget.addRenderable(polyline);
            success = true;
        } else if (value.getClass().equals(Area2D.class)) {
            // Clear out any already existing SurfacePolygons
            for (int i = 0; i < wwCanvas.getModel().getLayers().size(); i++) {
                Layer tempLayer = wwCanvas.getModel().getLayers().get(i);
                if (tempLayer instanceof RenderableLayer) {
                    RenderableLayer layer = (RenderableLayer) tempLayer;
                    ArrayList<Renderable> toRemove = new ArrayList<Renderable>();
                    Iterator<Renderable> iterator = layer.getRenderables().iterator();
                    while (iterator.hasNext()) {
                        Renderable renderable = iterator.next();
                        if (renderable instanceof SurfacePolygon) {
//                            // WorldWind iterator causes UnsupportedOperationException here..
//                            iterator.remove();
                            toRemove.add(renderable);
                        }
                    }
                    for (Renderable renderable : toRemove) {
                        layer.removeRenderable(renderable);
                    }
                }
            }

            // Grab or create the geometry widget
            SelectGeometryWidget selectWidget;
            if (hasWidget(SelectGeometryWidget.class)) {
                selectWidget = (SelectGeometryWidget) getWidget(SelectGeometryWidget.class);
            } else {
                selectWidget = new SelectGeometryWidget(this, new ArrayList<SelectMode>(), SelectMode.NONE);
                addWidget(selectWidget);
            }
            // Add surface polygon of area
            List<Location> locations = ((Area2D) value).getPoints();
            List<Position> positions = new ArrayList<Position>();
            for (Location location : locations) {
                positions.add(Conversion.locationToPosition(location));
            }
            SurfacePolygon polygon = new SurfacePolygon(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setInteriorOpacity(0.5);
            attributes.setInteriorMaterial(Material.YELLOW);
            attributes.setOutlineWidth(2);
            attributes.setOutlineMaterial(Material.BLACK);
            polygon.setAttributes(attributes);
            selectWidget.addRenderable(polygon);
            success = true;
        } else if (value.getClass() == ViewArea.class) {
            Area2D area = (Area2D) value;
            List<LatLon> points = new ArrayList<LatLon>();
            for (Location location : area.getPoints()) {
                points.add(Conversion.locationToPosition(location));
            }
            setView(points);
        } else if (value.getClass() == ViewPoint.class) {
            ViewPoint viewPoint = (ViewPoint) value;
            wwCanvas.getView().setEyePosition(Conversion.locationToPosition(viewPoint));
        } else if (value.getClass() == Hashtable.class) {
            Hashtable hashtable = (Hashtable) value;
            if (!hashtable.isEmpty()) {
                Class keyClass = null, valueClass = null;
                for (Object key : hashtable.keySet()) {
                    keyClass = key.getClass();
                    valueClass = hashtable.get(key).getClass();
                    break;
                }

                if (ProxyInt.class.isAssignableFrom(keyClass) && PathUtm.class.isAssignableFrom(valueClass)) {
                    // Grab or create the geometry widget
                    PathWidget pathWidget;
                    if (hasWidget(PathWidget.class)) {
                        pathWidget = (PathWidget) getWidget(PathWidget.class);
                    } else {
                        pathWidget = new PathWidget(this);
                        addWidget(pathWidget);
                    }
                    pathWidget.addRenderable(hashtable);

                    success = true;
                }
            }
        }
        return success;
    }

    @Override
    public void handleMarkups(ArrayList<Markup> markups, MarkupManager manager) {
        // No dynamic markups handled

        // Static markups
        for (final Markup markup : markups) {
            if (markup instanceof RelevantArea) {
                RelevantArea relevantArea = (RelevantArea) markup;
                boolean handled = false;
                List<LatLon> points = new ArrayList<LatLon>();
                switch (relevantArea.areaSelection) {
                    case ALL_PROXIES:
                        switch (relevantArea.viewModification) {
                            case EXPAND:
                                points.addAll(getCurrentCorners());
                                for (ProxyInt proxy : Engine.getInstance().getAllProxies()) {
                                    if (proxy instanceof BoatProxy) {
                                        points.add(((BoatProxy) proxy).getPosition());
                                    } else {
                                        LOGGER.warning("Proxy is not a BoatProxy");
                                    }
                                }
                                setView(points, PROXY_VIEW_MULTIPLIER);
                                handled = true;
                                break;
                            case REDUCE:
                                for (ProxyInt proxy : Engine.getInstance().getAllProxies()) {
                                    if (proxy instanceof BoatProxy) {
                                        points.add(((BoatProxy) proxy).getPosition());
                                    } else {
                                        LOGGER.warning("Proxy is not a BoatProxy");
                                    }
                                }
                                setView(points, PROXY_VIEW_MULTIPLIER);
                                handled = true;
                                break;
                        }
                        break;
                    case AREA:
                        switch (relevantArea.viewModification) {
                            case EXPAND:
                                points.addAll(locationstoLatLons(relevantArea.areaOption.area.getPoints()));
                                points.addAll(getCurrentCorners());
                                setView(points);
                                handled = true;
                                break;
                            case REDUCE:
                                setView(locationstoLatLons(relevantArea.areaOption.area.getPoints()));
                                handled = true;
                                break;
                        }
                        break;
                    case POINT:
                        switch (relevantArea.viewModification) {
                            case EXPAND:
                                points.addAll(getCurrentCorners());
                                if (relevantArea.pointOption.point != null) {
                                    Position point = Conversion.locationToPosition(relevantArea.pointOption.point);
                                    if (point != null) {
                                        points.add(point);
                                    }
                                }
                                setView(points);
                                handled = true;
                                break;
                            case REDUCE:
                                if (relevantArea.pointOption.point != null) {
                                    Position point = Conversion.locationToPosition(relevantArea.pointOption.point);
                                    if (point != null) {
                                        wwCanvas.getView().setEyePosition(point);
                                    }
                                }
                                handled = true;
                                break;
                        }
                        break;
                    case RELEVANT_PROXIES:
                        switch (relevantArea.viewModification) {
                            case EXPAND:
                                points.addAll(getCurrentCorners());
                                for (ProxyInt proxy : relevantArea.getRelevantProxies()) {
                                    if (proxy instanceof BoatProxy) {
                                        points.add(((BoatProxy) proxy).getPosition());
                                    } else {
                                        LOGGER.warning("Proxy is not a BoatProxy");
                                    }
                                }
                                setView(points, PROXY_VIEW_MULTIPLIER);
                                handled = true;
                                break;
                            case REDUCE:
                                for (ProxyInt proxy : relevantArea.getRelevantProxies()) {
                                    if (proxy instanceof BoatProxy) {
                                        points.add(((BoatProxy) proxy).getPosition());
                                    } else {
                                        LOGGER.warning("Proxy is not a BoatProxy");
                                    }
                                }
                                setView(points, PROXY_VIEW_MULTIPLIER);
                                handled = true;
                                break;
                        }
                        break;

                }
            }
        }
    }

    @Override
    public void disableMarkup(Markup markup) {
        // No dynamic markups handled
    }

    @Override
    public void environmentUpdated() {
        if (Mediator.getInstance().getEnvironment() != null && Mediator.getInstance().getEnvironment().getDefaultLocation() != null) {
            Position defaultPosition = Conversion.locationToPosition(Mediator.getInstance().getEnvironment().getDefaultLocation());
            Configuration.setValue(AVKey.INITIAL_LATITUDE, defaultPosition.getLatitude());
            Configuration.setValue(AVKey.INITIAL_LONGITUDE, defaultPosition.getLongitude());
            Configuration.setValue(AVKey.INITIAL_ALTITUDE, defaultPosition.getAltitude());
            getCanvas().getView().setEyePosition(defaultPosition);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        // Make this flexible
        frame.getContentPane().setLayout(new FlowLayout());

        WorldWindPanel www = new WorldWindPanel();
        www.createMap();
        frame.getContentPane().add(www.wwCanvas);

        for (Layer l : www.getCanvas().getModel().getLayers()) {
            System.out.println("Layer: " + l);
        }

        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        ArrayList<Class> compCreationClasses = new ArrayList<Class>();
        compCreationClasses.addAll(supportedCreationClasses);
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget temp = (MarkupComponentWidget) widgetClass.newInstance();
                ArrayList<Class> widgetCreationClasses = temp.getSupportedCreationClasses();
                for (Class widgetCreationClass : widgetCreationClasses) {
                    if (!compCreationClasses.contains(widgetCreationClass)) {
                        compCreationClasses.add(widgetCreationClass);
                    }
                }
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return compCreationClasses;
    }

    /**
     * Set canvas view to capture a set of lat/lon points
     *
     * @param points The points the view should capture
     * @param altitudeMultiplier How much to multiply the altitude by to pad the
     * view
     */
    private void setView(List<LatLon> points, double altitudeMultiplier) {
        System.out.println("setView: " + points.toString());
        Sector sector;
        if (points.isEmpty()) {
//            // Show entire globe if no points were provided
//            sector = Sector.FULL_SPHERE;
            // Do nothing if no points were provided
            LOGGER.warning("setView called with empty list of points");
            return;
        } else {
            sector = Sector.boundingSector(points);
        }
        Angle horizontalFov = getCanvas().getView().getFieldOfView();
        Angle verticalFov = ViewUtil.computeVerticalFieldOfView(getCanvas().getView().getFieldOfView(), getCanvas().getView().getViewport());

        Angle delta;
        Angle fov;
        if (verticalFov.compareTo(horizontalFov) > 0) {
            delta = sector.getDeltaLon();
            fov = horizontalFov;
        } else {
            delta = sector.getDeltaLat();
            fov = verticalFov;
        }

        double altitude = 0.0;
        if (points.size() == 1) {
            // Use default view's altitude if one exists, otherwise use 0
            EnvironmentProperties envProp = Mediator.getInstance().getEnvironment();
            if (envProp != null && envProp.getDefaultLocation() != null) {
                altitude = envProp.getDefaultLocation().getAltitude();
                System.out.println("Using default altitude of " + altitude);
            } else {
                System.out.println("Using unspecified altitude of 0.0");
            }
        } else {
            // Calculate altitude to show all the points
            double arcLength = delta.radians * Earth.WGS84_EQUATORIAL_RADIUS;
            double fieldOfView = fov.radians;
            altitude = arcLength / (2 * Math.tan(fieldOfView / 2.0));
            // Pad the altitude a bit so WorldWind renderables are drawn and fully visible
            altitude *= altitudeMultiplier;
            System.out.println("Using calculated altitude of " + altitude);
        }

        Position sectorPosition = new Position(sector.getCentroid(), altitude);
        getCanvas().getView().setEyePosition(sectorPosition);
        getCanvas().redrawNow();
    }

    private void setView(List<LatLon> points) {
        setView(points, 0);
    }

    private List<LatLon> locationstoLatLons(List<Location> locations) {
        ArrayList<LatLon> points = new ArrayList<LatLon>();
        for (Location location : locations) {
            if (location != null) {
                LatLon point = Conversion.locationToPosition(location);
                if (point != null) {
                    points.add(point);
                }
            }
        }
        return points;
    }

    /**
     * Gets a LatLon list of the current view's 4 corners
     *
     * @param position
     * @return
     */
    private ArrayList<LatLon> getCurrentCorners() {
        ArrayList<LatLon> points = new ArrayList<LatLon>();

        Rectangle viewport = getCanvas().getView().getViewport();
        Position p;
        p = getCanvas().getView().computePositionFromScreenPoint(viewport.getMinX(), viewport.getMinY());
        // Don't add position if it is NULL (not on globe)
        if (p != null) {
            points.add(p);
        }
        p = getCanvas().getView().computePositionFromScreenPoint(viewport.getMinX(), viewport.getMaxY());
        if (p != null) {
            points.add(p);
        }
        p = getCanvas().getView().computePositionFromScreenPoint(viewport.getMaxX(), viewport.getMaxY());
        if (p != null) {
            points.add(p);
        }
        p = getCanvas().getView().computePositionFromScreenPoint(viewport.getMaxX(), viewport.getMinY());
        if (p != null) {
            points.add(p);
        }

        return points;
    }

    private Sector getCurrentSector() {
        ArrayList<LatLon> points = getCurrentCorners();
        Sector sector;
        if (points.isEmpty()) {
            // Show entire globe if no points were provided
            sector = Sector.FULL_SPHERE;
        } else {
            sector = Sector.boundingSector(points);
        }
        return sector;
    }
}
