package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class QuantityGreater extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public QuantityGreater() {
        id = UUID.randomUUID();
    }

    public QuantityGreater(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
}
