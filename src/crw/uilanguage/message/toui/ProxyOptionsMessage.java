package crw.uilanguage.message.toui;

import sami.proxy.ProxyInt;
import sami.uilanguage.toui.SelectionMessage;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class ProxyOptionsMessage extends SelectionMessage {

    public ProxyOptionsMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, List<ProxyInt> optionsList) {
        super(relevantOutputEventId, missionId, priority, allowMultiple, false, true, optionsList);
    }
}
