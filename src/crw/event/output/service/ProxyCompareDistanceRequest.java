package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyCompareDistanceRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, Location> proxyCompareLocation;
    public double compareDistance;

    static {
        fieldNames.add("proxyCompareLocation");
        fieldNames.add("compareDistance");

        fieldNameToDescription.put("proxyCompareLocation", "Location to compute distance to?");
        fieldNameToDescription.put("compareDistance", "Distance to compare to?");
    }

    public ProxyCompareDistanceRequest() {
        id = UUID.randomUUID();
    }

    public ProxyCompareDistanceRequest(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, Location> proxyCompareLocation, double compareDistance) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyCompareLocation = proxyCompareLocation;
        this.compareDistance = compareDistance;
    }

    public Hashtable<ProxyInt, Location> getProxyCompareLocation() {
        return proxyCompareLocation;
    }

    public void setProxyCompareLocation(Hashtable<ProxyInt, Location> proxyCompareLocation) {
        this.proxyCompareLocation = proxyCompareLocation;
    }

    public double getCompareDistance() {
        return compareDistance;
    }

    public void setCompareDistance(double compareDistance) {
        this.compareDistance = compareDistance;
    }

    public String toString() {
        return "ProxyCompareDistanceRequest [" + proxyCompareLocation + ", " + compareDistance + "]";
    }
}
