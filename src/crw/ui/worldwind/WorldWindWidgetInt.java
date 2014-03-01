package crw.ui.worldwind;

import crw.ui.component.WorldWindPanel;
import gov.nasa.worldwind.WorldWindow;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 *
 * @author pscerri
 */
public interface WorldWindWidgetInt {

    public void setMap(WorldWindPanel wwPanel);

    public void setVisible(boolean visible);

    public boolean isVisible();

    public void paint(Graphics2D g2d);

    public boolean mouseClicked(MouseEvent evt, WorldWindow wwd);

    public boolean mousePressed(MouseEvent evt, WorldWindow wwd);

    public boolean mouseReleased(MouseEvent evt, WorldWindow wwd);

    public boolean mouseDragged(MouseEvent evt, WorldWindow wwd);

    public boolean mouseMoved(MouseEvent evt, WorldWindow wwd);

    public boolean mouseWheelMoved(MouseWheelEvent evt, WorldWindow wwd);

    public boolean dispatchKeyEvent(KeyEvent e, WorldWindow wwd);
}
