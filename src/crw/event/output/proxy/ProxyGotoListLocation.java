package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

/**
 * Goto location x in a list of locations for a proxy.
 *
 * @author nbb
 */
public class ProxyGotoListLocation extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, ArrayList<Location>> proxyLocations;
    
    // Miscellaneous
    // Filled out by ProxyEventHandler, the position (starting at 1, not 0) in the list to grab a location from
    transient public int position;

    static {
        fieldNames.add("proxyLocations");

        fieldNameToDescription.put("proxyLocations", "Locations for the proxies to execute?");
    }

    public ProxyGotoListLocation() {
        id = UUID.randomUUID();
    }

    public ProxyGotoListLocation(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, ArrayList<Location>> proxyLocations) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyLocations = proxyLocations;
    }

    public ProxyGotoListLocation clone() {
        ProxyGotoListLocation clone = new ProxyGotoListLocation(id, missionId, proxyLocations);
        clone.position = position;
        return clone;
    }

    public Hashtable<ProxyInt, ArrayList<Location>> getProxyLocations() {
        return proxyLocations;
    }

    public void setProxyLocations(Hashtable<ProxyInt, ArrayList<Location>> proxyLocations) {
        this.proxyLocations = proxyLocations;
    }

    public String toString() {
        return "ProxyGotoListLocation [" + proxyLocations + "]";
    }
}
