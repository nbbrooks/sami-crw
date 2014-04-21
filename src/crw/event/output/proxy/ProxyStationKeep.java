package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.path.Path;
import sami.proxy.ProxyInt;

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
    public Hashtable<ProxyInt, Location> proxyPoints;
    public double threshold;

    static {
        fieldNames.add("proxyPoints");
        fieldNames.add("threshold");

        fieldNameToDescription.put("proxyPoints", "Location for the proxies to station keep around?");
        fieldNameToDescription.put("threshold", "Distance from point robot should stay within?");
    }

    public ProxyStationKeep() {
    }

    public ProxyStationKeep(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, Location> proxyPoints, double threshold) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyPoints = proxyPoints;
        this.threshold = threshold;

    }

    public Hashtable<ProxyInt, Location> getProxyPoints() {
        return proxyPoints;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setProxyPoints(Hashtable<ProxyInt, Location> proxyPoints) {
        this.proxyPoints = proxyPoints;
    }

    public String toString() {
        return "ProxyStationKeep [" + proxyPoints + ", " + threshold + "]";
    }
}
