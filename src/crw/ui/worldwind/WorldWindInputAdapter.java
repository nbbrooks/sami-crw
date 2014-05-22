package crw.ui.worldwind;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.OrbitViewInputHandler;
import java.awt.event.MouseEvent;
import java.util.Vector;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class WorldWindInputAdapter extends OrbitViewInputHandler {

    private static final Logger LOGGER = Logger.getLogger(WorldWindInputAdapter.class.getName());
    protected Vector<WorldWindWidgetInt> widgetList;

    public void setWidgetList(Vector<WorldWindWidgetInt> widgetList) {
        this.widgetList = widgetList;
    }

    @Override
    protected void handleMouseClicked(MouseEvent e) {
        // Forward the event to the widgets
        for (int i = widgetList.size() - 1; i >= 0; i--) {
            WorldWindWidgetInt w = widgetList.get(i);
            if (!w.isVisible()) {
                continue;
            }
            if (w.mouseClicked(e, wwd)) {
                return;
            }
        }
    }

    protected void handleMouseDragged(MouseEvent e) {
        // Forward the event to the widgets
        for (int i = widgetList.size() - 1; i >= 0; i--) {
            WorldWindWidgetInt w = widgetList.get(i);
            if (!w.isVisible()) {
                continue;
            }
            if (w.mouseDragged(e, wwd)) {
                return;
            }
        }
    }

    protected void handleMouseMoved(MouseEvent e) {
        // Forward the event to the widgets
        for (int i = widgetList.size() - 1; i >= 0; i--) {
            WorldWindWidgetInt w = widgetList.get(i);
            if (!w.isVisible()) {
                continue;
            }
            if (w.mouseMoved(e, wwd)) {
                return;
            }
        }
    }

    protected void handleMousePressed(MouseEvent e) {
        // Forward the event to the widgets
        for (int i = widgetList.size() - 1; i >= 0; i--) {
            WorldWindWidgetInt w = widgetList.get(i);
            if (!w.isVisible()) {
                continue;
            }
            if (w.mousePressed(e, wwd)) {
                return;
            }
        }
    }

    protected void handleMouseReleased(MouseEvent e) {
        // Forward the event to the widgets
        for (int i = widgetList.size() - 1; i >= 0; i--) {
            WorldWindWidgetInt w = widgetList.get(i);
            if (!w.isVisible()) {
                continue;
            }
            if (w.mouseReleased(e, wwd)) {
                return;
            }
        }

        // If no one "claims" the mouse click, translate the view
        if (wwd.getCurrentPosition() == null) {
            LOGGER.warning("wwd.getCurrentPosition() is NULL");
            return;
        }
        Position newPos = new Position(wwd.getCurrentPosition().getLatitude(), wwd.getCurrentPosition().getLongitude(), wwd.getView().getCurrentEyePosition().getElevation());
        LOGGER.finest("Clicked: " + newPos.getLatitude() + ", " + newPos.getLongitude());
        wwd.getView().setEyePosition(newPos);
        wwd.redraw();
    }
}
