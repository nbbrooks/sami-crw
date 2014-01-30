package crw.event.input.proxy;

import sami.event.BlockingInputEvent;
import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ProxyPathCompleted extends BlockingInputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public ProxyPathCompleted() {
        id = UUID.randomUUID();
    }

    public ProxyPathCompleted(UUID relevantOutputEventUuid, UUID missionUuid, ProxyInt proxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
    }
}
