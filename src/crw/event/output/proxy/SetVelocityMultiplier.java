package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class SetVelocityMultiplier extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public double velocityMultiplier;

    static {
        fieldNames.add("velocityMultiplier");

        fieldNameToDescription.put("velocityMultiplier", "Fraction of max speed to move at in autonomous mode?");
    }

    public SetVelocityMultiplier() {
        id = UUID.randomUUID();
    }

    public SetVelocityMultiplier(UUID uuid, UUID missionUuid, double velocityMultiplier) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.velocityMultiplier = velocityMultiplier;
    }

    public String toString() {
        return "SetVelocityMultiplier [" + velocityMultiplier + "]";
    }
}
