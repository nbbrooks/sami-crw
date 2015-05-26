package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 * Used in a plan to prevent output events in other plans from moving a proxy
 * while the owner plan is still executing (for example, for use by a station
 * keeping sub-mission)
 *
 * @author nbb
 */
public class BlockMovement extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public BlockMovement() {
        id = UUID.randomUUID();
    }

    public BlockMovement(UUID missionUuid) {
        this.missionId = missionUuid;
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "BlockMovement";
    }
}
