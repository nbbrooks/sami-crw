package crw;

import crw.ui.component.WorldWindPanel;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author nbb
 */
public class CrwHelper {

    private static final Logger LOGGER = Logger.getLogger(CrwHelper.class.getName());

    public static String getUniqueName(String name, ArrayList<String> existingNames) {
        boolean invalidName = existingNames.contains(name);
        while (invalidName) {
            int index = name.length() - 1;
            if ((int) name.charAt(index) < (int) '0' || (int) name.charAt(index) > (int) '9') {
                // name does not end with a number - attach a "2"
                name += "2";
            } else {
                // Find the number the name ends with and increment it
                int numStartIndex = -1, numEndIndex = -1;
                while (index >= 0) {
                    if ((int) name.charAt(index) >= (int) '0' && (int) name.charAt(index) <= (int) '9') {
                        if (numEndIndex == -1) {
                            numEndIndex = index;
                        }
                    } else if (numEndIndex != -1) {
                        numStartIndex = index + 1;
                        break;
                    }
                    index--;
                }
                int number = Integer.parseInt(name.substring(numStartIndex, numEndIndex + 1));
                name = name.substring(0, numStartIndex) + (number + 1);
            }
            invalidName = existingNames.contains(name);
        }
        return name;
    }

    public static boolean isPositionBetween(Position position, Position northWest, Position southEast) {
        if (position == null || northWest == null || southEast == null) {
            LOGGER.severe("NULL position!");
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

    public static double calculateDistance(Globe globe, Position position1, Position position2) {
        if (globe == null) {
            LOGGER.severe("NULL globe!");
            return Double.NaN;
        }
        if (position1 == null || position2 == null) {
            LOGGER.severe("NULL position!");
            return Double.NaN;
        }
        Vec4 p1 = globe.computePointFromPosition(position1);
        Vec4 p2 = globe.computePointFromPosition(position2);
        return Math.abs(p1.distanceTo3(p2));
    }

    public static Position getPositionAsl(Globe globe, Position zeroElevationPosition) {
        // Return click position with elevation m ASL (above sea level)
        double elevationAsl = (globe.getElevation(Angle.fromDegrees(zeroElevationPosition.getLatitude().degrees), Angle.fromDegrees(zeroElevationPosition.getLongitude().degrees)));
        return new Position(zeroElevationPosition.getLatitude(), zeroElevationPosition.getLongitude(), elevationAsl);
    }

    public static boolean positionBetween(Position position, Position northWest, Position southEast) {
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

    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame();
        WorldWindPanel www = new WorldWindPanel();
        frame.getContentPane().add(www.getComponent());
        frame.pack();
        frame.setVisible(true);
        Thread.sleep(10000);
        Position p1 = new Position(Angle.fromDegreesLatitude(40.44330), Angle.fromDegreesLongitude(-79.94082), 0);
        Position p2 = new Position(Angle.fromDegreesLatitude(40.44295), Angle.fromDegreesLongitude(-79.93979), 0);
        System.out.println("" + calculateDistance(www.getCanvas().getView().getGlobe(), p1, p2));
    }
}
