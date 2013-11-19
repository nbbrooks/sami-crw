package crw.ui.widget;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author nbb
 */
public class SelectGeometryWidget implements WorldWindWidgetInt {

    public enum SelectMode {

        POINT, PATH, AREA, NONE, CLEAR
    };
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
        Position clickPosition = wwd.getView().computePositionFromScreenPoint(evt.getX(), evt.getY());
        // Continue creating a new area?
        switch (selectMode) {
            case POINT:
                if (clickPosition != null) {
                    BasicMarkerAttributes attributes = new BasicMarkerAttributes();
                    attributes.setShapeType(BasicMarkerShape.SPHERE);
                    attributes.setMinMarkerSize(50);
                    attributes.setMaterial(Material.YELLOW);
                    attributes.setOpacity(1);
                    BasicMarker circle = new BasicMarker(clickPosition, attributes);
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
                if (clickPosition != null) {
                    selectedPositions.add(clickPosition);
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
                if (clickPosition != null) {
                    selectedPositions.add(clickPosition);
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
                for (Marker marker : markers) {
                    Point clickPoint = evt.getPoint();
                    Position tlPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x - marker.getAttributes().getMarkerPixels(), clickPoint.y - marker.getAttributes().getMarkerPixels());
                    Position brPos = wwd.getView().computePositionFromScreenPoint(clickPoint.x + marker.getAttributes().getMarkerPixels(), clickPoint.y + marker.getAttributes().getMarkerPixels());
                    Position markerPos = marker.getPosition();
                    if (positionBetween(markerPos, tlPos, brPos)) {
                        return true;
                    }
                }
                return false;
        }

        return false;
    }

    public boolean positionBetween(Position position, Position northWest, Position southEast) {
        if (position == null || northWest == null || southEast == null) {
            return false;
        }
        Angle latNorth = northWest.latitude;
        Angle latSouth = southEast.latitude;
        Angle lonWest = northWest.longitude;
        Angle lonEast = southEast.longitude;
        if (latSouth.compareTo(latNorth) > 0) {
            // Latitude wrapped around globe
            latSouth = latSouth.subtract(Angle.POS360);
        }
        if (lonWest.compareTo(lonEast) > 0) {
            // Longitude wrapped around globe
            lonWest = lonWest.subtract(Angle.POS180);
        }
        if (latSouth.compareTo(position.latitude) <= 0
                && position.latitude.compareTo(latNorth) <= 0
                && lonWest.compareTo(position.longitude) <= 0
                && position.longitude.compareTo(lonEast) <= 0) {
            return true;
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
        markerLayer.setElevation(10d);
//        highAltMarkerLayer.setKeepSeparated(false);
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
}
