package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import java.util.UUID;
import sami.path.PathUtm;

/**
 * Receives a list of locations and distributes them amongst a number of proxies.
 * 
 * @author nbb
 */
public class DistributeLocationsRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
//    public ArrayList<Location> locations;
    // PathUtm GUI widget works better for this request than ArrayList<Location>'s
    public PathUtm locations;

    static {
        fieldNames.add("locations");

        fieldNameToDescription.put("locations", "Locations to ditribute?");
    }

    public DistributeLocationsRequest() {
        id = UUID.randomUUID();
    }

    public DistributeLocationsRequest(UUID uuid, UUID missionUuid, PathUtm locations) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.locations = locations;
    }

    public PathUtm getLocations() {
        return locations;
    }

    public void setLocations(PathUtm locations) {
        this.locations = locations;
    }

    public String toString() {
        return "DistributeLocationsRequest [" + locations + "]";
    }
}
