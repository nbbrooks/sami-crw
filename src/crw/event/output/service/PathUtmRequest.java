package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;

public class PathUtmRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Location endLocation;

    static {
        fieldNames.add("endLocation");

        fieldNameToDescription.put("endLocation", "Destination location?");
    }

    public PathUtmRequest() {
        id = UUID.randomUUID();
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }

    public String toString() {
        return "PathUtmRequest: endLocation = " + endLocation;
    }
}
