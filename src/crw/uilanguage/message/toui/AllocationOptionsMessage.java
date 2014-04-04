package crw.uilanguage.message.toui;

import sami.uilanguage.toui.SelectionMessage;
import sami.allocation.ResourceAllocation;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AllocationOptionsMessage extends SelectionMessage {

    public AllocationOptionsMessage(UUID relevantOutputEventId, UUID missionId, int priority, List<ResourceAllocation> optionsList) {
        super(relevantOutputEventId, missionId, priority, false, true, true, optionsList);
    }
}
