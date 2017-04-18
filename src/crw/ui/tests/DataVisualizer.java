package crw.ui.tests;

import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import crw.ui.widget.SensorDataWidget;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.render.Polyline;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.Conversion;
import sami.path.Location;
import sami.sensor.Observation;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;

/**
 * Plot the path a boat traveled from its log data
 *
 * @author nbb
 */
public class DataVisualizer extends UiFrame {

    public WorldWindPanel wwPanel;
    UiClientInt uiClient;
    UiServerInt uiServer;
    public AnnotationWidget annotation;
    public SensorDataWidget data;
    static final double DIST_STEP_THRESH = 0.00005;
    static Color BOAT_COLOR = Color.yellow;
    static int LINE_WIDTH = 2;
    static final boolean VIZ_PATH = true;
    static final boolean VIZ_DATA = false;

    HashMap<String, ArrayList<Position>> directoryNameToPath = new HashMap<String, ArrayList<Position>>();

    static int MIN_POS = -1;
    static int MAX_POS = -1;

    static Date MIN_DATE = null;
    static Date MAX_DATE = null;

    public DataVisualizer() {
        super("SensorTest");
        getContentPane().setLayout(new BorderLayout());

        // Add map
        wwPanel = new WorldWindPanel();
        wwPanel.createMap(800, 600, null);
        getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
        annotation = new AnnotationWidget(wwPanel);
        wwPanel.addWidget(annotation);
        data = new SensorDataWidget(wwPanel);
        wwPanel.addWidget(annotation);

        pack();
        setVisible(true);
    }

    public boolean loadTeamDirectory(String path) {
        return loadTeamDirectory(path, null, null);
    }

    public boolean loadTeamDirectory(String path, final String[] directoryNameExclusions, final String[] fileNameInclusions) {

//    }
//    
//    public boolean loadTeamDirectory(String path, String filenameRestrictions) {
        System.out.println("Loading team directory: " + path);

        File directory = new File(path);
        if (directory != null && directory.isDirectory()) {

            File[] subDirectories = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        return false;
                    }
                    if (directoryNameExclusions != null) {
                        for (String s : directoryNameExclusions) {
                            if (file.getName().contains(s)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });

            for (File subDirectory : subDirectories) {
                if (subDirectory.getName().contains("ylw")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("org")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("grn")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("red")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("blu")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("blk")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
                } else if (subDirectory.getName().contains("wht")) {
                    loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
//                } else {
//                    BOAT_COLOR = Color.BLACK;
//                    loadDirectory(subDirectory.getAbsolutePath(), fileNameRestrictions);
                }
            }
        }
        return true;
    }

    public boolean loadDirectory(String path) {
        return loadDirectory(path, null);
    }

    public boolean loadDirectory(String path, final String[] fileNameInclusions) {
        File directory = new File(path);
        if (directory != null && directory.isDirectory()) {

            if (directory.getName().contains("ylw")) {
                BOAT_COLOR = Color.YELLOW;
            } else if (directory.getName().contains("org")) {
                BOAT_COLOR = Color.ORANGE;
            } else if (directory.getName().contains("grn")) {
                BOAT_COLOR = Color.GREEN;
            } else if (directory.getName().contains("red")) {
                BOAT_COLOR = Color.RED;
            } else if (directory.getName().contains("blu")) {
                BOAT_COLOR = Color.BLUE;
            } else if (directory.getName().contains("blk")) {
                BOAT_COLOR = Color.BLACK;
            } else if (directory.getName().contains("wht")) {
                BOAT_COLOR = Color.WHITE;
            } else {
                BOAT_COLOR = Color.CYAN;
            }

            File[] files = directory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (!name.toLowerCase().endsWith(".txt")) {
                        return false;
                    }
                    if (fileNameInclusions != null) {
                        for (String s : fileNameInclusions) {
                            if (!name.contains(s)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });
            System.out.println("Found " + files.length + " files");
            for (File file : files) {
                loadFile(file.getAbsolutePath(), directory.getName());
            }

            List<Position> positions = directoryNameToPath.get(directory.getName());
            if (positions != null) {
                System.out.println("Have " + positions.size() + " positions for " + directory.getName());
//                System.out.println("Have " + observationCount + " observations");

                if (MIN_POS != -1 && MAX_POS != -1) {
                    System.out.println("\t Removing entries outside of positions " + MIN_POS + " to " + MAX_POS);
                    if (positions.size() > MAX_POS) {
                        positions = positions.subList(MIN_POS, MAX_POS);
                    } else if (positions.size() > MIN_POS) {
                        positions = positions.subList(MIN_POS, positions.size());
                    } else {
                        positions.clear();
                    }
                }

                // Add path
                Polyline polyline = new Polyline(positions);
                polyline.setColor(BOAT_COLOR);
                polyline.setLineWidth(LINE_WIDTH);
                polyline.setFollowTerrain(true);
                annotation.addRenderable(polyline);
            }
        }

        return true;
    }

    public boolean loadFile(String path, String directoryName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd:HH:mm:ss");
        File file = new File(path);
        if (file != null && file.isFile()) {
            Scanner input;
            try {
                input = new Scanner(file);

                System.out.println("Loading " + path);

                int counter = 0;
                LatLon oldLatLon = null;
                LatLon newLatLon = null;
//                UTMCoordinate latestUtm = null;
                double oldEasting = -1, oldNorthing = -1, newEasting = -1, newNorthing = -1;
                ArrayList<Position> positions;
                if (directoryNameToPath.containsKey(directoryName)) {
                    positions = directoryNameToPath.get(directoryName);
                } else {
                    positions = new ArrayList<Position>();
                    directoryNameToPath.put(directoryName, positions);
                }
                int observationCount = 0;

                while (input.hasNext()) {
                    //0 18:19:48,140 POSE: {476608.34, 4671214.4, 172.35, Q[0.0,0.0,0.0]} @ 17North 
                    //449 05:55:47,847 Value{"c":15.151515,"v":0} Namem1 
                    String nextLine = input.nextLine();
                    if (!nextLine.contains("POSE:") && !nextLine.contains("Value{")) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(nextLine, " ");
                    String token;
                    try {
                        st.nextToken();
                        // 10:56:08,107
                        token = st.nextToken();
                        String combined = file.getName().substring(file.getName().indexOf("_") + 1);
                        combined = combined.substring(0, combined.indexOf("_"));
                        combined += ":" + token.substring(0, token.indexOf(","));
                        try {
                            Date date = sdf.parse(combined);
                            if (MIN_DATE != null && MAX_DATE != null) {
//                                System.out.println("Date " + date.toString() + "\t" + MIN_DATE.toString() + "\t" + MAX_DATE.toString());
                                if (date.before(MIN_DATE) || date.after(MAX_DATE)) {
                                    continue;
                                }
                            }
                        } catch (ParseException ex) {
                            Logger.getLogger(DataVisualizer.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        // POSE:
                        token = st.nextToken();
                        if (token.equals("POSE:")) {
//                            399 07:09:57,251 POSE: {476608.34, 4671214.4, 172.35, Q[0.0,0.0,-1.2871662207207546]} @ 17North 
//                            3998 07:10:00,851 POSE: {553157.9033275151, 2804260.1982287127, -15.0, Q[0.0,0.0,-1.340208024615352]} @ 39North 
                            newLatLon = null;

                            // easting
                            token = st.nextToken();
                            String easting = token.substring(1, token.length() - 1);
                            newEasting = Double.parseDouble(easting);
                            // northing
                            token = st.nextToken();
                            String northing = token.substring(0, token.length() - 1);
                            newNorthing = Double.parseDouble(northing);
                            st.nextToken();
                            st.nextToken();
                            st.nextToken();
                            // zone
                            String zone = st.nextToken();

                            if (zone.length() > 2 && Character.isDigit(zone.charAt(1))) {
                                // 2 digit zone
                                int zoneNum = Integer.parseInt(zone.substring(0, 2));
                                if (zoneNum == 17) {
                                    continue;
                                }
                                if (zone.charAt(2) == 'N') {
//                                    latestUtm = new UTMCoordinate(Double.parseDouble(easting), Double.parseDouble(northing), (zoneNum + "") + zone.charAt(2));
                                    newLatLon = UTMCoord.locationFromUTMCoord(
                                            zoneNum,
                                            AVKey.NORTH,
                                            Double.parseDouble(easting),
                                            Double.parseDouble(northing),
                                            null);
                                } else if (zone.charAt(2) == 'S') {
//                                    latestUtm = new UTMCoordinate(Double.parseDouble(easting), Double.parseDouble(northing), (zoneNum + "") + zone.charAt(2));
                                    newLatLon = UTMCoord.locationFromUTMCoord(
                                            zoneNum,
                                            AVKey.SOUTH,
                                            Double.parseDouble(easting),
                                            Double.parseDouble(northing),
                                            null);
                                }
                            } else if (zone.length() > 1 && Character.isDigit(zone.charAt(0))) {
                                // 1 digit zone
                                int zoneNum = Integer.parseInt(zone.substring(0, 1));
                                if (zoneNum == 17) {
                                    continue;
                                }
                                if (zone.charAt(1) == 'N') {
//                                    latestUtm = new UTMCoordinate(Double.parseDouble(easting), Double.parseDouble(northing), (zoneNum + "") + zone.charAt(1));
                                    newLatLon = UTMCoord.locationFromUTMCoord(
                                            zoneNum,
                                            AVKey.NORTH,
                                            Double.parseDouble(easting),
                                            Double.parseDouble(northing),
                                            null);
                                } else if (zone.charAt(1) == 'S') {
//                                    latestUtm = new UTMCoordinate(Double.parseDouble(easting), Double.parseDouble(northing), (zoneNum + "") + zone.charAt(1));
                                    newLatLon = UTMCoord.locationFromUTMCoord(
                                            zoneNum,
                                            AVKey.SOUTH,
                                            Double.parseDouble(easting),
                                            Double.parseDouble(northing),
                                            null);
                                }
                            }

//                            System.out.println("Distance: " + (Math.pow(oldEasting - newEasting, 2) + Math.pow(oldNorthing - newNorthing, 2)));
                            if (VIZ_PATH) {
                                if (newLatLon != null
                                        && (oldLatLon == null
                                        || (Math.pow(oldEasting - newEasting, 2) + Math.pow(oldNorthing - newNorthing, 2) > DIST_STEP_THRESH))) {
                                    positions.add(new Position(newLatLon, 0));
                                    oldLatLon = new LatLon(newLatLon);
                                    oldEasting = newEasting;
                                    oldNorthing = newNorthing;
                                }
                            }
                        } else if (token.startsWith("Value{\"c\"")) {
                            if (VIZ_DATA) {
                                if (newLatLon != null) {
//                                    System.out.println("value " + token);
                                    String value = token.substring(token.indexOf(":") + 1, token.indexOf(","));
                                    Location location = Conversion.latLonToLocation(newLatLon);
                                    Observation observation = new Observation("UNKNOWN", Double.parseDouble(value), "source", location, 0L);
                                    UTMCoord utm = UTMCoord.fromLatLon(newLatLon.latitude, newLatLon.longitude);
                                    data.addObservation(observation, utm);
                                    observationCount++;
                                }
                            }
                        }
                    } catch (NoSuchElementException nsee) {

                    }
                }
                input.close();

//                System.out.println("Have " + positions.size() + " positions");
//                System.out.println("Have " + observationCount + " observations");
//                
//                int min = 400;
//                int max = 600;
//                if(positions.size() > max) {
//                    positions = positions.subList(min, max);                    
//                } else if(positions.size() > min) {
//                    positions = positions.subList(min, positions.size());    
//                } else {
//                    positions.clear();
//                }
//
//                // Add path
//                Polyline polyline = new Polyline(positions);
//                polyline.setColor(BOAT_COLOR);
//                polyline.setLineWidth(LINE_WIDTH);
//                polyline.setFollowTerrain(true);
//                annotation.addRenderable(polyline);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

        } else {
            System.out.println("Could not load file " + path);
        }

        return true;
    }

    public void clearWidgets() {

    }

    public static void main(String[] args) {
        final DataVisualizer viz = new DataVisualizer();
        // SENSOR DATA
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/sensor-data/", null, new String[] {"airboat", "20151211"});

//        // LOCATION DATA
//        viz.loadTeamDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/",  new String[] {"120-blu-tri", "122-wht-crc", "112-red-dmd"}, new String[] {"airboat", "20151219"});
//
        // LOCATION DATA
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/102-ylw-sq", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/104-grn-sq", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/107-blk-sq", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/108-wht-dmd", new String[] {"airboat", "20151219"}); // N;
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/109-ylw-dmd", new String[] {"airboat", "20151219"}); // Y
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/110-org-dmd", new String[] {"airboat", "20151219"}); // ?
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/111-grn-dmd", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/112-red-dmd", new String[] {"airboat", "20151219"}); // N
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/113-blu-dmd", new String[] {"airboat", "20151219"}); // Y
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/114-blk-dmd", new String[] {"airboat", "20151219"}); // N
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/115-wht-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/116-ylw-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/117-org-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/118-grn-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/119-red-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/120-blu-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/121-blk-tri", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/122-wht-crc", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/123-ylw-crc", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/124-org-crc", new String[] {"airboat", "20151219"}); // N
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/125-grn-crc", new String[] {"airboat", "20151219"}); // Y
//
        // Station keeping
        // Min 300, max 500
        MIN_DATE = new Date(2015 - 1900, 12 - 1, 17, 7, 35);
        MAX_DATE = new Date(2015 - 1900, 12 - 1, 17, 7, 45);
//        MIN_DATE = new Date(2015 - 1900, 12 - 1, 17, 7, 35);
//        MAX_DATE = new Date(2015 - 1900, 12 - 1, 17, 7, 55);
//        MIN_DATE = new Date(2015 - 1900, 12 - 1, 17, 10, 00);
//        MAX_DATE = new Date(2015 - 1900, 12 - 1, 17, 10, 30);
//        viz.loadTeamDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/",  null, new String[] {"airboat", "20151217"});
        viz.loadTeamDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/",  new String[] {"104-grn-sq"}, new String[] {"airboat", "20151217"});
//        viz.loadTeamDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/",  new String[] {"102-ylw-sq", "117-org-tri"}, new String[] {"airboat", "20151217"});
//
        // Bullet failure
//        MIN_DATE = new Date(2015 - 1900, 12 - 1, 19, 11, 26);
//        MAX_DATE = new Date(2015 - 1900, 12 - 1, 19, 11, 46);
//        viz.loadTeamDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/", new String[] {"102-ylw-sq"}, new String[]{"airboat", "20151219"});
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/104-grn-sq", new String[] {"airboat", "20151219"}); // N
////        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/102-ylw-sq", new String[] {"airboat", "20151219"}); // N

        // Explore area with compass failure
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/109-ylw-dmd", new String[] {"airboat", "20151219"}); // Y
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/113-blu-dmd", new String[] {"airboat", "20151219"}); // Y
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/115-wht-tri", new String[] {"airboat", "20151219"}); // N
//        viz.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/phone-data-viz/125-grn-crc", new String[] {"airboat", "20151219"}); // Y
// 
        System.out.println("Done loading");
    }
}
