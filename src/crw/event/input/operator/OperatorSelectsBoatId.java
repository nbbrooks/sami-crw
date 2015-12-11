package crw.event.input.operator;

import crw.event.output.proxy.BoatProxyId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;

/**
 *
 * @author nbb
 */
public class OperatorSelectsBoatId extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public BoatProxyId selectedId;

    static {
        variableNames.add("selectedId");

        variableNameToDescription.put("selectedId", "Accepted boat ID.");
    }

    public OperatorSelectsBoatId() {
        id = UUID.randomUUID();
    }

    public OperatorSelectsBoatId(UUID relevantOutputEventUuid, UUID missionUuid, BoatProxyId selectedId) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.selectedId = selectedId;
        id = UUID.randomUUID();
    }
}
