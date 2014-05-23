package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.InputEvent;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class PathUtmResponse extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths = null;

    static {
        variableNames.add("proxyPaths");

        variableNameToDescription.put("proxyPaths", "Returned path options.");
    }

    public PathUtmResponse() {
    }

    public PathUtmResponse(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.proxyPaths = proxyPaths;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }

    public ArrayList<Hashtable<ProxyInt, PathUtm>> getPaths() {
        return proxyPaths;
    }

    public void setPaths(ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths) {
        this.proxyPaths = proxyPaths;
    }

    public String toString() {
        return "PathUtmResponse [" + proxyPaths + "]";
    }
}
