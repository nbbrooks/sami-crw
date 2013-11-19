package crw.event.input.operator;

import java.util.Hashtable;
import java.util.UUID;
import sami.event.InputEvent;
import sami.path.Path;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class OperatorAcceptsPath extends InputEvent {

    public Hashtable<ProxyInt, Path> acceptedProxyPaths;

    public OperatorAcceptsPath() {
        id = UUID.randomUUID();
    }

    public OperatorAcceptsPath(UUID relevantOutputEventUuid, UUID missionUuid, Hashtable<ProxyInt, Path> acceptedProxyPaths) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.acceptedProxyPaths = acceptedProxyPaths;
        id = UUID.randomUUID();
    }
}
