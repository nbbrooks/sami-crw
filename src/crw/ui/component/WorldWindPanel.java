package crw.ui.widget;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.MouseInputActionHandler;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import crw.ui.worldwind.WorldWindInputAdapter;

/**
 *
 * @author pscerri
 */
public class WorldWindPanel extends JPanel {

    protected WorldWindowGLCanvas wwCanvas = null;
    public JPanel buttonPanels;
    protected final WorldWindInputAdapter mouseHandler;
    protected final Vector<WorldWindWidgetInt> widgetList;
    protected MouseInputActionHandler handler;
    protected BorderLayout borderLayout;

    public WorldWindPanel() {
        this(800, 600, Double.NaN, Double.NaN, Double.NaN);
    }

    public WorldWindPanel(int width, int height) {
        this(width, height, Double.NaN, Double.NaN, Double.NaN);
    }

    public WorldWindPanel(int width, int height, double lat, double lon, double alt) {
        widgetList = new Vector<WorldWindWidgetInt>();

        // Change mouse handler
        Configuration.setValue(AVKey.VIEW_INPUT_HANDLER_CLASS_NAME, WorldWindInputAdapter.class.getName());

        // Use flat Earth
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());

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
        setLayout(borderLayout);
        // World Wind Canvas
        add(wwCanvas, BorderLayout.CENTER);
        // Button panel
        buttonPanels = new JPanel();
        buttonPanels.setLayout(new BoxLayout(buttonPanels, BoxLayout.Y_AXIS));
        buttonPanels.revalidate();
        add(buttonPanels, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(0, 0));
        setMaximumSize(new java.awt.Dimension(width, height));
        setPreferredSize(new java.awt.Dimension(width, height));
    }

    public BorderLayout getLayout() {
        return borderLayout;
    }

    public JPanel getPanel() {
        return this;
    }

    public JPanel getControl() {
        return null;
    }

    public WorldWindowGLCanvas getCanvas() {
        return wwCanvas;
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

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        // Make this flexible
        frame.getContentPane().setLayout(new FlowLayout());

        WorldWindPanel www = new WorldWindPanel();
        frame.getContentPane().add(www);

        for (Layer l :
                www.getCanvas().getModel().getLayers()) {
            System.out.println("Layer: " + l);
        }

        frame.pack();
        frame.setVisible(true);
    }
}