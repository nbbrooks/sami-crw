package crw.event.output.proxy.single;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.SimpleLatLon;

public class SingleProxyGotoLatLon extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public SimpleLatLon latLon;

    static {
        fieldNames.add("latLon");

        fieldNameToDescription.put("latLon", "Latitude/Longitude for the proxies to go to?");
    }

    public SingleProxyGotoLatLon() {
        id = UUID.randomUUID();
    }

    public SingleProxyGotoLatLon(UUID uuid, UUID missionUuid, SimpleLatLon latLon) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.latLon = latLon;
    }

    public SimpleLatLon getLatLon() {
        return latLon;
    }

    public void setLatLon(SimpleLatLon latLon) {
        this.latLon = latLon;
    }

    public String toString() {
        return "SingleProxyGotoLatLon [" + latLon + "]";
    }
}
