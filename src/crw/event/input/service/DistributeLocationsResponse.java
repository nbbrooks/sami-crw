package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.InputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class DistributeLocationsResponse extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public Hashtable<ProxyInt, ArrayList<Location>> proxyLocations = null;

    static {
        variableNames.add("proxyLocations");

        variableNameToDescription.put("proxyLocations", "Returned lists of distributed locations.");
    }

    public DistributeLocationsResponse() {
    }

    public DistributeLocationsResponse(UUID relevantOutputEventUuid, UUID missionUuid, Hashtable<ProxyInt, ArrayList<Location>> proxyLocations, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.proxyLocations = proxyLocations;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
}
