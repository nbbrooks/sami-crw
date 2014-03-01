package crw.ui.component;

import crw.Conversion;
import sami.uilanguage.MarkupComponent;
import crw.ui.widget.RobotTrackWidget;
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
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import sami.area.Area2D;
import sami.markup.Markup;
import sami.markup.RelevantArea;
import sami.path.Location;
import sami.path.PathUtm;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author pscerri
 */
public class WorldWindPanel implements MarkupComponent {

    // MarkupComponent variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    public final ArrayList<Class> widgetClasses = new ArrayList<Class>();
    public JComponent component = null;
    //
    private final static Logger LOGGER = Logger.getLogger(WorldWindPanel.class.getName());
    public WorldWindowGLCanvas wwCanvas = null;
    public JPanel buttonPanels;
    protected WorldWindInputAdapter mouseHandler;
    protected Vector<WorldWindWidgetInt> widgetList;
    protected MouseInputActionHandler handler;
    protected BorderLayout borderLayout;

    public WorldWindPanel() {
        populateLists();
    }

    public void createMap() {
        createMap(400, 300, Double.NaN, Double.NaN, Double.NaN);
    }

    public void createMap(int width, int height) {
        createMap(width, height, Double.NaN, Double.NaN, Double.NaN);
    }

    public void createMap(int width, int height, double lat, double lon, double alt) {
        widgetList = new Vector<WorldWindWidgetInt>();
        // Use flat Earth
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());
        // Change mouse handler
        Configuration.setValue(AVKey.VIEW_INPUT_HANDLER_CLASS_NAME, WorldWindInputAdapter.class.getName());
        // @todo Make initial position configurable
        // Doha Corniche
        if (Double.isNaN(lat) && Double.isNaN(lon) && Double.isNaN(alt)) {
//            Configuration.setValue(AVKey.INITIAL_LATITUDE, 25.29636);
//            Configuration.setValue(AVKey.INITIAL_LONGITUDE, 51.52699);
//            Configuration.setValue(AVKey.INITIAL_ALTITUDE, 5000.0);
//        // Pittsburgh
            Configuration.setValue(AVKey.INITIAL_LATITUDE, 40.44515205369163);
            Configuration.setValue(AVKey.INITIAL_LONGITUDE, -80.01877404355538);
            Configuration.setValue(AVKey.INITIAL_ALTITUDE, 30000.0);
        } else {
            Configuration.setValue(AVKey.INITIAL_LATITUDE, lat);
            Configuration.setValue(AVKey.INITIAL_LONGITUDE, lon);
            Configuration.setValue(AVKey.INITIAL_ALTITUDE, alt);
        }

        // Set this when offline
        Configuration.setValue(AVKey.OFFLINE_MODE, "false");

        wwCanvas = new WorldWindowGLCanvas();
        wwCanvas.setPreferredSize(new java.awt.Dimension(width, height));
        wwCanvas.setModel(new BasicModel());

        // Virtual Earth
        for (Layer layer : wwCanvas.getModel().getLayers()) {
            if (layer.getName().equals("MS Virtual Earth Aerial")
                    || layer.getName().equals("Blue Marble (WMS) 2004")
                    || layer.getName().equals("Scale bar")) {
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
        widgetClasses.add(RobotWidget.class);
        widgetClasses.add(SelectGeometryWidget.class);
        widgetClasses.add(SensorDataWidget.class);
        widgetClasses.add(SensorDataWidget.class);
        // Creation
        //
        // Visualization
        //
        // Markups
        supportedMarkups.add(RelevantArea.AreaSelection.AREA);
        supportedMarkups.add(RelevantArea.AreaSelection.CENTER_ON_POINT);
        supportedMarkups.add(RelevantArea.AreaSelection.CENTER_ON_ALL_PROXIES);
        supportedMarkups.add(RelevantArea.AreaSelection.CENTER_ON_RELEVANT_PROXIES);
        supportedMarkups.add(RelevantArea.MapType.POLITICAL);
        supportedMarkups.add(RelevantArea.MapType.SATELLITE);
    }

    @Override
    public int getCreationComponentScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationComponentScore(supportedCreationClasses, supportedMarkups, widgetClasses, type, markups);
    }

    @Override
    public int getSelectionComponentScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionComponentScore(supportedSelectionClasses, supportedMarkups, widgetClasses, type, markups);
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        return MarkupComponentHelper.getMarkupComponentScore(supportedMarkups, widgetClasses, markups);
    }

    @Override
    public MarkupComponent useCreationComponent(Type type, ArrayList<Markup> markups) {
        if (wwCanvas == null) {
            createMap();
        }
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget widgetInstance = (MarkupComponentWidget) widgetClass.newInstance();
                int widgetCreationScore = widgetInstance.getCreationWidgetScore(type, markups);
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
    public MarkupComponent useSelectionComponent(Object object, ArrayList<Markup> markups) {
        if (wwCanvas == null) {
            createMap();
        }
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget widgetInstance = (MarkupComponentWidget) widgetClass.newInstance();
                int widgetSelectionScore = widgetInstance.getSelectionWidgetScore(object.getClass(), markups);
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
    public Object getComponentValue(Field field) {
        Object value = null;
        if (field.getType().equals(Location.class)) {
            MarkerLayer layer = (MarkerLayer) wwCanvas.getModel().getLayers().getLayerByName("Marker Layer");
            Marker position = null;
            for (Marker marker : layer.getMarkers()) {
                if (marker instanceof Marker) {
                    position = (Marker) marker;
                }
            }
            if (position
                    != null) {
                value = Conversion.positionToLocation(position.getPosition());
            }
        } else if (field.getType().equals(PathUtm.class)) {
            RenderableLayer layer = (RenderableLayer) wwCanvas.getModel().getLayers().getLayerByName("Renderable");
            Path path = null;
            for (Renderable renderable : layer.getRenderables()) {
                if (renderable instanceof Path) {
                    path = (Path) renderable;
                }
            }
            if (path
                    != null) {
                ArrayList<Location> locationList = new ArrayList<Location>();
                for (Position position : path.getPositions()) {
                    locationList.add(Conversion.positionToLocation(position));
                }
                value = new Area2D(locationList);
            }
        } else if (field.getType().equals(Area2D.class)) {
            RenderableLayer layer = (RenderableLayer) wwCanvas.getModel().getLayers().getLayerByName("Renderable");
            SurfacePolygon area = null;
            for (Renderable renderable : layer.getRenderables()) {
                if (renderable instanceof SurfacePolygon) {
                    area = (SurfacePolygon) renderable;
                }
            }
            if (area
                    != null) {
                ArrayList<Location> locationList = new ArrayList<Location>();
                for (LatLon latLon : area.getLocations()) {
                    locationList.add(Conversion.latLonToLocation(latLon));
                }
                value = new Area2D(locationList);
            }
        }
        return value;
    }

    @Override
    public boolean setComponentValue(Object value) {
        boolean success = false;
        if (value.getClass().equals(Location.class)) {
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
            attributes.setMinMarkerSize(50);
            attributes.setMaterial(Material.YELLOW);
            attributes.setOpacity(1);
            BasicMarker circle = new BasicMarker(position, attributes);
            selectWidget.addMarker(circle);
            success = true;
        } else if (value.getClass().equals(PathUtm.class)) {
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
            Path path = new Path(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setOutlineWidth(8);
            attributes.setOutlineMaterial(Material.YELLOW);
            attributes.setDrawOutline(true);
            path.setAttributes(attributes);
            path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            selectWidget.addRenderable(path);
            success = true;
        } else if (value.getClass().equals(Area2D.class)) {
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
        }
        return success;
    }

    @Override
    public void handleMarkups(ArrayList<Markup> markups, MarkupManager manager) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void disableMarkup(Markup markup) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
