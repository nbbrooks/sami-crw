package crw.uilanguage.message.toui;

import crw.event.output.proxy.BoatProxyId;
import sami.uilanguage.toui.SelectionMessage;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class BoatIdOptionsMessage extends SelectionMessage {

    public BoatIdOptionsMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, List<BoatProxyId> optionsList) {
        super(relevantOutputEventId, missionId, priority, allowMultiple, false, true, optionsList);
    }
}
