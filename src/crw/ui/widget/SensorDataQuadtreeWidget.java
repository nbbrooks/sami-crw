package crw.ui.widget;

import com.platypus.crw.VehicleServer;
import crw.ui.worldwind.WorldWindWidgetInt;
import crw.ui.component.WorldWindPanel;
import crw.Conversion;
import crw.quadtree.RenderableQuadTree;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Renderable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.event.InputEvent;
import sami.markup.Markup;
import sami.markup.RelevantInformation;
import sami.path.Location;
import sami.path.UTMCoordinate;
import sami.sensor.Observation;
import sami.sensor.ObservationListenerInt;
import sami.sensor.ObserverInt;
import sami.sensor.ObserverServerListenerInt;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author nbb
 */
public class SensorDataQuadtreeWidget implements MarkupComponentWidget, WorldWindWidgetInt, ObserverServerListenerInt, ObservationListenerInt {

    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses = new Hashtable<Class, ArrayList<Class>>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses = new Hashtable<Class, ArrayList<Class>>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private static final Logger LOGGER = Logger.getLogger(SensorDataQuadtreeWidget.class.getName());
//    // Altitude to switch from rectangles that are fixed size in m (< ALT_THRESH) to spheres that are fixed size in pixels (> ALT_THRESH)
//    static final double ALT_THRESH = 2500.0;
    // What percent difference in data range to trigger a recalculation of heatmap colors for data values
    static final double HEATMAP_THRESH = 10.0;
    // How far (m) from the last measurement recorded a new measurement must be in order to add it to the visualization
    static final double DIST_THRESH = 10.0;
    // Opacity of markers that are fixed size in m (< ALT_THRESH)
    final double RECT_OPACITY = 0.5;
//    private Hashtable<String, Integer> sensorNameToIndex = new Hashtable<String, Integer>();
//    private Hashtable<Integer, String> indexToSensorName = new Hashtable<Integer, String>();
    private String activeSensor;
    private ArrayList<Renderable> activeQuadTreeRenderables = null;
    private HashMap<String, RenderableQuadTree> sensorNameToQuadTree = new HashMap<String, RenderableQuadTree>();
    private RenderableQuadTree activeQuadTree = null;
    private RenderableLayer renderableLayer;
//    private MarkerLayer highAltMarkerLayer;
    JLabel sourceL;
    JComboBox sourceCB;
    private boolean visible = true;
    private WorldWindPanel wwPanel;

    // Lookup for the last location an observation for a particular observor source and sensor was recorded to its corresponding quadtree
    //  no need to record every single value to the quadtree - make the source move a certain distance between recordings
    final private Hashtable<String, UTMCoord> sourceToLastUtm = new Hashtable<String, UTMCoord>();

    public SensorDataQuadtreeWidget() {
        populateLists();
    }

    public SensorDataQuadtreeWidget(final WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
        initRenderableLayer();
        initButtons();
        Engine.getInstance().getObserverServer().addListener(this);

        JFrame f = new JFrame();
        JButton b = new JButton("GENERATE");
        f.add(b);
        f.setSize(new Dimension(200, 100));
        f.setPreferredSize(new Dimension(200, 100));
        b.addActionListener(new ActionListener() {
            Random r = new Random(0L);
            int count = 1;

            @Override
            public void actionPerformed(ActionEvent e) {
//553000.0
//2804000.0
//
//553500.0
//2804500.0
//                UTMCoordinate coord = new UTMCoordinate(553000 + r.nextDouble() * 500, 2804000 + r.nextDouble() * 500, "39R");
                UTMCoordinate coord = new UTMCoordinate(553000 + count * 50 - 1, 2804000 + count * 50 - 1, "39R");
                count++;
                
                double dist = Math.pow(coord.getEasting() - 553375, 2) + Math.pow(coord.getNorthing() - 2804375, 2);
                double value = Math.min(Math.max(100.0 * (1 - (dist / (Math.pow(375, 2) * 2))), 0), 100);

//                System.out.println("### CREATING OBS AT " + (coord.getEasting()) + ", " + (coord.getNorthing()));
//                System.out.println("###\t " + (coord.getEasting() - 553000.0) + ", " + (coord.getNorthing() - 2804000.0));
                System.out.println("");
                System.out.println("### CREATING OBS " + (coord.getEasting() - 553000.0) + ", " + (coord.getNorthing() - 2804000.0) + ": " + value);
                Location l = new Location(coord, 0);
//                Observation o = new Observation("NONE", r.nextDouble() * 100, "P1", l, System.currentTimeMillis());
                Observation o = new Observation("NONE", value, "P1", l, System.currentTimeMillis());
//                    UTMCoordinate coord = new UTMCoordinate(2804466 + count * 10, 553327 + count * 10, "39R");
//                    Location l = new Location(coord, 0);
//                    Observation o = new Observation("NONE", (count + 1) * 10, "P1", l, System.currentTimeMillis());
                newObservation(o);

                if (activeSensor != null && sensorNameToQuadTree.containsKey(activeSensor)) {
                    sensorNameToQuadTree.get(activeSensor).rebuildRenderables();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SensorDataQuadtreeWidget.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    activeQuadTreeRenderables = sensorNameToQuadTree.get(activeSensor).getRenderables();
//                        renderableLayer.clearRenderables();
                    renderableLayer.setRenderables(activeQuadTreeRenderables);
                    wwPanel.wwCanvas.redrawNow();
                }
            }
        });
        f.pack();
        f.setVisible(true);

//        (new Thread() {
//            public void run() {
//                Random r = new Random(0L);
//                while (true) {
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(SensorDataQuadtreeWidget.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    
//                    UTMCoordinate coord = new UTMCoordinate(2804466 + r.nextDouble() * 100, 553327 + r.nextDouble() * 100, "39R");
//                    Location l = new Location(coord, 0);
//                    Observation o = new Observation("NONE", r.nextDouble() * 100, "P1", l, System.currentTimeMillis());
////                    UTMCoordinate coord = new UTMCoordinate(2804466 + count * 10, 553327 + count * 10, "39R");
////                    Location l = new Location(coord, 0);
////                    Observation o = new Observation("NONE", (count + 1) * 10, "P1", l, System.currentTimeMillis());
//                    newObservation(o);
//                    
//                    if (activeSensor != null && sensorNameToQuadTree.containsKey(activeSensor)) {
//                        sensorNameToQuadTree.get(activeSensor).rebuildRenderables();
//                        activeQuadTreeRenderables = sensorNameToQuadTree.get(activeSensor).getRenderables();
////                        renderableLayer.clearRenderables();
//                        renderableLayer.setRenderables(activeQuadTreeRenderables);
////                        wwPanel.wwCanvas.redrawNow();
//                    }
//                }
//            }
//        }).start();
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
        renderableLayer.setRenderables(activeQuadTreeRenderables);
        wwPanel.wwCanvas.getModel().getLayers().add(renderableLayer);
    }

    protected void initButtons() {
        if (wwPanel == null) {
            return;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        sourceL = new JLabel("Visualized data source:");
        buttonPanel.add(sourceL);

        VehicleServer.SensorType[] sensorTypes = VehicleServer.SensorType.values();
        String[] choices = new String[sensorTypes.length + 1];
        final String noSensor = "NONE";
        choices[0] = noSensor;
//        sensorNameToQuadTree.put(noSensor, new RenderableQuadTree(0, 0, 0, 0));
//        sensorNameToIndex.put(noSensor, 0);
//        indexToSensorName.put(0, noSensor);

        for (int i = 0; i < sensorTypes.length; i++) {
            choices[i + 1] = sensorTypes[i].name();
//        sensorNameToQuadTree.put(sensorTypes[i].toString(), new RenderableQuadTree(0, 0, 500000, 500000));
//            sensorNameToIndex.put(sensorTypes[i].toString(), i + 1);
//            indexToSensorName.put(i + 1, sensorTypes[i].toString());
        }
        sourceCB = new JComboBox(choices);
        sourceCB.setSelectedIndex(0);
        activeSensor = noSensor;
        sourceCB.addActionListener(new SensorChoiceLister());
        buttonPanel.add(sourceCB);

        wwPanel.buttonPanels.add(buttonPanel, BorderLayout.SOUTH);
        wwPanel.buttonPanels.revalidate();
    }

    @Override
    public void observerAdded(ObserverInt p) {
        p.addListener(this);
    }

    @Override
    public void observerRemoved(ObserverInt p) {
    }

    @Override
    public void eventOccurred(InputEvent ie) {
    }

    @Override
    public void newObservation(Observation observation) {
        UTMCoord adjUtm = checkObservation(observation);
        if (adjUtm != null) {
//            System.out.println("### ADDING OBS AT " + (observation.location.getCoordinate().getEasting() - 553000.0) + ", " + (observation.location.getCoordinate().getNorthing() - 2804000.0) + " at "
//                    + (adjUtm.getEasting() - 553000.0) + ", " + (adjUtm.getNorthing() - 2804000.0));
            addObservation(observation, adjUtm);
        } else {
            //553000.0
            //2804000.0
//            System.out.println("### DISCARDING OBS AT " + (observation.location.getCoordinate().getEasting() - 553000.0) + ", " + (observation.location.getCoordinate().getNorthing() - 2804000.0));
        }
    }

    /**
     * Check if this sensor data is at least DIST_THRESH away from the last
     * recorded sensor data from this proxy's sensor
     */
    public UTMCoord checkObservation(Observation observation) {

        UTMCoord lastUtm = null;

        synchronized (sourceToLastUtm) {
            lastUtm = sourceToLastUtm.get(observation.getSource() + observation.getVariable());
        }

        // Compute adjusted center location for grid-like visualization
        Position curP = Conversion.utmToPosition(observation.location.getCoordinate(), 0);
        UTMCoord curUtm = UTMCoord.fromLatLon(curP.latitude, curP.longitude);
        //@todo assumes shift does not change zones
        UTMCoord adjUtm = UTMCoord.fromUTM(curUtm.getZone(), curUtm.getHemisphere(),
                DIST_THRESH * Math.floor(curUtm.getEasting() / DIST_THRESH),
                DIST_THRESH * Math.floor(curUtm.getNorthing() / DIST_THRESH));
        if ((lastUtm != null && (Math.sqrt(Math.pow(lastUtm.getEasting() - adjUtm.getEasting(), 2) + Math.pow(lastUtm.getNorthing() - adjUtm.getNorthing(), 2))) < DIST_THRESH)) {
            // Too close to last recorded measurement, skip these measurements
            return null;
        }
        return adjUtm;
    }

    /**
     * Add a heatmapped rectangle and sphere marker representing the sensor data
     * and record the observation
     *
     * @param sd The received sensor data
     * @param adjUtm The UTM coordinate of the sensor data, snapped to the grid
     * defined by DIST_THRESH
     */
    public void addObservation(Observation observation, UTMCoord adjUtm) {
        RenderableQuadTree renderableQuadtree = sensorNameToQuadTree.get(observation.getVariable());
        if (renderableQuadtree == null) {
            //@todo check bounds
            UTMCoord minUtm = UTMCoord.fromUTM(adjUtm.getZone(), adjUtm.getHemisphere(), 553000, 2804000);
            UTMCoord maxUtm = UTMCoord.fromUTM(adjUtm.getZone(), adjUtm.getHemisphere(), 553500, 2804500);
//            UTMCoord minUtm = UTMCoord.fromUTM(adjUtm.getZone(), adjUtm.getHemisphere(), adjUtm.getEasting() - 2500, adjUtm.getNorthing() - 2500);
//            UTMCoord maxUtm = UTMCoord.fromUTM(adjUtm.getZone(), adjUtm.getHemisphere(), adjUtm.getEasting() + 2500, adjUtm.getNorthing() + 2500);
            renderableQuadtree = new RenderableQuadTree(minUtm, maxUtm);
            sensorNameToQuadTree.put(observation.getVariable(), renderableQuadtree);
        }

        // Update location of last observation by this boat and sensor
        synchronized (sourceToLastUtm) {
            sourceToLastUtm.put(observation.getSource() + observation.getVariable(), adjUtm);
        }

        LatLon adjLatLon
                = UTMCoord.locationFromUTMCoord(
                        adjUtm.getZone(),
                        adjUtm.getHemisphere(),
                        adjUtm.getEasting(),
                        adjUtm.getNorthing(),
                        null);

        // Based on zoom level, choose max depth of quadtree to render
        // Recurse through quadtree
        //   If node is a leaf, draw and color from value
        //   If node is empty, draw and color from some calculation from parent's values
        //   If node is pointer
        //      If level < max, recurse down a level
        //      If level == max, draw and color from some calculation from children
        //
        //
        //
        // Adding a value to quadtree
        // Get node's depth
        // If depth <= maxRenderDepth
        //  If we added a value to an empty node: we are replacing a renderable
        //  If we split a leaf: we are removing a renderable and adding 4 new ones
        // If depth > maxRenderDepth
        //  We could recalculate the color of the parent at maxDepth
        renderableQuadtree.set(adjUtm, observation.value);
//        RenderableNode node = renderableQuadtree.getNode(adjUtm.getEasting(), adjUtm.getNorthing());
//
//        /////
//        int maxDepth = 10;
//        /////
//        if (renderableQuadtree.getDepth(node) > maxDepth) {
//
//        } else {
//
//        }
    }

    class SensorChoiceLister implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox) e.getSource();
            String sensorChoice = (String) cb.getSelectedItem();
            if (!activeSensor.equals(sensorChoice)) {
                activeSensor = sensorChoice;
                activeQuadTree = sensorNameToQuadTree.get(sensorChoice);
//            activeQuadTree.checkRecalculateRenderables();
                activeQuadTree.rebuildRenderables();
                renderableLayer.setRenderables(activeQuadTree.getRenderables());
                wwPanel.wwCanvas.redrawNow();
            }
        }
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        //
        // Markups
        supportedMarkups.add(RelevantInformation.Information.SPECIFY);
        supportedMarkups.add(RelevantInformation.Visualization.HEATMAP);
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
        for (Markup markup : markups) {
            if (markup instanceof RelevantInformation) {
                RelevantInformation relevantInformation = (RelevantInformation) markup;
                if (relevantInformation.information == RelevantInformation.Information.SPECIFY) {
                    if (relevantInformation.visualization == RelevantInformation.Visualization.HEATMAP) {
                        widget = new SensorDataQuadtreeWidget((WorldWindPanel) component);
                    }
                }
            }
        }
        return widget;
    }

    @Override
    public MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups) {
        MarkupComponentWidget widget = null;
        for (Markup markup : markups) {
            if (markup instanceof RelevantInformation) {
                RelevantInformation relevantInformation = (RelevantInformation) markup;
                if (relevantInformation.information == RelevantInformation.Information.SPECIFY) {
                    if (relevantInformation.visualization == RelevantInformation.Visualization.HEATMAP) {
                        widget = new SensorDataQuadtreeWidget((WorldWindPanel) component);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object getComponentValue(Field field) {
        return null;
    }

    @Override
    public boolean setComponentValue(Object value) {
        return false;
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
}
