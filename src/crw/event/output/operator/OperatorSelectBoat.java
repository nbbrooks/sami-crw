package crw.event.output.operator;

import sami.event.OutputEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorSelectBoat extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields

    public OperatorSelectBoat() {
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "OperatorSelectBoat";
    }
}
