package crw.event.output.proxy;

import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Path;
import sami.proxy.ProxyInt;

public class ProxyExecutePath extends OutputEvent {

    public Hashtable<ProxyInt, Path> proxyPaths;

    public ProxyExecutePath() {
        id = UUID.randomUUID();
    }

    public ProxyExecutePath(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, Path> proxyPaths) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyPaths = proxyPaths;
    }

    public Hashtable<ProxyInt, Path> getProxyPaths() {
        return proxyPaths;
    }

    public void setProxyPaths(Hashtable<ProxyInt, Path> proxyPaths) {
        this.proxyPaths = proxyPaths;
    }
}
