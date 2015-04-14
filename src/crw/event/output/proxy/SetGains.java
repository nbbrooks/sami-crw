package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class SetGains extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public double thrustP;
    public double thrustI;
    public double thrustD;
    public double rudderP;
    public double rudderI;
    public double rudderD;

    static {
        fieldNames.add("thrustP");
        fieldNames.add("thrustI");
        fieldNames.add("thrustD");
        fieldNames.add("rudderP");
        fieldNames.add("rudderI");
        fieldNames.add("rudderD");

        fieldNameToDescription.put("thrustP", "Thurst P?");
        fieldNameToDescription.put("thrustI", "Thurst I?");
        fieldNameToDescription.put("thrustD", "Thurst D?");
        fieldNameToDescription.put("rudderP", "Rudder P?");
        fieldNameToDescription.put("rudderI", "Rudder I?");
        fieldNameToDescription.put("rudderD", "Rudder D?");
    }

    public SetGains() {
        id = UUID.randomUUID();
    }

    public SetGains(UUID uuid, UUID missionUuid, double thrustP, double thrustI, double thrustD, double rudderP, double rudderI, double rudderD) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.thrustP = thrustP;
        this.thrustI = thrustI;
        this.thrustD = thrustD;
        this.rudderP = rudderP;
        this.rudderI = rudderI;
        this.rudderD = rudderD;
    }

    public String toString() {
        return "SetGains [" + thrustP + ", " + thrustI + ", " + thrustD + ", " + rudderP + ", " + rudderI + ", " + rudderD + "]";
    }
}
