package crw.event.input.operator;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.InputEvent;
import sami.allocation.ResourceAllocation;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorAcceptsAllocation extends InputEvent {
    
    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public OperatorAcceptsAllocation() {
        id = UUID.randomUUID();
    }

    public OperatorAcceptsAllocation(UUID relevantOutputEventUuid, UUID missionUuid, ResourceAllocation allocation) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId= missionUuid;
        this.allocation = allocation;
        id = UUID.randomUUID();
    }
}
