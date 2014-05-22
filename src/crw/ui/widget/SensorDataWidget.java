package crw.ui.widget;

import crw.ui.worldwind.WorldWindWidgetInt;
import crw.ui.component.WorldWindPanel;
import crw.Conversion;
import edu.cmu.ri.crw.VehicleServer;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceQuad;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.event.InputEvent;
import sami.markup.Markup;
import sami.markup.RelevantInformation;
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
public class SensorDataWidget implements MarkupComponentWidget, WorldWindWidgetInt, ObserverServerListenerInt, ObservationListenerInt {

    // MarkupComponentWidget variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    //
    private static final Logger LOGGER = Logger.getLogger(SensorDataWidget.class.getName());
//    // Altitude to switch from rectangles that are fixed size in m (< ALT_THRESH) to spheres that are fixed size in pixels (> ALT_THRESH)
//    static final double ALT_THRESH = 2500.0;
    // What percent difference in data range to trigger a recalculation of heatmap colors for data values
    static final double HEATMAP_THRESH = 10.0;
    // How far (m) from the last measurement recorded a new measurement must be in order to add it to the visualization
    static final double DIST_THRESH = 10.0;
    // Opacity of markers that are fixed size in m (< ALT_THRESH)
    final double RECT_OPACITY = 0.5;
    // Opacity of spheres that are fixed size in pixels (> ALT_THRESH)
    final double SPHERE_OPACITY = 1.0;
    // Sphere size
    final int SPHERE_SIZE = 30;
    private Hashtable<String, Integer> sensorNameToIndex = new Hashtable<String, Integer>();
    private Hashtable<Integer, String> indexToSensorName = new Hashtable<Integer, String>();
    private ArrayList<ArrayList<Renderable>> lowAltRenderables = new ArrayList<ArrayList<Renderable>>();
//    private ArrayList<ArrayList<Marker>> highAltMarkers = new ArrayList<ArrayList<Marker>>();
    private ArrayList<ArrayList<Observation>> observations = new ArrayList<ArrayList<Observation>>();
    private Hashtable<String, UTMCoord> sourceToLastObsUtm = new Hashtable<String, UTMCoord>();
    private ArrayList<double[]> dataMinMax = new ArrayList<double[]>();
    private ArrayList<Renderable> activeLowAltRenderables = null;
//    private ArrayList<Marker> activeHighAltMarkers = null;
    private double[] activeDataMinMax = null;
    private ArrayList<Observation> activeObservations = null;
    private RenderableLayer lowAltRenderableLayer;
//    private MarkerLayer highAltMarkerLayer;
    JLabel sourceL;
    JComboBox sourceCB;
    private boolean visible = true;
    private WorldWindPanel wwPanel;

    public SensorDataWidget() {
        populateLists();
    }

    public SensorDataWidget(WorldWindPanel wwPanel) {
        this.wwPanel = wwPanel;
        initRenderableLayer();
        initButtons();
        Engine.getInstance().getObserverServer().addListener(this);
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

        lowAltRenderableLayer = new RenderableLayer();
        lowAltRenderableLayer.setPickEnabled(false);
//        lowAltRenderableLayer.setMaxActiveAltitude(ALT_THRESH);
        lowAltRenderableLayer.setRenderables(activeLowAltRenderables);
        wwPanel.wwCanvas.getModel().getLayers().add(lowAltRenderableLayer);

//        highAltMarkerLayer = new MarkerLayer();
//        highAltMarkerLayer.setOverrideMarkerElevation(true);
////        highAltMarkerLayer.setElevation(10d);
//        highAltMarkerLayer.setKeepSeparated(false);
//        highAltMarkerLayer.setPickEnabled(false);
//        highAltMarkerLayer.setMinActiveAltitude(ALT_THRESH);
//        highAltMarkerLayer.setMarkers(activeHighAltMarkers);
//        wwPanel.wwCanvas.getModel().getLayers().add(highAltMarkerLayer);
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
        sensorNameToIndex.put(noSensor, 0);
        indexToSensorName.put(0, noSensor);

        lowAltRenderables.add(null);
//        highAltMarkers.add(null);
        dataMinMax.add(null);
        observations.add(null);

        for (int i = 0; i < sensorTypes.length; i++) {
            choices[i + 1] = sensorTypes[i].name();
            sensorNameToIndex.put(sensorTypes[i].toString(), i + 1);
            indexToSensorName.put(i + 1, sensorTypes[i].toString());

            lowAltRenderables.add(new ArrayList<Renderable>());
//            highAltMarkers.add(new ArrayList<Marker>());
            dataMinMax.add(new double[]{Double.MAX_VALUE, Double.MIN_VALUE, 0.0});
            observations.add(new ArrayList<Observation>());
        }
        sourceCB = new JComboBox(choices);
        sourceCB.setSelectedIndex(0);
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
            addObservation(observation, adjUtm);
        }
    }

    /**
     * Check if this sensor data is at least DIST_THRESH away from the last
     * recorded sensor data from this proxy's sensor
     *
     * @param sd The received sensor data
     * @param curP The proxy's position when the sensor data was received
     * @return The UTM coordinate of the sensor data, snapped to the grid
     * defined by DIST_THRESH
     */
    public UTMCoord checkObservation(Observation observation) {
        int sensorIndex = sensorNameToIndex.get(observation.getVariable());
        ArrayList<Renderable> sensorLowAltRenderables = lowAltRenderables.get(sensorIndex);
//        ArrayList<Marker> sensorHighAltMarkers = highAltMarkers.get(sensorIndex);

        if (sensorLowAltRenderables == activeLowAltRenderables) {
//        if (sensorLowAltRenderables == activeLowAltRenderables || sensorHighAltMarkers == activeHighAltMarkers) {
            // If we are visualizing the dataset this measurement belongs to,
            //  clone the list WW is rendering to avoid concurrent modification exceptions
            lowAltRenderableLayer.setRenderables((ArrayList<Renderable>) sensorLowAltRenderables.clone());
//            highAltMarkerLayer.setMarkers((ArrayList<Marker>) sensorHighAltMarkers.clone());
            wwPanel.wwCanvas.redrawNow();
        }
        // Compute adjusted center location for grid-like visualization
        Position curP = Conversion.utmToPosition(observation.location.getCoordinate(), 0);
        UTMCoord curUtm = UTMCoord.fromLatLon(curP.latitude, curP.longitude);
        //@todo assumes shift does not change zones
        UTMCoord adjUtm = UTMCoord.fromUTM(curUtm.getZone(), curUtm.getHemisphere(),
                DIST_THRESH * Math.floor(curUtm.getEasting() / DIST_THRESH),
                DIST_THRESH * Math.floor(curUtm.getNorthing() / DIST_THRESH));
        UTMCoord lastUtm = sourceToLastObsUtm.get(observation.getSource() + observation.getVariable());
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
        int sensorIndex = sensorNameToIndex.get(observation.getVariable());
        ArrayList<Renderable> sensorLowAltRenderables = lowAltRenderables.get(sensorIndex);
//        ArrayList<Marker> sensorHighAltMarkers = highAltMarkers.get(sensorIndex);
        ArrayList<Observation> sensorObservations = observations.get(sensorIndex);
        double[] sensorDataMinMax = dataMinMax.get(sensorIndex);
        boolean rangeChanged = false;

        synchronized (sourceToLastObsUtm) {
            sourceToLastObsUtm.put(observation.getSource() + observation.getVariable(), adjUtm);
        }
        LatLon adjLatLon
                = UTMCoord.locationFromUTMCoord(
                        adjUtm.getZone(),
                        adjUtm.getHemisphere(),
                        adjUtm.getEasting(),
                        adjUtm.getNorthing(),
                        null);

        // Check if sensor measurement is outside of current heatmap bounds
        if (observation.value < sensorDataMinMax[0]) {
            sensorDataMinMax[0] = observation.value;
            rangeChanged = true;
        }
        if (observation.value > sensorDataMinMax[1]) {
            sensorDataMinMax[1] = observation.value;
            rangeChanged = true;
        }
        // Add observation
        synchronized (sensorObservations) {
            sensorObservations.add(observation);
        }
        // Compute heatmap color from current heatmap bounds
        Color dataColor = dataToColor(observation.value, sensorDataMinMax[0], sensorDataMinMax[1]);
        Material material = new Material(dataColor);
//        // Create high altitude sphere
//        BasicMarkerAttributes markerAtt = new BasicMarkerAttributes();
//        markerAtt.setShapeType(BasicMarkerShape.SPHERE);
//        markerAtt.setMinMarkerSize(SPHERE_SIZE);
//        markerAtt.setMaterial(material);
//        markerAtt.setOpacity(SPHERE_OPACITY);
//        Position curP = new Position(adjLatLon, 0);
//        BasicMarker marker = new BasicMarker(curP, markerAtt);
//        synchronized (sensorHighAltMarkers) {
//            sensorHighAltMarkers.add(marker);
//        }
        // Create low altitude square
        SurfaceQuad rect = new SurfaceQuad(adjLatLon, DIST_THRESH, DIST_THRESH);
        ShapeAttributes rectAtt = new BasicShapeAttributes();
        rectAtt.setInteriorMaterial(material);
        rectAtt.setInteriorOpacity(RECT_OPACITY);
        rectAtt.setOutlineMaterial(material);
        rectAtt.setOutlineOpacity(RECT_OPACITY);
        rectAtt.setOutlineWidth(0);
        rect.setAttributes(rectAtt);
        synchronized (sensorLowAltRenderables) {
            sensorLowAltRenderables.add(rect);
        }

        boolean recompute = false;
        if (rangeChanged && (sensorLowAltRenderables == activeLowAltRenderables)) {
//        if (rangeChanged && (sensorLowAltRenderables == activeLowAltRenderables || sensorHighAltMarkers == activeHighAltMarkers)) {
            // If a observation was outside of the current heatmap min/max bounds and is from the sensor being visualized, check if the heatmap needs updating
            double[] activeDataMinMaxClone;
            synchronized (activeDataMinMax) {
                activeDataMinMaxClone = activeDataMinMax.clone();
            }
            if (checkHeatmap(activeDataMinMaxClone)) {
                // In separate thread, recompute heatmap and update marker and renderable layer
                recompute = true;
                recomputeHeatmap();
            }
        }
        if (!recompute && (sensorLowAltRenderables == activeLowAltRenderables)) {
//        if (!recompute && (sensorLowAltRenderables == activeLowAltRenderables || sensorHighAltMarkers == activeHighAltMarkers)) {
            // Heatmap was not updated, switch back from cloned version of list to original list with the received observation added
            lowAltRenderableLayer.setRenderables(activeLowAltRenderables);
//            highAltMarkerLayer.setMarkers(activeHighAltMarkers);
            wwPanel.wwCanvas.redrawNow();
        }
    }

    /**
     * Check if a sensor's values have diverged enough to warrant recomputation
     * of heatmap colors
     *
     * @param dataMinMax Bounds of the data of interest
     * @return True if heatmap needs to be recomputed
     */
    public boolean checkHeatmap(double[] dataMinMax) {
        if (((dataMinMax[1] - dataMinMax[0]) / dataMinMax[2] - 1) * 100.0 > HEATMAP_THRESH) {
            return true;
        }
        return false;
    }

    /**
     * Recomputes the currently viewed sensor's heatmap values in a separate
     * thread
     */
    public void recomputeHeatmap() {
//        System.out.println("### RECOMPUTE ###");
        (new Thread() {
            public void run() {
                activeDataMinMax[2] = activeDataMinMax[1] - activeDataMinMax[0];
                Iterator obsIt = activeObservations.iterator();
                Iterator lowAltIt = activeLowAltRenderables.iterator();
//                Iterator highAltIt = activeHighAltMarkers.iterator();

                while (obsIt.hasNext() && lowAltIt.hasNext()) {
//                while (obsIt.hasNext() && lowAltIt.hasNext() && highAltIt.hasNext()) {
                    Observation o = (Observation) obsIt.next();
                    Renderable r = (Renderable) lowAltIt.next();
//                    Marker m = (Marker) highAltIt.next();

                    Color dataColor = dataToColor(o.getValue(), activeDataMinMax[0], activeDataMinMax[1]);
                    Material material = new Material(dataColor);
//                    ((BasicMarker) m).getAttributes().setMaterial(material);
                    ((SurfaceQuad) r).getAttributes().setInteriorMaterial(material);
                    ((SurfaceQuad) r).getAttributes().setOutlineMaterial(material);
                }
                lowAltRenderableLayer.setRenderables(activeLowAltRenderables);
//                highAltMarkerLayer.setMarkers(activeHighAltMarkers);
                wwPanel.wwCanvas.redrawNow();

            }
        }).start();
    }

    /**
     * Heatmap color computation taken from
     * http://stackoverflow.com/questions/2374959/algorithm-to-convert-any-positive-integer-to-an-rgb-value
     *
     * @param value The value to compute the color for
     * @param min Min value of data range
     * @param max Max value of data range
     * @return Heatmap color
     */
    public Color dataToColor(double value, double min, double max) {

        double wavelength = 0.0, factor = 0.0, red = 0.0, green = 0.0, blue = 0.0, gamma = 1.0;
        double adjMin = min - 5;
        double adjMax = max - 5;

        if (value < adjMin) {
            wavelength = 0.0;
        } else if (value <= adjMax) {
            wavelength = (value - adjMin) / (adjMax - adjMin) * (750.0f - 350.0f) + 350.0f;
        } else {
            wavelength = 0.0;
        }

        if (wavelength == 0.0f) {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        } else if (wavelength < 440.0f) {
            red = -(wavelength - 440.0f) / (440.0f - 350.0f);
            green = 0.0;
            blue = 1.0;
        } else if (wavelength < 490.0f) {
            red = 0.0;
            green = (wavelength - 440.0f) / (490.0f - 440.0f);
            blue = 1.0;
        } else if (wavelength < 510.0f) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510.0f) / (510.0f - 490.0f);
        } else if (wavelength < 580.0f) {
            red = (wavelength - 510.0f) / (580.0f - 510.0f);
            green = 1.0;
            blue = 0.0;
        } else if (wavelength < 645) {
            red = 1.0;
            green = -(wavelength - 645.0f) / (645.0f - 580.0f);
            blue = 0.0;
        } else {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        }

        if (wavelength == 0.0f) {
            factor = 0.0;
        } else if (wavelength < 420) {
            factor = 0.3f + 0.7f * (wavelength - 350.0f) / (420.0f - 350.0f);
        } else if (wavelength < 680) {
            factor = 1.0;
        } else {
            factor = 0.3f + 0.7f * (750.0f - wavelength) / (750.0f - 680.0f);
        }

        Color color = new Color(
                (int) Math.floor(255.0 * Math.pow(red * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(green * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(blue * factor, gamma)));
        return color;
    }

    class SensorChoiceLister implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox) e.getSource();
            String sensorChoice = (String) cb.getSelectedItem();
            int index = sensorNameToIndex.get(sensorChoice);
            activeLowAltRenderables = lowAltRenderables.get(index);
//            activeHighAltMarkers = highAltMarkers.get(index);
            activeDataMinMax = dataMinMax.get(index);
            activeObservations = observations.get(index);

            boolean recompute = false;
            if (activeDataMinMax != null) {
                // May have new values that changed the data range since last time we visualized this sensor type, check if heatmap needs recomputing
                double[] activeDataMinMaxClone;
                synchronized (activeDataMinMax) {
                    activeDataMinMaxClone = activeDataMinMax.clone();
                }
                if (checkHeatmap(activeDataMinMaxClone)) {
                    recompute = true;
                    recomputeHeatmap();
                } else {
                    lowAltRenderableLayer.setRenderables(activeLowAltRenderables);
//                    highAltMarkerLayer.setMarkers(activeHighAltMarkers);
                    wwPanel.wwCanvas.redrawNow();
                }
            } else {
                lowAltRenderableLayer.setRenderables(null);
//                highAltMarkerLayer.setMarkers(null);
                wwPanel.wwCanvas.redraw();
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
    public int getCreationWidgetScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationWidgetScore(supportedCreationClasses, supportedMarkups, type, markups);
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
        for (Markup markup : markups) {
            if (markup instanceof RelevantInformation) {
                RelevantInformation relevantInformation = (RelevantInformation) markup;
                if (relevantInformation.information == RelevantInformation.Information.SPECIFY) {
                    if (relevantInformation.visualization == RelevantInformation.Visualization.HEATMAP) {
                        widget = new SensorDataWidget((WorldWindPanel) component);
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
                        widget = new SensorDataWidget((WorldWindPanel) component);
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
    }

    @Override
    public void disableMarkup(Markup markup) {
    }
}
