package crw.event.input.operator;

import sami.event.InputEvent;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorRejectsAllocation extends InputEvent {
    // Could store the rejected allocations here for the next iteration of resource allocation    

    public OperatorRejectsAllocation() {
        id = UUID.randomUUID();
    }

    public OperatorRejectsAllocation(UUID relevantOutputEventUuid, UUID missionUuid) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
    }
}
