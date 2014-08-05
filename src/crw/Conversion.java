package crw;

import sami.path.Location;
import sami.path.UTMCoordinate;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.util.ArrayList;

/**
 *
 * @author nbb
 */
public class Conversion {

    public static Location latLonToLocation(LatLon latLon) {
        return new Location(latLon.latitude.degrees, latLon.longitude.degrees, 0);
    }

    public static Location positionToLocation(Position position) {
        return new Location(position.latitude.degrees, position.longitude.degrees, position.getAltitude());
    }
    
    public static ArrayList<Location> positionToLocation(ArrayList<Position> positions){
        ArrayList<Location> ret = new ArrayList<Location>();
        
        for(Position p : positions){
            ret.add(new Location(p.latitude.degrees, p.longitude.degrees, p.getAltitude()));
        }
        
        return ret;
    }
    
   public static ArrayList<Position> locationToPosition(ArrayList<Location> positions){
        ArrayList<Position> ret = new ArrayList<Position>();
        
        for(Location p : positions){
            UTMCoordinate utmCoordinate = p.getCoordinate();
        ret.add( new Position(
                UTMCoord.locationFromUTMCoord(
                Integer.parseInt(utmCoordinate.getZone().substring(0, utmCoordinate.getZone().length() - 1)),
                (utmCoordinate.getHemisphere().equals(UTMCoordinate.Hemisphere.NORTH) ? AVKey.NORTH : AVKey.SOUTH),
                utmCoordinate.getEasting(),
                utmCoordinate.getNorthing(),
                null),
                p.getAltitude()));
        }
        
        return ret;
    }

    public static Position locationToPosition(Location location) {
        UTMCoordinate utmCoordinate = location.getCoordinate();
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
