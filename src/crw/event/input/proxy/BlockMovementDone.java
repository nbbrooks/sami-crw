package crw.event.input.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 * Used to indicate BlockMovement has been terminated
 *  
 * @author nbb
 */
public class BlockMovementDone extends InputEvent {

    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public BlockMovementDone() {
        id = UUID.randomUUID();
    }

    public BlockMovementDone(UUID relevantOutputEventUuid, UUID missionUuid, ProxyInt proxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
    }

    @Override
    public String toString() {
        return "BlockMovementDone [" + (relevantProxyList != null ? relevantProxyList.toString() : "null") + "]";
    }
}
