package crw.event.input.operator;

import java.util.ArrayList;
import java.util.HashMap;
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

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public Hashtable<ProxyInt, Path> acceptedProxyPaths;

    static {
        variableNames.add("acceptedProxyPaths");

        variableNameToDescription.put("acceptedProxyPaths", "Accepted set of paths.");
    }

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
