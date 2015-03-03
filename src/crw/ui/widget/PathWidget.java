package crw.ui.widget;

import crw.Conversion;
import crw.ui.worldwind.WorldWindWidgetInt;
import crw.ui.component.WorldWindPanel;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;
import crw.proxy.BoatProxy;
import crw.ui.worldwind.BoatMarker;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import sami.markup.Markup;
import sami.path.Location;
import sami.path.PathUtm;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 * Takes a Hashtable<ProxyInt, PathUtm> and displays each proxy's current
 * position and the path
 *
 * @author nbb
 */
public class PathWidget implements MarkupComponentWidget, WorldWindWidgetInt {

    private static final Logger LOGGER = Logger.getLogger(PathWidget.class.getName());
    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses = new Hashtable<Class, ArrayList<Class>>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses = new Hashtable<Class, ArrayList<Class>>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private boolean visible = true;
    private WorldWindPanel wwPanel;
    // Boat markers
    private final Material UNSELECTED_MAT = new Material(new Color(25, 0, 0));
    private MarkerLayer markerLayer;
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    // Path polylines
    private RenderableLayer renderableLayer;

    public PathWidget() {
        populateLists();
    }

    public PathWidget(WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
        initRenderableLayer();
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

    public void addRenderable(Renderable renderable) {
        renderableLayer.addRenderable(renderable);
    }

    public void addRenderable(Hashtable<ProxyInt, PathUtm> renderable) {

        for (ProxyInt proxy : renderable.keySet()) {
            // Create marker
            if (proxy instanceof BoatProxy) {
                BoatProxy boatProxy = (BoatProxy) proxy;
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

            // Add path
            Polyline proxyPolyline = new Polyline();
            if (proxy instanceof BoatProxy) {
                Color boatColor = ((BoatProxy) proxy).getColor();
                proxyPolyline.setColor(new Color(boatColor.getRed() / 255f, boatColor.getGreen() / 255f, boatColor.getBlue() / 255f, 0.5f));
            } else {
                proxyPolyline.setColor(Color.YELLOW);
            }
            proxyPolyline.setLineWidth(4);
            proxyPolyline.setFollowTerrain(true);

            PathUtm pathUtm = renderable.get(proxy);
            List<Location> waypoints = pathUtm.getPoints();
            List<Position> positions = new ArrayList<Position>();
            for (Location waypoint : waypoints) {
                positions.add(Conversion.locationToPosition(waypoint));
            }
            proxyPolyline.setPositions(positions);
            renderableLayer.addRenderable(proxyPolyline);
        }
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        ArrayList<Class> classes = new ArrayList<Class>();
        classes.add(PathUtm.class);
        supportedHashtableSelectionClasses.put(ProxyInt.class, classes);
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
        MarkupComponentWidget widget = new PathWidget((WorldWindPanel) component);
        return widget;
    }

    @Override
    public MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = new PathWidget((WorldWindPanel) component);

        if (selectionObject.getClass() == Hashtable.class) {
            Hashtable hashtable = (Hashtable) selectionObject;
            if (!hashtable.isEmpty()) {
                Class keyClass = null, valueClass = null;
                for (Object key : hashtable.keySet()) {
                    keyClass = key.getClass();
                    valueClass = hashtable.get(key).getClass();
                    break;
                }

                if (ProxyInt.class.isAssignableFrom(keyClass) && PathUtm.class.isAssignableFrom(valueClass)) {
                    ((PathWidget) widget).addRenderable(hashtable);
                }
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void disableMarkup(Markup markup) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        return (ArrayList<Class>) supportedCreationClasses.clone();
    }
}
