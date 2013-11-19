package crw.event.input.operator;

import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class OperatorSelectsBoat extends InputEvent {

    public OperatorSelectsBoat() {
        id = UUID.randomUUID();
    }

    public OperatorSelectsBoat(UUID relevantOutputEventUuid, UUID missionUuid, BoatProxy boatProxy) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        if (boatProxy != null) {
            relevantProxyList = new ArrayList<ProxyInt>();
            relevantProxyList.add(boatProxy);
        } else {
            relevantProxyList = null;
        }
    }
}
