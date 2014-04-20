package crw.ui.widget;

import crw.ui.worldwind.WorldWindWidgetInt;
import crw.Conversion;
import crw.ui.component.WorldWindPanel;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import sami.engine.Engine;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
import sami.markup.Markup;
import sami.path.Location;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author nbb
 */
public class ObstacleWidget implements MarkupComponentWidget, WorldWindWidgetInt, EnvironmentListenerInt {

    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private boolean visible = true;
    private RenderableLayer renderableLayer;
    private WorldWindPanel wwPanel;
    ArrayList<SurfacePolygon> obstacleAreas = new ArrayList<SurfacePolygon>();

    public ObstacleWidget() {
        populateLists();
    }

    public ObstacleWidget(WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
        initRenderableLayer();
        Engine.getInstance().addEnvironmentLister(this);
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
        renderableLayer.setPickEnabled(false);
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
    }

    public void addRenderable(Renderable renderable) {
        renderableLayer.addRenderable(renderable);
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        //
        // Markups
        //
    }

    @Override
    public int getCreationWidgetScore(Type type, ArrayList<Markup> markups) {
        int score = MarkupComponentHelper.getCreationWidgetScore(supportedCreationClasses, supportedMarkups, type, markups);
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
        if (component instanceof WorldWindPanel) {
            widget = new ObstacleWidget((WorldWindPanel) component);
        }
        return widget;
    }

    @Override
    public MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = null;
        if (component instanceof WorldWindPanel) {
            widget = new ObstacleWidget((WorldWindPanel) component);
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

    public void addObstacle(ArrayList<Location> obstacleLocations) {
        ArrayList<Position> obstaclePositions = new ArrayList<Position>();
        for (Location location : obstacleLocations) {
            Position position = Conversion.utmToPosition(location.getCoordinate(), 0);
            obstaclePositions.add(position);
        }
        SurfacePolygon area = new SurfacePolygon(obstaclePositions);
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setInteriorOpacity(0.5);
        attributes.setInteriorMaterial(Material.YELLOW);
        attributes.setOutlineWidth(2);
        attributes.setOutlineMaterial(Material.BLACK);
        area.setAttributes(attributes);
        obstacleAreas.add(area);
        renderableLayer.addRenderable(area);
        wwPanel.wwCanvas.redraw();
    }

    public void clearObstalces() {
        for (SurfacePolygon area : obstacleAreas) {
            renderableLayer.removeRenderable(area);
        }
        obstacleAreas.clear();
        wwPanel.wwCanvas.redraw();
    }

    @Override
    public void environmentUpdated() {
        clearObstalces();

        EnvironmentProperties environment = Engine.getInstance().getEnvironmentProperties();
        if (environment != null) {
            for (ArrayList<Location> obstacle : environment.getObstacleList()) {
                addObstacle(obstacle);
            }
        }
    }
}
