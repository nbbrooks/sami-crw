package crw.ui.widget;

import crw.ui.component.WorldWindPanel;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;
import sami.proxy.ProxyServerListenerInt;
import sami.engine.Engine;
import crw.proxy.BoatProxy;
import crw.ui.component.UiWidget;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;
import sami.markup.Attention;
import sami.markup.RelevantProxy;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class RobotTrackWidget extends UiWidget implements WorldWindWidgetInt, ProxyServerListenerInt {

//    static {
//        computeUiComponent();
//    }
    private static final Logger LOGGER = Logger.getLogger(RobotTrackWidget.class.getName());
    private boolean visible = true;
    private Hashtable<BoatProxy, BoatProxyListener> proxyToListener = new Hashtable<BoatProxy, BoatProxyListener>();
    private RenderableLayer renderableLayer;
    private WorldWindPanel wwPanel;

    public RobotTrackWidget(WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
        initRenderableLayer();
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

        renderableLayer = new RenderableLayer();
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
        renderableLayer.setPickEnabled(false);
    }

    @Override
    public void proxyAdded(ProxyInt proxy) {
        if (proxy instanceof BoatProxy) {
            BoatProxy boatProxy = (BoatProxy) proxy;

            // Create listener to update visualization of proxy's assigned path
            if (proxyToListener.contains(boatProxy)) {
                LOGGER.warning("Proxy somehow already has a listener!");
            } else {
                BoatProxyListener listener = new BoatProxyListener(boatProxy);
                proxyToListener.put(boatProxy, listener);
                boatProxy.addListener(listener);
            }
        }
    }

    @Override
    public void proxyRemoved(ProxyInt proxy) {
        if (proxy instanceof BoatProxy) {
            BoatProxy boatProxy = (BoatProxy) proxy;
            if (proxyToListener.contains(boatProxy)) {
                proxyToListener.get(boatProxy).waypointsComplete();
                boatProxy.removeListener(proxyToListener.get(boatProxy));
                proxyToListener.remove(boatProxy);
            }
        }
    }

    class BoatProxyListener implements ProxyListenerInt {

        Polyline curPolyline = null, futurePolyline = null;
        BoatProxy boatProxy;

        public BoatProxyListener(BoatProxy boatProxy) {
            this.boatProxy = boatProxy;
            Color boatColor = boatProxy.getColor();
            curPolyline = new Polyline();
            curPolyline.setColor(new Color(boatColor.getRed() / 255f, boatColor.getGreen() / 255f, boatColor.getBlue() / 255f, 0.9f));
            curPolyline.setLineWidth(6);
            curPolyline.setFollowTerrain(true);
            futurePolyline = new Polyline();
            futurePolyline.setColor(new Color(boatColor.getRed() / 255f, boatColor.getGreen() / 255f, boatColor.getBlue() / 255f, 0.5f));
            futurePolyline.setLineWidth(4);
            futurePolyline.setFollowTerrain(true);
        }

        @Override
        public void poseUpdated() {
            waypointsUpdated();
        }

        @Override
        public void waypointsComplete() {
            // Remove old waypoint path
            renderableLayer.removeRenderable(curPolyline);
            renderableLayer.removeRenderable(futurePolyline);
            wwPanel.wwCanvas.redraw();
        }

        @Override
        public void eventOccurred(InputEvent e) {
        }

        @Override
        public void waypointsUpdated() {
            // Curent waypoints
            // Remove old waypoint path
            renderableLayer.removeRenderable(curPolyline);
            // Create new waypoint path
            Iterable<Position> positions = boatProxy.getCurrentWaypointsAsPositions();
            Position lastPosForCurWp = null;
            if (positions != null) {
                ArrayList<Position> list = new ArrayList<Position>();
                Iterator<Position> it = positions.iterator();
                while (it.hasNext()) {
                    list.add(it.next());
                }
                if (list.size() == 1) {
                    // If we only have a single point to move to, draw a line between the robot's current location and that point
                    //  Don't do this otherwise, because we don't get updates about individually completed waypoints (only about the path as a whole)
                    //  so it looks weird
                    list.add(0, boatProxy.getCurrLoc());
                }
                if (!list.isEmpty()) {
                    lastPosForCurWp = list.get(list.size() - 1);
                }
                curPolyline.setPositions(list);
                renderableLayer.addRenderable(curPolyline);
            }

            // Future waypoints
            renderableLayer.removeRenderable(futurePolyline);
            positions = boatProxy.getFutureWaypointsAsPositions();
            if (positions != null) {
                ArrayList<Position> list = new ArrayList<Position>();
                Iterator<Position> it = positions.iterator();
                while (it.hasNext()) {
                    list.add(it.next());
                }
                if (!list.isEmpty() && lastPosForCurWp != null) {
                    // If we have future wapoints, connect them to the last of the current waypoints
                    list.add(lastPosForCurWp);
                }
                futurePolyline.setPositions(list);
                renderableLayer.addRenderable(futurePolyline);
            }

            wwPanel.wwCanvas.redraw();
        }
    }

//    public static void computeUiComponent() {
//        // Markups
//        supportedMarkups.add(RelevantProxy.ShowPaths.NO);
//        supportedMarkups.add(RelevantProxy.ShowPaths.YES);
//    }
}
