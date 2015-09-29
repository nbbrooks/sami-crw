package crw;

import sami.path.Location;
import sami.path.UTMCoordinate;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class Conversion {
    private final static Logger LOGGER = Logger.getLogger(Conversion.class.getName());

    public static Location latLonToLocation(LatLon latLon) {
        if(latLon == null) {
            LOGGER.warning("NULL LatLon in Conversion.latLonToLocation");
            return null;
        }
        return new Location(latLon.latitude.degrees, latLon.longitude.degrees, 0);
    }

    public static Location positionToLocation(Position position) {
        if(position == null) {
            LOGGER.warning("NULL Position in Conversion.latLonToLocation");
            return null;
        }
        return new Location(position.latitude.degrees, position.longitude.degrees, position.getAltitude());
    }

    public static Position locationToPosition(Location location) {
        UTMCoordinate utmCoordinate = location.getCoordinate();
        if(utmCoordinate == null) {
            LOGGER.warning("NULL UTMCoordinate in Conversion.locationToPosition");
            return null;
        }
        return new Position(
                UTMCoord.locationFromUTMCoord(
                        Integer.parseInt(utmCoordinate.getZone().substring(0, utmCoordinate.getZone().length() - 1)),
                        (utmCoordinate.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH) ? AVKey.NORTH : AVKey.SOUTH),
                        utmCoordinate.getEasting(),
                        utmCoordinate.getNorthing(),
                        null),
                location.getAltitude());
    }

    public static Position utmToPosition(UTMCoordinate utmCoordinate, double altitude) {
        if(utmCoordinate == null) {
            LOGGER.warning("NULL UTMCoordinate in Conversion.utmToPosition");
            return null;
        }
        return new Position(
                UTMCoord.locationFromUTMCoord(
                        Integer.parseInt(utmCoordinate.getZone().substring(0, utmCoordinate.getZone().length() - 1)),
                        (utmCoordinate.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH) ? AVKey.NORTH : AVKey.SOUTH),
                        utmCoordinate.getEasting(),
                        utmCoordinate.getNorthing(),
                        null),
                altitude);
    }

    // Linearly scale a value from one value range to another
    public static double convertRange(double valueIn, double minIn, double maxIn, double minOut, double maxOut) {
        return (valueIn - minIn) / (maxIn - minIn) * (maxOut - minOut) + minOut;
    }
}
