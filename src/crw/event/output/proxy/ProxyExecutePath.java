package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Path;
import sami.proxy.ProxyInt;

public class ProxyExecutePath extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, Path> proxyPaths;

    static {
        fieldNames.add("proxyPaths");

        fieldNameToDescription.put("proxyPaths", "Paths for the proxies to execute?");
    }

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

    public String toString() {
        return "ProxyEmergencyAbort [" + proxyPaths + "]";
    }
}
