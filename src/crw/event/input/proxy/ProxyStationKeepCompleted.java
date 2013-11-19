package crw.event.input.proxy;

import java.util.ArrayList;
import java.util.UUID;
import sami.event.BlockingInputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyStationKeepCompleted extends BlockingInputEvent {

    public ProxyStationKeepCompleted() {
    }

    public ProxyStationKeepCompleted(UUID relevantOutputEventUuid, UUID missionUuid, ProxyInt proxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
    }
}
