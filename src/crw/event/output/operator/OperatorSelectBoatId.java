package crw.event.output.operator;

import crw.event.output.proxy.BoatProxyId;
import sami.event.OutputEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorSelectBoatId extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public List<BoatProxyId> options;

//    static {
//        fieldNames.add("options");
//
//        fieldNameToDescription.put("options", "Boat ID options to show to operator?");
//    }

    public OperatorSelectBoatId() {
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "OperatorSelectBoatId";
    }
}
