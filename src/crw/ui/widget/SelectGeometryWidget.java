package crw.ui.widget;

import crw.ui.worldwind.WorldWindWidgetInt;
import crw.Conversion;
import crw.CrwHelper;
import crw.proxy.BoatProxy;
import crw.ui.component.WorldWindPanel;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
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
import java.util.Hashtable;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import sami.area.Area2D;
import sami.markup.Markup;
import sami.path.Location;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author nbb
 */
public class SelectGeometryWidget implements MarkupComponentWidget, WorldWindWidgetInt {

    public enum SelectMode {

        POINT, PATH, AREA, NONE, CLEAR
    };
    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private boolean visible = true;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Position> selectedPositions = new ArrayList<Position>();
    private JButton pointButton, pathButton, areaButton, cancelButton;
    private List<SelectMode> enabledModes;
    private MarkerLayer markerLayer;
    private SelectMode selectMode = SelectMode.NONE;
    private RenderableLayer renderableLayer;
    private WorldWindPanel wwPanel;
    private Polyline polyline = null;
//    private Path path = null;
    private SurfacePolygon area = null;

    public SelectGeometryWidget() {
        populateLists();
    }

    public SelectGeometryWidget(WorldWindPanel wwPanel) {
        this(wwPanel, Arrays.asList(SelectMode.POINT, SelectMode.PATH, SelectMode.AREA, SelectMode.NONE, SelectMode.CLEAR), SelectMode.NONE);
    }

    public SelectGeometryWidget(WorldWindPanel wwPanel, List<SelectMode> enabledModes, SelectMode defaultMode) {
        if (enabledModes == null) {
            enabledModes = new ArrayList<SelectMode>();
        }
        if (defaultMode == null) {
            defaultMode = SelectMode.NONE;
        }
        this.wwPanel = wwPanel;
        this.enabledModes = enabledModes;
        initRenderableLayer();
        initButtons(enabledModes);
        setSelectMode(defaultMode);
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
        throw new UnsupportedOperationException("Not supported yet.");
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
        Position clickPositionAsl = CrwHelper.getPositionAsl(wwd.getView().getGlobe(), wwd.getCurrentPosition());
        // Continue creating a new area?
        switch (selectMode) {
            case POINT:
                if (clickPositionAsl != null) {
                    BasicMarkerAttributes attributes = new BasicMarkerAttributes();
                    attributes.setShapeType(BasicMarkerShape.SPHERE);
                    attributes.setMinMarkerSize(50);
                    attributes.setMaterial(Material.YELLOW);
                    attributes.setOpacity(1);
                    BasicMarker circle = new BasicMarker(clickPositionAsl, attributes);
                    markers.add(circle);
                    setSelectMode(SelectMode.NONE);

//                    SurfaceCircle circle = new SurfaceCircle(new LatLon(clickPosition.getLatitude(), clickPosition.getLongitude()), 25);
//                    ShapeAttributes attributes = new BasicShapeAttributes();
//                    attributes.setInteriorMaterial(Material.YELLOW);
//                    attributes.setInteriorOpacity(0.75);
//                    attributes.setOutlineMaterial(Material.YELLOW);
//                    attributes.setOutlineOpacity(0.75);
//                    attributes.setOutlineWidth(10);
//                    circle.setAttributes(attributes);
//                    renderableLayer.addRenderable(circle);
//                    setSelectMode(SelectMode.NONE);
                    return true;
                }
                break;
            case PATH:
                if (clickPositionAsl != null) {
                    selectedPositions.add(clickPositionAsl);
                    // Update temporary path
                    if (polyline != null) {
                        renderableLayer.removeRenderable(polyline);
                    }
                    polyline = new Polyline(selectedPositions);
                    polyline.setColor(Color.yellow);
                    polyline.setLineWidth(8);
                    renderableLayer.addRenderable(polyline);
//                    path = new Path(selectedPositions);
//                    ShapeAttributes attributes = new BasicShapeAttributes();
//                    attributes.setOutlineWidth(8);
//                    attributes.setOutlineMaterial(Material.YELLOW);
//                    attributes.setDrawOutline(true);
//                    path.setAttributes(attributes);
//                    path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
//                    renderableLayer.addRenderable(path);
                    wwd.redraw();
                    if (evt.getClickCount() > 1) {
                        // Finish path
                        polyline = null;
//                        path = null;
                        setSelectMode(SelectMode.NONE);
                    }
                    return true;
                }
                break;
            case AREA:
                if (clickPositionAsl != null) {
                    selectedPositions.add(clickPositionAsl);
                    // Update temporary area
                    if (area != null) {
                        renderableLayer.removeRenderable(area);
                    }
                    area = new SurfacePolygon(selectedPositions);
                    ShapeAttributes attributes = new BasicShapeAttributes();
                    attributes.setInteriorOpacity(0.5);
                    attributes.setInteriorMaterial(Material.YELLOW);
                    attributes.setOutlineWidth(2);
                    attributes.setOutlineMaterial(Material.BLACK);
                    area.setAttributes(attributes);
                    renderableLayer.addRenderable(area);
                    wwd.redraw();
                    if (evt.getClickCount() > 1) {
                        // Finish area
                        area = null;
                        setSelectMode(SelectMode.NONE);
                    }
                    return true;
                }
                break;
            case NONE:
//                for (Marker marker : markers) {
//                    Point clickPoint = evt.getPoint();
//                    Position tlPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x - marker.getAttributes().getMarkerPixels(), clickPoint.y - marker.getAttributes().getMarkerPixels());
//                    Position brPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x + marker.getAttributes().getMarkerPixels(), clickPoint.y + marker.getAttributes().getMarkerPixels());
//                    Position markerPos = marker.getPosition();
//                    if (CrwHelper.positionBetween(markerPos, tlPos, brPos)) {
//                        return true;
//                    }
//                }
                return false;
        }

        return false;
    }

    public void setSelectMode(SelectMode selectMode) {
        this.selectMode = selectMode;
        switch (selectMode) {
            case POINT:
                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(true);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                break;

            case PATH:
                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(true);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                break;
            case AREA:
                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(true);
                }
                break;
            case NONE:
            default:
                boolean redraw = false;
                if (polyline != null) {
                    renderableLayer.removeRenderable(polyline);
                    polyline = null;
                    redraw = true;
                }
//                if (path != null) {
//                    renderableLayer.removeRenderable(path);
//                    path = null;
//                    redraw = true;
//                }
                if (area != null) {
                    renderableLayer.removeRenderable(area);
                    area = null;
                    redraw = true;
                }
                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                if (redraw) {
                    wwPanel.wwCanvas.redraw();
                }
                break;
        }
        selectedPositions = new ArrayList<Position>();
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

    protected void initRenderableLayer() {
        if (wwPanel == null) {
            return;
        }

        markerLayer = new MarkerLayer();
        markerLayer.setOverrideMarkerElevation(true);
//        markerLayer.setElevation(10d);
        markerLayer.setKeepSeparated(false);
        markerLayer.setPickEnabled(false);
        markerLayer.setMarkers(markers);
        wwPanel.wwCanvas.getModel().getLayers().add(markerLayer);
        renderableLayer = new RenderableLayer();
        renderableLayer.setPickEnabled(false);
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
    }

    protected void initButtons(List<SelectMode> enabledModes) {
        if (wwPanel == null) {
            return;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        if (enabledModes.contains(SelectMode.POINT)) {
            pointButton = new JButton("Point");
            buttonPanel.add(pointButton);
            pointButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.POINT);
                }
            });
        }
        if (enabledModes.contains(SelectMode.PATH)) {
            pathButton = new JButton("Path");
            buttonPanel.add(pathButton);
            pathButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.PATH);
                }
            });
        }
        if (enabledModes.contains(SelectMode.AREA)) {
            areaButton = new JButton("Area");
            buttonPanel.add(areaButton);
            areaButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.AREA);
                }
            });
        }
        if (enabledModes.contains(SelectMode.NONE)) {
            cancelButton = new JButton("Cancel");
            buttonPanel.add(cancelButton);
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.NONE);
                }
            });
        }
        if (enabledModes.contains(SelectMode.CLEAR)) {
            cancelButton = new JButton("Clear");
            buttonPanel.add(cancelButton);
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    markers.clear();
                    renderableLayer.removeAllRenderables();
                    setSelectMode(SelectMode.NONE);
                }
            });
        }

        wwPanel.buttonPanels.add(buttonPanel, BorderLayout.SOUTH);
        wwPanel.buttonPanels.revalidate();
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
    }

    public void addRenderable(Renderable renderable) {
        renderableLayer.addRenderable(renderable);
    }

    private void populateLists() {
        // Creation
        supportedCreationClasses.add(Location.class);
        supportedCreationClasses.add(PathUtm.class);
        supportedCreationClasses.add(Area2D.class);
        // Visualization
        supportedSelectionClasses.add(Location.class);
        supportedSelectionClasses.add(PathUtm.class);
        supportedSelectionClasses.add(Area2D.class);
        // Markups
        //
    }

    @Override
    public int getCreationWidgetScore(Type type, ArrayList<Markup> markups) {
        int score = MarkupComponentHelper.getCreationWidgetScore(supportedCreationClasses, supportedMarkups, type, markups);
//        System.out.println("### Geom widget creation score for " + creationClass.getSimpleName() + ": " + score);
        return score;
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
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                widget = handleCreationHashtable(component, pt);
            }
        } else if (type instanceof Class) {
            Class creationClass = (Class) type;
            if (creationClass.equals(Location.class)) {
                List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.POINT, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
                widget = new SelectGeometryWidget((WorldWindPanel) component, modes, SelectGeometryWidget.SelectMode.POINT);
            } else if (creationClass.equals(PathUtm.class)) {
                List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.PATH, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
                widget = new SelectGeometryWidget((WorldWindPanel) component, modes, SelectGeometryWidget.SelectMode.PATH);
            } else if (creationClass.equals(Area2D.class)) {
                List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.AREA, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
                widget = new SelectGeometryWidget((WorldWindPanel) component, modes, SelectGeometryWidget.SelectMode.AREA);
            }
        }
        return widget;
    }

    public MarkupComponentWidget handleCreationHashtable(MarkupComponent component, ParameterizedType pt) {
        MarkupComponentWidget widget = null;

        return widget;
    }

    @Override
    public MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = null;
        if (selectionObject instanceof Hashtable) {
            Hashtable hashtable = (Hashtable) selectionObject;
            widget = handleSelectionHashtable(component, hashtable);
        } else if (selectionObject instanceof Location) {
            Location location = (Location) selectionObject;
            Position position = Conversion.locationToPosition(location);

            SelectGeometryWidget select = new SelectGeometryWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            BasicMarkerAttributes attributes = new BasicMarkerAttributes();
            attributes.setShapeType(BasicMarkerShape.SPHERE);
            attributes.setMinMarkerSize(50);
            attributes.setMaterial(Material.YELLOW);
            attributes.setOpacity(1);
            BasicMarker circle = new BasicMarker(position, attributes);
            select.addMarker(circle);
            widget = select;
        } else if (selectionObject instanceof PathUtm) {
            List<Location> waypoints = ((PathUtm) selectionObject).getPoints();
            List<Position> positions = new ArrayList<Position>();
            // Convert from Locations to LatLons
            for (Location waypoint : waypoints) {
                positions.add(Conversion.locationToPosition(waypoint));
            }
            SelectGeometryWidget select = new SelectGeometryWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            // Add path
            Path path = new Path(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setOutlineWidth(8);
            attributes.setOutlineMaterial(Material.YELLOW);
            attributes.setDrawOutline(true);
            path.setAttributes(attributes);
            path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            select.addRenderable(path);
            widget = select;
        } else if (selectionObject instanceof Area2D) {
            List<Location> locations = ((Area2D) selectionObject).getPoints();
            List<Position> positions = new ArrayList<Position>();
            // Convert from Locations to LatLons
            for (Location location : locations) {
                positions.add(Conversion.locationToPosition(location));
            }
            SelectGeometryWidget select = new SelectGeometryWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            // Add surface polygon of area
            SurfacePolygon polygon = new SurfacePolygon(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setInteriorOpacity(0.5);
            attributes.setInteriorMaterial(Material.YELLOW);
            attributes.setOutlineWidth(2);
            attributes.setOutlineMaterial(Material.BLACK);
            polygon.setAttributes(attributes);
            select.addRenderable(polygon);
            widget = select;
        }
        return widget;
    }

    public MarkupComponentWidget handleSelectionHashtable(MarkupComponent component, Hashtable hashtable) {
        MarkupComponentWidget widget = null;
        Object keyObject = null;
        Object valueObject = null;
        if (hashtable.size() > 0) {
            for (Object key : hashtable.keySet()) {
                if (key != null && hashtable.get(key) != null) {
                    keyObject = key;
                    valueObject = hashtable.get(key);
                    break;
                }
            }
        }
        if (keyObject == null || valueObject == null) {
            return null;
        }

        if (keyObject instanceof ProxyInt && valueObject instanceof PathUtm) {
            Hashtable<ProxyInt, PathUtm> proxyPaths = (Hashtable<ProxyInt, PathUtm>) hashtable;
            SelectGeometryWidget select = new SelectGeometryWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            // Add paths
            for (ProxyInt proxy : proxyPaths.keySet()) {
                // Convert to locations to positions
                List<Location> waypoints = ((PathUtm) proxyPaths.get(proxy)).getPoints();
                List<Position> positions = new ArrayList<Position>();
                // Convert from Locations to LatLons
                for (Location waypoint : waypoints) {
                    positions.add(Conversion.locationToPosition(waypoint));
                }
                // Create path renderable
                Path path = new Path(positions);
                ShapeAttributes attributes = new BasicShapeAttributes();
                attributes.setOutlineWidth(8);
                if (proxy instanceof BoatProxy) {
                    attributes.setOutlineMaterial(new Material(((BoatProxy) proxy).getColor()));
                } else {
                    attributes.setOutlineMaterial(Material.YELLOW);
                }
                attributes.setDrawOutline(true);
                path.setAttributes(attributes);
                path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                select.addRenderable(path);
            }
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
}
