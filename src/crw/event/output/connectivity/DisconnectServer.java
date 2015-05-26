package crw.event.output.connectivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class DisconnectServer extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public DisconnectServer() {
    }

    public DisconnectServer(UUID uuid, UUID missionUuid) {
        this.id = uuid;
        this.missionId = missionUuid;
    }

    public String toString() {
        return "DisconnectServer";
    }
}
