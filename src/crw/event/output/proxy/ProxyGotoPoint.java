package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

public class ProxyGotoPoint extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, Location> proxyPoints;

    static {
        fieldNames.add("proxyPoints");

        fieldNameToDescription.put("proxyPoints", "Points for the proxies to go to?");
    }

    public ProxyGotoPoint() {
        id = UUID.randomUUID();
    }

    public ProxyGotoPoint(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, Location> proxyPoints) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyPoints = proxyPoints;
    }

    public Hashtable<ProxyInt, Location> getProxyPoints() {
        return proxyPoints;
    }

    public void setProxyPoints(Hashtable<ProxyInt, Location> proxyPoints) {
        this.proxyPoints = proxyPoints;
    }
    
    public String toString() {
        return "ProxyGotoPoint [" + proxyPoints + "]";
    }
}
