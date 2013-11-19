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

    public ProxyOptionsMessage(UUID relevantOutputEventId, UUID missionId, int priority, List<ProxyInt> optionsList, boolean allowMultiple) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.priority = priority;
        this.optionsList = optionsList;
        this.allowMultiple = allowMultiple;
        allowRejection = false;
        showOptionsIndividually = true;
    }
}
