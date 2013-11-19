package crw.event.input.proxy;

import sami.event.BlockingInputEvent;
import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.UUID;

public class ProxyPathCompleted extends BlockingInputEvent {
    
    public ProxyPathCompleted() {
        id = UUID.randomUUID();
    }
    
    public ProxyPathCompleted(UUID relevantOutputEventUuid, UUID missionUuid, ProxyInt proxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
    }
}
