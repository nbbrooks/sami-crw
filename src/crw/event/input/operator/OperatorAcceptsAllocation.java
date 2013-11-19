package crw.event.input.operator;

import sami.event.InputEvent;
import sami.allocation.ResourceAllocation;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorAcceptsAllocation extends InputEvent {

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
