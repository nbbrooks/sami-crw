package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class ProxyResendWaypoints extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public ProxyResendWaypoints() {
        id = UUID.randomUUID();
    }

    public ProxyResendWaypoints(UUID uuid, UUID missionUuid) {
        id = UUID.randomUUID();
        this.id = uuid;
        this.missionId = missionUuid;
    }
    
    public String toString() {
        return "ProxyResendWaypoints";
    }
}
