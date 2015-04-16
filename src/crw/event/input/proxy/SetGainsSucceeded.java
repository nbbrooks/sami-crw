package crw.event.input.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class SetGainsSucceeded extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    static {
        fieldNames.add("blocking");

        fieldNameToDescription.put("blocking", "Wait for all proxies to finish events before moving any?");
    }

    public SetGainsSucceeded() {
    }

    public SetGainsSucceeded(UUID relevantOutputEventUuid, UUID missionUuid, ProxyInt proxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
    }

    public SetGainsSucceeded(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "SetGainsSucceeded [" + (relevantProxyList != null ? relevantProxyList.toString() : "null") + ", " + blocking + "]";
    }
}
