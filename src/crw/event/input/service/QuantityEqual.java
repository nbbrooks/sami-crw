package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class QuantityEqual extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public QuantityEqual() {
        id = UUID.randomUUID();
    }

    public QuantityEqual(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
}
