package crw.event.input.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.area.Area2D;
import sami.event.InputEvent;

/**
 *
 * @author nbb
 */
public class OperatorCreatedArea extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public Area2D area;

    static {
        variableNames.add("area");

        variableNameToDescription.put("area", "Drawn area.");
    }

    public OperatorCreatedArea() {
        id = UUID.randomUUID();
    }

    public OperatorCreatedArea(UUID relevantOutputEventUuid, UUID missionUuid, Area2D area) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        this.area = area;
    }

    @Override
    public String toString() {
        return "OperatorCreatedArea [" + area + "]";
    }
}
