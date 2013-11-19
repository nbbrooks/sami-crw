package crw.event.input.operator;

import sami.event.InputEvent;
import sami.proxy.ProxyInt;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorSelectsBoatList extends InputEvent {

    public OperatorSelectsBoatList() {
        id = UUID.randomUUID();
    }

    public OperatorSelectsBoatList(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<BoatProxy> boatProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        if (boatProxyList != null) {
            relevantProxyList = new ArrayList<ProxyInt>();
            for (BoatProxy boatProxy : boatProxyList) {
                relevantProxyList.add(boatProxy);
            }
        } else {
            relevantProxyList = null;
        }
    }
}
