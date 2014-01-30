package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class ProxyStationKeep extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Location point;
    public double radius;

    static {
        fieldNames.add("point");
        fieldNames.add("minRadius");

        fieldNameToDescription.put("point", "Location to station keep around?");
        fieldNameToDescription.put("radius", "Distance from point robot should stay within?");
    }

    public ProxyStationKeep() {
    }
}
