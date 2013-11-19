package crw.ui;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.Conversion;
import crw.proxy.BoatProxy;
import crw.ui.widget.SelectGeometryWidget;
import crw.ui.widget.SelectGeometryWidget.SelectMode;
import crw.ui.widget.WorldWindPanel;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import sami.allocation.ResourceAllocation;
import sami.area.Area2D;
import sami.path.Location;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.uilanguage.UiComponentGeneratorInt;

/**
 *
 * @author nbb
 */
public class CrwUiComponentGenerator implements UiComponentGeneratorInt {
    
    private final static Logger LOGGER = Logger.getLogger(CrwUiComponentGenerator.class.getName());
    
    private static class CrwUiComponentGeneratorHolder {
        
        public static final CrwUiComponentGenerator INSTANCE = new CrwUiComponentGenerator();
    }
    
    private CrwUiComponentGenerator() {
    }
    
    public static CrwUiComponentGenerator getInstance() {
        return CrwUiComponentGenerator.CrwUiComponentGeneratorHolder.INSTANCE;
    }
    
    public JComponent getFakeComponent() {
        JComponent component = new JLabel("No handler");
        
        int numAllocs = 3;
        Random random = new Random();
        Object[][] table = new Object[numAllocs][3];
        Object[] header = new Object[]{"", "", ""};
        int row = 0;
        for (int alloc = 0; alloc < numAllocs; alloc++) {
            table[row][0] = random.nextDouble() + " (" + random.nextDouble() + ")";
            table[row][1] = " \u21e8 ";
            table[row][2] = random.nextDouble() + " (" + random.nextDouble() + ")";
            row++;
            
        }
        component = new JTable(table, header);
        ((JTable) component).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int width;
        for (int c = 0; c < table[0].length; c++) {
            width = 0;
            for (int r = 0; r < table.length; r++) {
                TableCellRenderer renderer = ((JTable) component).getCellRenderer(r, c);
                Component comp = ((JTable) component).prepareRenderer(renderer, r, c);
                width = Math.max(comp.getPreferredSize().width + ((JTable) component).getIntercellSpacing().width, width);
            }
            ((JTable) component).getColumnModel().getColumn(c).setMaxWidth(width);
        }
        ((JTable) component).setShowGrid(false);
        return component;
    }
    
    @Override
    public JComponent getCreationComponent(Class objectClass) {
        JComponent component = null;
        if (objectClass.equals(Location.class)) {
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.POINT, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, modes, SelectGeometryWidget.SelectMode.POINT);
            worldWindPanel.addWidget(select);
            component = worldWindPanel;
        } else if (objectClass.equals(PathUtm.class)) {
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.PATH, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, modes, SelectGeometryWidget.SelectMode.PATH);
            worldWindPanel.addWidget(select);
            component = worldWindPanel;
        } else if (objectClass.equals(Area2D.class)) {
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.AREA, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, modes, SelectGeometryWidget.SelectMode.AREA);
            worldWindPanel.addWidget(select);
            component = worldWindPanel;
        } else if (objectClass.equals(Color.class)) {
            component = new ColorSlider();
        } else if (objectClass.equals(String.class)
                || objectClass.equals(Double.class)
                || objectClass.equals(Float.class)
                || objectClass.equals(Integer.class)
                || objectClass.equals(Long.class)
                || objectClass.equals(double.class)
                || objectClass.equals(float.class)
                || objectClass.equals(int.class)
                || objectClass.equals(long.class)) {
            component = new JTextField();
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        } else if (objectClass.isEnum()) {
            component = new JComboBox(objectClass.getEnumConstants());
        } else {
            LOGGER.severe("Could not generate component for object class: " + objectClass);
        }
        return component;
    }
    
    @Override
    public JComponent getSelectionComponent(Object object) {
        JComponent component = null;
        if (object instanceof ResourceAllocation) {
            Map<ITask, AbstractAsset> allocation = ((ResourceAllocation) object).getAllocation();
            if (allocation.isEmpty()) {
                return null;
            }
            Object[][] table = new Object[allocation.size()][3];
            Object[] header = new Object[]{"", "", ""};
            int row = 0;
            for (ITask task : allocation.keySet()) {
                AbstractAsset asset = allocation.get(task);
                table[row][0] = asset.getName() + " (" + asset.getClass().getSimpleName() + ")";
                table[row][1] = " \u21e8 ";
                table[row][2] = task.getName() + " (" + task.getClass().getSimpleName() + ")";
                row++;
            }
            component = new JTable(table, header);
            ((JTable) component).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int width;
            for (int c = 0; c < table[0].length; c++) {
                width = 0;
                for (int r = 0; r < table.length; r++) {
                    TableCellRenderer renderer = ((JTable) component).getCellRenderer(r, c);
                    Component comp = ((JTable) component).prepareRenderer(renderer, r, c);
                    width = Math.max(comp.getPreferredSize().width + ((JTable) component).getIntercellSpacing().width, width);
                }
                ((JTable) component).getColumnModel().getColumn(c).setMaxWidth(width);
            }
            ((JTable) component).setShowGrid(false);
        } else if (object instanceof Location) {
            Location location = (Location) object;
            Position position = Conversion.locationToPosition(location);
            
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, new ArrayList<SelectMode>(), SelectMode.NONE);
            worldWindPanel.addWidget(select);
            BasicMarkerAttributes attributes = new BasicMarkerAttributes();
            attributes.setShapeType(BasicMarkerShape.SPHERE);
            attributes.setMinMarkerSize(50);
            attributes.setMaterial(Material.YELLOW);
            attributes.setOpacity(1);
            BasicMarker circle = new BasicMarker(position, attributes);
            select.addMarker(circle);
            component = worldWindPanel;
        } else if (object instanceof PathUtm) {
            List<Location> waypoints = ((PathUtm) object).getPoints();
            List<Position> positions = new ArrayList<Position>();
            // Convert from Locations to LatLons
            for (Location waypoint : waypoints) {
                positions.add(Conversion.locationToPosition(waypoint));
            }
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, new ArrayList<SelectMode>(), SelectMode.NONE);
            worldWindPanel.addWidget(select);
            // Add path
            Path path = new Path(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setOutlineWidth(8);
            attributes.setOutlineMaterial(Material.YELLOW);
            attributes.setDrawOutline(true);
            path.setAttributes(attributes);
            path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            select.addRenderable(path);
            component = worldWindPanel;
        } else if (object instanceof Area2D) {
            List<Location> locations = ((Area2D) object).getPoints();
            List<Position> positions = new ArrayList<Position>();
            // Convert from Locations to LatLons
            for (Location location : locations) {
                positions.add(Conversion.locationToPosition(location));
            }
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, new ArrayList<SelectMode>(), SelectMode.NONE);
            worldWindPanel.addWidget(select);
            // Add surface polygon of area
            SurfacePolygon polygon = new SurfacePolygon(positions);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setInteriorOpacity(0.5);
            attributes.setInteriorMaterial(Material.YELLOW);
            attributes.setOutlineWidth(2);
            attributes.setOutlineMaterial(Material.BLACK);
            polygon.setAttributes(attributes);
            select.addRenderable(polygon);
            component = worldWindPanel;
        } else if (object instanceof BoatProxy) {
            BoatProxy boatProxy = (BoatProxy) object;
            component = new JLabel(boatProxy.toString());
            component.setFont(new java.awt.Font("Lucida Grande", 1, 13));
            component.setBorder(BorderFactory.createLineBorder(boatProxy.getColor(), 6));
            component.setForeground(boatProxy.getColor());
            component.setOpaque(true);
        } else if (object instanceof Color) {
            component = new JPanel();
            component.setBackground((Color) object);
        } else if (object instanceof Hashtable) {
            Hashtable hashtable = ((Hashtable) object);
            Object keyObject = null, valueObject = null;
            if (hashtable.size() > 0) {
                for (Object key : hashtable.keySet()) {
                    if (key != null && hashtable.get(key) != null) {
                        keyObject = key;
                        valueObject = hashtable.get(key);
                        break;
                    }
                }
            }
            if (keyObject != null && valueObject != null) {
                component = handleHashtable(hashtable, keyObject, valueObject);
            }
        } else {
            LOGGER.severe("Could not selection component for object class: " + object.getClass().getSimpleName());
        }
        return component;
    }
    
    public JComponent handleHashtable(Hashtable hashtable, Object keyObject, Object valueObject) {
        if (keyObject == null || valueObject == null) {
            return null;
        }
        JComponent component = null;
        if (keyObject instanceof ProxyInt && valueObject instanceof PathUtm) {
            Hashtable<ProxyInt, PathUtm> proxyPaths = (Hashtable<ProxyInt, PathUtm>) hashtable;
            WorldWindPanel worldWindPanel = new WorldWindPanel(500, 300);
            SelectGeometryWidget select = new SelectGeometryWidget(worldWindPanel, new ArrayList<SelectMode>(), SelectMode.NONE);
            worldWindPanel.addWidget(select);
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
            component = worldWindPanel;
        }
        return component;
    }
    
    @Override
    public Object getComponentValue(JComponent component, Field field) {
        Object value = null;
        
        
        
        
        if (component instanceof WorldWindPanel) {
            if (field.getType().equals(Location.class)) {
                WorldWindPanel wwp = (WorldWindPanel) component;
                MarkerLayer layer = (MarkerLayer) wwp.getCanvas().getModel().getLayers().getLayerByName("Marker Layer");
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
                WorldWindPanel wwp = (WorldWindPanel) component;
                RenderableLayer layer = (RenderableLayer) wwp.getCanvas().getModel().getLayers().getLayerByName("Renderable");
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
                WorldWindPanel wwp = (WorldWindPanel) component;
                RenderableLayer layer = (RenderableLayer) wwp.getCanvas().getModel().getLayers().getLayerByName("Renderable");
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
        } else if (component instanceof JTextField) {
            String text = ((JTextField) component).getText();
            
            
            
            
            if (text.length() > 0) {
                try {
                    if (field.getType().equals(String.class)) {
                        value = text;
                    } else if (field.getType().equals(Double.class)) {
                        value = new Double(text);
                    } else if (field.getType().equals(Float.class)) {
                        value = new Float(text);
                    } else if (field.getType().equals(Integer.class)) {
                        value = new Integer(text);
                    } else if (field.getType().equals(Long.class)) {
                        value = new Long(text);
                    } else if (field.getType().equals(double.class)) {
                        value = Double.parseDouble(text);
                    } else if (field.getType().equals(float.class)) {
                        value = Float.parseFloat(text);
                    } else if (field.getType().equals(int.class)) {
                        value = Integer.parseInt(text);
                    } else if (field.getType().equals(long.class)) {
                        value = Long.parseLong(text);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Exception encountered when trying to get value for field type: " + field.getType() + " from text: " + text + ", setting value to null with exception: " + e);
                    value = null;
                }
            }
        } else if (component instanceof JComboBox) {
            value = ((JComboBox) component).getSelectedItem();
            
            
            
            
        } else if (component instanceof JSlider) {
            if (field.getType().equals(Color.class)) {
                value = component.getBackground();
            }
        } else {
            LOGGER.severe("Could not get component value for component: " + component + " and field type: " + field.getType().getSimpleName());
        }
        return value;
    }
    
    @Override
    public boolean setComponentValue(Object value, JComponent component) {
        boolean success = false;
        
        
        
        
        if (component instanceof WorldWindPanel) {
            if (value.getClass().equals(Location.class)) {
                WorldWindPanel wwp = (WorldWindPanel) component;
                // Grab or create the geometry widget
                SelectGeometryWidget selectWidget;
                
                if (wwp.hasWidget(SelectGeometryWidget.class)) {
                    selectWidget = (SelectGeometryWidget) wwp.getWidget(SelectGeometryWidget.class);
                } else {
                    selectWidget = new SelectGeometryWidget(wwp, new ArrayList<SelectMode>(), SelectMode.NONE);
                    wwp.addWidget(selectWidget);
                }
                // Add the marker of position
                Location location = (Location) value;
                Position position = Conversion.locationToPosition(location);
                BasicMarkerAttributes attributes = new BasicMarkerAttributes();
                
                attributes.setShapeType(BasicMarkerShape.SPHERE);
                
                attributes.setMinMarkerSize(
                        50);
                attributes.setMaterial(Material.YELLOW);
                
                attributes.setOpacity(
                        1);
                BasicMarker circle = new BasicMarker(position, attributes);
                
                selectWidget.addMarker(circle);
                success = true;
            } else if (value.getClass().equals(PathUtm.class)) {
                WorldWindPanel wwp = (WorldWindPanel) component;
                // Grab or create the geometry widget
                SelectGeometryWidget selectWidget;
                
                if (wwp.hasWidget(SelectGeometryWidget.class)) {
                    selectWidget = (SelectGeometryWidget) wwp.getWidget(SelectGeometryWidget.class);
                } else {
                    selectWidget = new SelectGeometryWidget(wwp, new ArrayList<SelectMode>(), SelectMode.NONE);
                    wwp.addWidget(selectWidget);
                }
                // Add surface polygon of area
                List<Location> waypoints = ((PathUtm) value).getPoints();
                List<Position> positions = new ArrayList<Position>();
                for (Location waypoint : waypoints) {
                    positions.add(Conversion.locationToPosition(waypoint));
                }
                Path path = new Path(positions);
                ShapeAttributes attributes = new BasicShapeAttributes();
                
                attributes.setOutlineWidth(
                        8);
                attributes.setOutlineMaterial(Material.YELLOW);
                
                attributes.setDrawOutline(
                        true);
                path.setAttributes(attributes);
                
                path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                
                selectWidget.addRenderable(path);
                success = true;
            } else if (value.getClass().equals(Area2D.class)) {
                WorldWindPanel wwp = (WorldWindPanel) component;
                // Grab or create the geometry widget
                SelectGeometryWidget selectWidget;
                
                if (wwp.hasWidget(SelectGeometryWidget.class)) {
                    selectWidget = (SelectGeometryWidget) wwp.getWidget(SelectGeometryWidget.class);
                } else {
                    selectWidget = new SelectGeometryWidget(wwp, new ArrayList<SelectMode>(), SelectMode.NONE);
                    wwp.addWidget(selectWidget);
                }
                // Add surface polygon of area
                List<Location> locations = ((Area2D) value).getPoints();
                List<Position> positions = new ArrayList<Position>();
                for (Location location : locations) {
                    positions.add(Conversion.locationToPosition(location));
                }
                SurfacePolygon polygon = new SurfacePolygon(positions);
                ShapeAttributes attributes = new BasicShapeAttributes();
                
                attributes.setInteriorOpacity(
                        0.5);
                attributes.setInteriorMaterial(Material.YELLOW);
                
                attributes.setOutlineWidth(
                        2);
                attributes.setOutlineMaterial(Material.BLACK);
                
                polygon.setAttributes(attributes);
                
                selectWidget.addRenderable(polygon);
                success = true;
            }
        } else if (component instanceof JTextField) {
            ((JTextField) component).setText(value.toString());
        } else if (component instanceof JComboBox) {
            ((JComboBox) component).setSelectedItem(value);
        } else if (component instanceof ColorSlider) {
            if (value instanceof Color) {
                ((ColorSlider) component).setBackground((Color) value);
            }
        } else {
            LOGGER.severe("Could not set component value for component: " + component + " and value class: " + value.getClass().getSimpleName());
        }
        return success;
    }
}
