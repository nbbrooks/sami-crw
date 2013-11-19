package crw.event.input.operator;

import sami.event.InputEvent;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorRejectsPath extends InputEvent {
    // Could store the rejected paths here for the next iteration of path planning

    public OperatorRejectsPath() {
        id = UUID.randomUUID();
    }

    public OperatorRejectsPath(UUID relevantOutputEventUuid, UUID missionUuid) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
    }
}
