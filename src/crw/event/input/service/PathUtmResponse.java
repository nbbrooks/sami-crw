package crw.event.input.service;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.BlockingInputEvent;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class PathUtmResponse extends BlockingInputEvent {

    public ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths = null;

    public PathUtmResponse() {
    }

    public PathUtmResponse(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.proxyPaths = proxyPaths;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }

    public ArrayList<Hashtable<ProxyInt, PathUtm>> getPaths() {
        return proxyPaths;
    }

    public void setPaths(ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPaths) {
        this.proxyPaths = proxyPaths;
    }

    @Override
    public PathUtmResponse copyForProxyTrigger() {
        PathUtmResponse copy = new PathUtmResponse();
        copy.setGeneratorEvent(getGeneratorEvent());
        copy.setVariables(getVariables());
        return copy;
    }
}
