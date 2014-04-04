package crw.uilanguage.message.toui;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import sami.path.Path;
import sami.proxy.ProxyInt;
import sami.uilanguage.toui.SelectionMessage;

/**
 *
 * @author nbb
 */
public class PathOptionsMessage extends SelectionMessage {

    public PathOptionsMessage(UUID relevantOutputEventId, UUID missionId, int priority, List<Hashtable<ProxyInt, Path>> optionsList) {
        super(relevantOutputEventId, missionId, priority, false, true, true, optionsList);
    }

    public List<Hashtable<ProxyInt, Path>> getOptions() {
        return (List<Hashtable<ProxyInt, Path>>) optionsList;
    }
}
