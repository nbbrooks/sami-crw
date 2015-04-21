package crw.ui.widget;

import crw.ui.worldwind.WorldWindWidgetInt;
import crw.Conversion;
import crw.proxy.BoatProxy;
import crw.ui.component.WorldWindPanel;
import static crw.ui.component.WorldWindPanel.SPHERE_SIZE;
import crw.ui.worldwind.AnnotatedMarker;
import crw.ui.worldwind.AnnotatedPolyline;
import crw.ui.worldwind.AnnotatedSurfacePolygon;
import gov.nasa.worldwind.WorldWind;
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
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.area.Area2D;
import sami.engine.Mediator;
import static sami.engine.Mediator.LAST_EPF_FOLDER;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
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
public class AnnotationWidget implements MarkupComponentWidget, WorldWindWidgetInt, EnvironmentListenerInt {

    private static final Logger LOGGER = Logger.getLogger(AnnotationWidget.class.getName());

    public enum SelectMode {

        POINT, PATH, AREA, NONE, DELETE
    };
    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses = new Hashtable<Class, ArrayList<Class>>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses = new Hashtable<Class, ArrayList<Class>>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private final BasicMarkerAttributes UNSEL_MARKER_ATTR = new BasicMarkerAttributes();
    private final Color UNSEL_POLYLINE_COLOR = Color.YELLOW;
    private final double UNSEL_POLYLINE_WIDTH = 8;
    private final ShapeAttributes UNSEL_AREA_ATTR = new BasicShapeAttributes();
    private final BasicMarkerAttributes SEL_MARKER_ATTR = new BasicMarkerAttributes();
    private final Color SEL_POLYLINE_COLOR = Color.RED;
    private final double SEL_POLYLINE_WIDTH = 8;
    private final ShapeAttributes SEL_AREA_ATTR = new BasicShapeAttributes();

    private boolean visible = true;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Position> selectedPositions = new ArrayList<Position>();
    private JButton pointButton, pathButton, areaButton, noneButton, deleteButton, newButton, loadButton, saveButton, saveAsButton, exportButton, importButton;
    private JPanel selectModeP;
    private List<SelectMode> enabledModes;
    private MarkerLayer markerLayer;
    private SelectMode selectMode = SelectMode.NONE;
    private RenderableLayer renderableLayer;
    private WorldWindPanel wwPanel;
    private AnnotatedPolyline polyline = null;
    private AnnotatedSurfacePolygon area = null;
    private AnnotatedMarker highlightedMarker = null;
    private AnnotatedPolyline highlightedPolyline = null;
    private AnnotatedSurfacePolygon highlightedArea = null;
    private boolean selectListenerEnabled = false;
    private SelectListener selectListener;
    private final Object highlightedLock = new Object();

    public AnnotationWidget() {
        populateLists();
    }

    public AnnotationWidget(WorldWindPanel wwPanel) {
        this(wwPanel, Arrays.asList(SelectMode.POINT, SelectMode.PATH, SelectMode.AREA, SelectMode.NONE, SelectMode.DELETE), SelectMode.NONE);
    }

    public AnnotationWidget(final WorldWindPanel wwPanel, List<SelectMode> enabledModes, SelectMode defaultMode) {
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

        UNSEL_MARKER_ATTR.setShapeType(BasicMarkerShape.SPHERE);
        UNSEL_MARKER_ATTR.setMinMarkerSize(SPHERE_SIZE);
        UNSEL_MARKER_ATTR.setMaterial(Material.YELLOW);
        UNSEL_MARKER_ATTR.setOpacity(1);

        UNSEL_AREA_ATTR.setInteriorOpacity(0.5);
        UNSEL_AREA_ATTR.setInteriorMaterial(Material.YELLOW);
        UNSEL_AREA_ATTR.setOutlineWidth(2);
        UNSEL_AREA_ATTR.setOutlineMaterial(Material.BLACK);

        SEL_MARKER_ATTR.setShapeType(BasicMarkerShape.SPHERE);
        SEL_MARKER_ATTR.setMinMarkerSize(SPHERE_SIZE);
        SEL_MARKER_ATTR.setMaterial(Material.RED);
        SEL_MARKER_ATTR.setOpacity(1);

        SEL_AREA_ATTR.setInteriorOpacity(0.5);
        SEL_AREA_ATTR.setInteriorMaterial(Material.RED);
        SEL_AREA_ATTR.setOutlineWidth(2);
        SEL_AREA_ATTR.setOutlineMaterial(Material.BLACK);

        // Set up a SelectListener to know when the cursor is over a BoatMarker
        selectListener = new SelectListener() {

            @Override
            public void selected(SelectEvent event) {
                // Click type SelectEvents are only generated if an event is generated, and
                //  are handled before MouseEvents, which makes deselecting a BoatMarker impossible from a SelectEvent
                // Instead, keep track of if the cursor is over a BoatMarker and let the MouseListener handle selection/deselection

                if (event.getEventAction().equals(SelectEvent.ROLLOVER)
                        && event.hasObjects()) {
                    synchronized (highlightedLock) {
                        if (event.getTopObject() instanceof AnnotatedMarker) {
                            if (highlightedMarker != (Marker) event.getTopObject()) {
                                resetHighlighting();
                            }
                            highlightedMarker = (AnnotatedMarker) event.getTopObject();
                            highlightedMarker.setAttributes(SEL_MARKER_ATTR);
                        } else if (event.getTopObject() instanceof AnnotatedPolyline) {
                            if (highlightedPolyline != (AnnotatedPolyline) event.getTopObject()) {
                                resetHighlighting();
                            }
                            highlightedPolyline = (AnnotatedPolyline) event.getTopObject();
                            highlightedPolyline.setColor(SEL_POLYLINE_COLOR);
                            highlightedPolyline.setLineWidth(SEL_POLYLINE_WIDTH);
                        } else if (event.getTopObject() instanceof AnnotatedSurfacePolygon) {
                            if (highlightedArea != (AnnotatedSurfacePolygon) event.getTopObject()) {
                                resetHighlighting();
                            }
                            highlightedArea = (AnnotatedSurfacePolygon) event.getTopObject();
                            highlightedArea.setAttributes(SEL_AREA_ATTR);
                        } else {
                            resetHighlighting();
                        }
                    }
                } else if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
                    resetHighlighting();
                }
            }
        };

        Mediator.getInstance().addEnvironmentListener(this);
        environmentUpdated();
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
        Position clickPosition = wwd.getCurrentPosition();
        boolean complexHandled = false;
        // Continue creating a new area?
        switch (selectMode) {
            case POINT:
                if (clickPosition != null) {
                    AnnotatedMarker circle = new AnnotatedMarker(clickPosition, UNSEL_MARKER_ATTR);
                    markers.add(circle);
                    setSelectMode(SelectMode.NONE);
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
                    polyline = new AnnotatedPolyline(selectedPositions);
                    polyline.setColor(UNSEL_POLYLINE_COLOR);
                    polyline.setLineWidth(UNSEL_POLYLINE_WIDTH);
                    polyline.setFollowTerrain(true);
                    renderableLayer.addRenderable(polyline);
                    wwd.redraw();
                    complexHandled = true;
                }
                if (evt.getClickCount() > 1 && !evt.isConsumed()) {
                    // Finish path
                    polyline = null;
                    setSelectMode(SelectMode.NONE);
                    complexHandled = true;
                }
                if (complexHandled) {
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
                    area = new AnnotatedSurfacePolygon(UNSEL_AREA_ATTR, selectedPositions);
                    renderableLayer.addRenderable(area);
                    wwd.redraw();
                    complexHandled = true;
                }
                if (evt.getClickCount() > 1 && !evt.isConsumed()) {
                    // Finish area
                    area = null;
                    setSelectMode(SelectMode.NONE);
                    complexHandled = true;
                }
                if (complexHandled) {
                    return true;
                }
                break;
            case NONE:
                return false;
            case DELETE:
                synchronized (highlightedLock) {
                    // If have highlighted item, delete it
                    boolean deleted = false;
                    if (highlightedMarker != null) {
                        markers.remove(highlightedMarker);
                        deleted = true;
                    }
                    if (highlightedPolyline != null) {
                        renderableLayer.removeRenderable(highlightedPolyline);
                        deleted = true;
                    }
                    if (highlightedArea != null) {
                        renderableLayer.removeRenderable(highlightedArea);
                        deleted = true;
                    }
                    if (deleted) {
                        // If something was deleted, set the mode to NONE and consume the click
                        wwd.redraw();
                        setSelectMode(SelectMode.NONE);
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
                setHighlighting(false);

                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(true);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.DELETE)) {
                    deleteButton.setSelected(false);
                }
                break;
            case PATH:
                setHighlighting(false);

                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(true);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.DELETE)) {
                    deleteButton.setSelected(false);
                }
                break;
            case AREA:
                setHighlighting(false);

                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(true);
                }
                if (enabledModes.contains(SelectMode.DELETE)) {
                    deleteButton.setSelected(false);
                }
                break;
            case DELETE:
                setHighlighting(true);

                if (enabledModes.contains(SelectMode.POINT)) {
                    pointButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.PATH)) {
                    pathButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.AREA)) {
                    areaButton.setSelected(false);
                }
                if (enabledModes.contains(SelectMode.DELETE)) {
                    deleteButton.setSelected(true);
                }
                break;
            case NONE:
            default:
                setHighlighting(false);

                boolean redraw = false;
                if (polyline != null) {
                    renderableLayer.removeRenderable(polyline);
                    polyline = null;
                    redraw = true;
                }
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
                if (enabledModes.contains(SelectMode.DELETE)) {
                    deleteButton.setSelected(false);
                }
                if (redraw) {
                    wwPanel.wwCanvas.redraw();
                }
                break;
        }
        selectedPositions = new ArrayList<Position>();
    }

    private void setHighlighting(boolean enable) {
        if (this.selectListenerEnabled == enable) {
            return;
        }
        this.selectListenerEnabled = enable;
        if (selectListenerEnabled) {
            wwPanel.wwCanvas.addSelectListener(selectListener);
        } else {
            boolean redraw = false;
            wwPanel.wwCanvas.removeSelectListener(selectListener);
            resetHighlighting();
        }
    }

    @Override
    public boolean mouseDragged(MouseEvent evt, WorldWindow wwd) {
        return false;
    }

    @Override
    public boolean mouseMoved(MouseEvent evt, WorldWindow wwd) {
        switch (selectMode) {
            case DELETE:

            default:
                return false;
        }
    }

    private void resetHighlighting() {
        synchronized (highlightedLock) {
            boolean redraw = false;
            if (highlightedMarker != null) {
                highlightedMarker.setAttributes(UNSEL_MARKER_ATTR);
                highlightedMarker = null;
                redraw = true;
            }
            if (highlightedPolyline != null) {
                highlightedPolyline.setColor(UNSEL_POLYLINE_COLOR);
                highlightedPolyline.setLineWidth(UNSEL_POLYLINE_WIDTH);
                highlightedPolyline = null;
                redraw = true;
            }
            if (highlightedArea != null) {
                highlightedArea.setAttributes(UNSEL_AREA_ATTR);
                highlightedArea = null;
                redraw = true;
            }
            if (redraw) {
                wwPanel.wwCanvas.redraw();
            }
        }
    }

    @Override
    public boolean mouseWheelMoved(MouseWheelEvent evt, WorldWindow wwd
    ) {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e, WorldWindow wwd
    ) {
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
        markerLayer.setPickEnabled(true);
        markerLayer.setMarkers(markers);
        wwPanel.wwCanvas.getModel().getLayers().add(markerLayer);
        renderableLayer = new RenderableLayer();
        renderableLayer.setPickEnabled(true);
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
    }

    protected void initButtons(List<SelectMode> enabledModes) {
        if (wwPanel == null) {
            return;
        }

        selectModeP = new JPanel();
        selectModeP.setLayout(new BoxLayout(selectModeP, BoxLayout.X_AXIS));

        if (enabledModes.contains(SelectMode.POINT)) {
            pointButton = new JButton("Point");
            pointButton.setMinimumSize(new Dimension(pointButton.getPreferredSize().width, 10));
            selectModeP.add(pointButton);
            pointButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.POINT);
                }
            });
        }
        if (enabledModes.contains(SelectMode.PATH)) {
            pathButton = new JButton("Path");
            pathButton.setMinimumSize(new Dimension(10, pathButton.getPreferredSize().height));
            selectModeP.add(pathButton);
            pathButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.PATH);
                }
            });
        }
        if (enabledModes.contains(SelectMode.AREA)) {
            areaButton = new JButton("Area");
            areaButton.setMinimumSize(new Dimension(10, areaButton.getPreferredSize().height));
            selectModeP.add(areaButton);
            areaButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.AREA);
                }
            });
        }
        if (enabledModes.contains(SelectMode.NONE)) {
            noneButton = new JButton("None");
            noneButton.setMinimumSize(new Dimension(10, noneButton.getPreferredSize().height));
            selectModeP.add(noneButton);
            noneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.NONE);
                }
            });
        }
        if (enabledModes.contains(SelectMode.DELETE)) {
            deleteButton = new JButton("Del");
            deleteButton.setMinimumSize(new Dimension(10, deleteButton.getPreferredSize().height));
            selectModeP.add(deleteButton);
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    setSelectMode(SelectMode.DELETE);
                }
            });
        }

        newButton = new JButton("New");
        newButton.setMinimumSize(new Dimension(10, newButton.getPreferredSize().height));
        selectModeP.add(newButton);
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Mediator.getInstance().newEnvironment();
            }
        });

        loadButton = new JButton("Load");
        loadButton.setMinimumSize(new Dimension(10, loadButton.getPreferredSize().height));
        selectModeP.add(loadButton);
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                boolean success = Mediator.getInstance().openEnvironmentFromBrowser();
                if (!success) {
                    // Couldn't load the plan
                    JOptionPane.showMessageDialog(null, "Could not load environment properties (.EPF) file");
                }
            }
        });

        saveButton = new JButton("Save");
        saveButton.setMinimumSize(new Dimension(10, saveButton.getPreferredSize().height));
        selectModeP.add(saveButton);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                prepareValues();
                Mediator.getInstance().saveEnvironment();
            }
        });

        saveAsButton = new JButton("SaveAs");
        saveAsButton.setMinimumSize(new Dimension(10, saveAsButton.getPreferredSize().height));
        selectModeP.add(saveAsButton);
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                prepareValues();
                Mediator.getInstance().saveEnvironmentAs();
            }
        });

        exportButton = new JButton("Export");
        exportButton.setMinimumSize(new Dimension(10, exportButton.getPreferredSize().height));
        selectModeP.add(exportButton);
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                exportValues();
            }
        });

        importButton = new JButton("Import");
        importButton.setMinimumSize(new Dimension(importButton.getPreferredSize().width, 10));
        selectModeP.add(importButton);
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                importValues();
                wwPanel.wwCanvas.redraw();
            }
        });

        wwPanel.buttonPanels.add(selectModeP, BorderLayout.SOUTH);
        wwPanel.buttonPanels.revalidate();
    }

    public void addMarker(AnnotatedMarker marker) {
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
    public int getCreationWidgetScore(Type type, Field field, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationWidgetScore(supportedCreationClasses, supportedHashtableCreationClasses, supportedMarkups, type, field, markups);
    }

    @Override
    public int getSelectionWidgetScore(Type type, Object object, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionWidgetScore(supportedSelectionClasses, supportedHashtableSelectionClasses, supportedMarkups, type, object, markups);
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
                List<AnnotationWidget.SelectMode> modes = Arrays.asList(AnnotationWidget.SelectMode.POINT, AnnotationWidget.SelectMode.NONE, AnnotationWidget.SelectMode.DELETE);
                widget = new AnnotationWidget((WorldWindPanel) component, modes, AnnotationWidget.SelectMode.POINT);
            } else if (creationClass.equals(PathUtm.class)) {
                List<AnnotationWidget.SelectMode> modes = Arrays.asList(AnnotationWidget.SelectMode.PATH, AnnotationWidget.SelectMode.NONE, AnnotationWidget.SelectMode.DELETE);
                widget = new AnnotationWidget((WorldWindPanel) component, modes, AnnotationWidget.SelectMode.PATH);
            } else if (creationClass.equals(Area2D.class)) {
                List<AnnotationWidget.SelectMode> modes = Arrays.asList(AnnotationWidget.SelectMode.AREA, AnnotationWidget.SelectMode.NONE, AnnotationWidget.SelectMode.DELETE);
                widget = new AnnotationWidget((WorldWindPanel) component, modes, AnnotationWidget.SelectMode.AREA);
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

            AnnotationWidget select = new AnnotationWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            BasicMarkerAttributes attributes = new BasicMarkerAttributes();
            attributes.setShapeType(BasicMarkerShape.SPHERE);
            attributes.setMinMarkerSize(SPHERE_SIZE);
            attributes.setMaterial(Material.YELLOW);
            attributes.setOpacity(1);
            AnnotatedMarker circle = new AnnotatedMarker(position, attributes);
            select.addMarker(circle);
            widget = select;
        } else if (selectionObject instanceof PathUtm) {
            List<Location> waypoints = ((PathUtm) selectionObject).getPoints();
            List<Position> positions = new ArrayList<Position>();
            // Convert from Locations to LatLons
            for (Location waypoint : waypoints) {
                positions.add(Conversion.locationToPosition(waypoint));
            }
            AnnotationWidget select = new AnnotationWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
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
            AnnotationWidget select = new AnnotationWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
            // Add surface polygon of area
            AnnotatedSurfacePolygon polygon = new AnnotatedSurfacePolygon(positions);
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
            AnnotationWidget select = new AnnotationWidget((WorldWindPanel) component, new ArrayList<SelectMode>(), SelectMode.NONE);
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
        // No dynamic markups handled
    }

    @Override
    public void disableMarkup(Markup markup) {
        // No dynamic markups handled
    }

    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        return (ArrayList<Class>) supportedCreationClasses.clone();
    }

    public void clearObstalces() {
        markers.clear();
        renderableLayer.removeAllRenderables();
        wwPanel.wwCanvas.redraw();
    }

    @Override
    public void environmentUpdated() {
        applyValues();
    }

    public void prepareValues() {
        ArrayList<Location> markerPoints = new ArrayList<Location>();
        ArrayList<ArrayList<Location>> linePoints = new ArrayList<ArrayList<Location>>();
        ArrayList<ArrayList<Location>> areaPoints = new ArrayList<ArrayList<Location>>();
        for (Marker marker : markers) {
            if (marker instanceof AnnotatedMarker) {
                markerPoints.add(Conversion.positionToLocation(marker.getPosition()));
            }
        }
        for (Renderable renderable : renderableLayer.getRenderables()) {
            if (renderable instanceof AnnotatedPolyline) {
                AnnotatedPolyline polyline = (AnnotatedPolyline) renderable;
                ArrayList<Location> points = new ArrayList<Location>();
                for (Position p : polyline.getPositions()) {
                    points.add(Conversion.positionToLocation(p));
                }
                linePoints.add(points);
            } else if (renderable instanceof AnnotatedSurfacePolygon) {
                AnnotatedSurfacePolygon area = (AnnotatedSurfacePolygon) renderable;
                ArrayList<Location> points = new ArrayList<Location>();
                for (LatLon l : area.getLocations()) {
                    points.add(Conversion.latLonToLocation(l));
                }
                areaPoints.add(points);
            }
        }

        EnvironmentProperties environment = Mediator.getInstance().getEnvironment();
        Position defaultPosition = wwPanel.getCanvas().getView().getCurrentEyePosition();
        Location defaultLocation = Conversion.positionToLocation(defaultPosition);
        environment.setDefaultLocation(defaultLocation);
        environment.setMarkerPoints(markerPoints);
        environment.setLinePoints(linePoints);
        environment.setAreaPoints(areaPoints);
    }

    public void applyValues() {
        clearObstalces();

        EnvironmentProperties environment = Mediator.getInstance().getEnvironment();
        ArrayList<Location> markerPoints = environment.getMarkerPoints();
        ArrayList<ArrayList<Location>> linePoints = environment.getLinePoints();
        ArrayList<ArrayList<Location>> areaPoints = environment.getAreaPoints();

        // AnnotatedMarker
        for (Location point : markerPoints) {
            AnnotatedMarker circle = new AnnotatedMarker(Conversion.locationToPosition(point), UNSEL_MARKER_ATTR);
            markers.add(circle);
        }
        // AnnotatedPolyline
        for (ArrayList<Location> points : linePoints) {
            ArrayList<Position> obstaclePositions = new ArrayList<Position>();
            for (Location location : points) {
                obstaclePositions.add(Conversion.locationToPosition(location));
            }
            AnnotatedPolyline line = new AnnotatedPolyline(obstaclePositions);
            line.setColor(UNSEL_POLYLINE_COLOR);
            line.setLineWidth(UNSEL_POLYLINE_WIDTH);
            line.setFollowTerrain(true);
            renderableLayer.addRenderable(line);
        }
        // AnnotatedSurfacePolygon
        for (ArrayList<Location> points : areaPoints) {
            ArrayList<Position> obstaclePositions = new ArrayList<Position>();
            for (Location location : points) {
                obstaclePositions.add(Conversion.locationToPosition(location));
            }
            AnnotatedSurfacePolygon area = new AnnotatedSurfacePolygon(UNSEL_AREA_ATTR, obstaclePositions);
            renderableLayer.addRenderable(area);
        }

        wwPanel.wwCanvas.redraw();
    }

    private void exportValues() {
        JFileChooser chooser = new JFileChooser();
        File exportFile;
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_EPF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last EPF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last EPF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Exported Environment Properties", "txt");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().getName().endsWith(".txt")) {
                exportFile = chooser.getSelectedFile();
            } else {
                exportFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".txt");
            }
            LOGGER.info("Exporting environment properties to: " + exportFile.toString());
            try {
                exportFile.createNewFile();
                FileWriter writer = new FileWriter(exportFile);

                prepareValues();
                EnvironmentProperties environment = Mediator.getInstance().getEnvironment();
                ArrayList<Location> markerPoints = environment.getMarkerPoints();
                ArrayList<ArrayList<Location>> linePoints = environment.getLinePoints();
                ArrayList<ArrayList<Location>> areaPoints = environment.getAreaPoints();

                for (Location l : markerPoints) {
                    Position pos = Conversion.locationToPosition(l);
                    writer.write("P\n");
                    writer.write(pos.latitude.toDecimalDegreesString(10).replace("°", "") + "," + pos.longitude.toDecimalDegreesString(10).replace("°", "") + "\n");
                }
                for (ArrayList<Location> line : linePoints) {
                    writer.write("L\n");
                    for (Location l : line) {
                        Position pos = Conversion.locationToPosition(l);
                        writer.write(pos.latitude.toDecimalDegreesString(10).replace("°", "") + "," + pos.longitude.toDecimalDegreesString(10).replace("°", "") + "\n");
                    }
                }
                for (ArrayList<Location> area : areaPoints) {
                    writer.write("A\n");
                    for (Location l : area) {
                        Position pos = Conversion.locationToPosition(l);
                        writer.write(pos.latitude.toDecimalDegreesString(10).replace("°", "") + "," + pos.longitude.toDecimalDegreesString(10).replace("°", "") + "\n");
                    }
                }
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void importValues() {
        SelectMode importSelectMode = SelectMode.NONE;
        JFileChooser chooser = new JFileChooser();
        File importFile;
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_EPF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last EPF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last EPF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Exported Environment Properties", "txt");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            importFile = chooser.getSelectedFile();
            LOGGER.info("Importing environment properties geometry from: " + importFile.toString());
            clearObstalces();

            try {
                BufferedReader br = new BufferedReader(new FileReader(importFile));
                try {
                    String line = br.readLine();
                    while (line != null) {
                        if (line.equals("P")) {
                            // Immediately read in a point
                            String latLon = br.readLine();
                            if (latLon == null) {
                                LOGGER.severe("Failed to process line from environment properties geometry import: " + latLon);
                                line = br.readLine();
                                continue;
                            }
                            int split = latLon.indexOf(",");
                            if (split == -1) {
                                LOGGER.severe("Failed to process line from environment properties geometry import: " + latLon);
                                line = br.readLine();
                                continue;
                            }
                            String lat = latLon.substring(0, split);
                            String lon = latLon.substring(split + 1);
                            try {
                                double pointLat = new Double(lat).doubleValue();
                                double pointLon = new Double(lon).doubleValue();
                                Position position = new Position(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon), 0);
                                AnnotatedMarker circle = new AnnotatedMarker(position, UNSEL_MARKER_ATTR);
                                markers.add(circle);
                            } catch (NumberFormatException ex) {
                                LOGGER.severe("Failed to process line from environment properties geometry import: " + latLon);
                            }
                            selectedPositions = new ArrayList<Position>();
                            polyline = null;
                            area = null;
                        } else if (line.equals("L")) {
                            // Start reading in a line
                            importSelectMode = SelectMode.PATH;
                            selectedPositions = new ArrayList<Position>();
                            polyline = null;
                            area = null;
                        } else if (line.equals("A")) {
                            // Start reading in an area
                            importSelectMode = SelectMode.AREA;
                            selectedPositions = new ArrayList<Position>();
                            polyline = null;
                            area = null;
                        } else {
                            int split = line.indexOf(',');
                            if (split == -1) {
                                LOGGER.severe("Failed to process line from environment properties geometry import: " + line);
                                line = br.readLine();
                                continue;
                            }
                            String lat = line.substring(0, split);
                            String lon = line.substring(split + 1);
                            try {
                                double pointLat = new Double(lat).doubleValue();
                                double pointLon = new Double(lon).doubleValue();
                                Position position = new Position(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon), 0);

                                switch (importSelectMode) {
                                    case POINT:
                                        // Shouldn't end up here, we immediately process points
                                        LOGGER.severe("In importSelectMode switch with Point mode, should not occur");
                                        break;
                                    case PATH:
                                        selectedPositions.add(position);
                                        // Update temporary path
                                        if (polyline != null) {
                                            renderableLayer.removeRenderable(polyline);
                                        }
                                        polyline = new AnnotatedPolyline(selectedPositions);
                                        polyline.setColor(UNSEL_POLYLINE_COLOR);
                                        polyline.setLineWidth(UNSEL_POLYLINE_WIDTH);
                                        polyline.setFollowTerrain(true);
                                        renderableLayer.addRenderable(polyline);
                                        break;
                                    case AREA:
                                        selectedPositions.add(position);
                                        // Update temporary area
                                        if (area != null) {
                                            renderableLayer.removeRenderable(area);
                                        }
                                        area = new AnnotatedSurfacePolygon(UNSEL_AREA_ATTR, selectedPositions);
                                        renderableLayer.addRenderable(area);
                                        break;
                                }
                            } catch (NumberFormatException ex) {
                                LOGGER.severe("Failed to process line from environment properties geometry import: " + line);
                            }
                        }
                        line = br.readLine();
                    }
                } finally {
                    br.close();
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
