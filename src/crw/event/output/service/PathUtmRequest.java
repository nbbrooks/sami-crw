package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

public class PathUtmRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, Location> proxyEndLocation;

    static {
        fieldNames.add("proxyEndLocation");

        fieldNameToDescription.put("proxyEndLocation", "Destination location?");
    }

    public PathUtmRequest() {
        id = UUID.randomUUID();
    }

    public Hashtable<ProxyInt, Location> getEndLocation() {
        return proxyEndLocation;
    }

    public void setEndLocation(Hashtable<ProxyInt, Location> proxyEndLocation) {
        this.proxyEndLocation = proxyEndLocation;
    }

    public String toString() {
        return "PathUtmRequest [" + proxyEndLocation + "]";
    }
}
