package crw;

import crw.ui.component.WorldWindPanel;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import java.awt.Color;
import java.util.logging.Logger;
import javax.swing.JFrame;
import sami.CoreHelper;

/**
 *
 * @author nbb
 */
public class CrwHelper {

    private static final Logger LOGGER = Logger.getLogger(CrwHelper.class.getName());

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

    public static Color randomColor() {
        float r = CoreHelper.RANDOM.nextFloat();
        float g = CoreHelper.RANDOM.nextFloat();
        float b = CoreHelper.RANDOM.nextFloat();

        return new Color(r, g, b);
    }

    public static String colorToHtmlColor(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    public static String padLeftHtml(String value, int padding) {
        return String.format("%-" + padding + "s", value).replaceAll(" ", "&nbsp;");
    }

    public static String padRightHtml(String value, int padding) {
        return String.format("%" + padding + "s", value).replaceAll(" ", "&nbsp;");
    }

    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame();
        WorldWindPanel www = new WorldWindPanel();
        www.createMap();
        frame.getContentPane().add(www.getComponent());
        frame.pack();
        frame.setVisible(true);
        Thread.sleep(10000);
        Position p1 = new Position(Angle.fromDegreesLatitude(40.44330), Angle.fromDegreesLongitude(-79.94082), 0);
        Position p2 = new Position(Angle.fromDegreesLatitude(40.44295), Angle.fromDegreesLongitude(-79.93979), 0);
        System.out.println("" + calculateDistance(www.getCanvas().getView().getGlobe(), p1, p2));
    }
}
