package crw.event.output.operator;

import sami.event.OutputEvent;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    List<BoatProxy> options;

    public OperatorSelectBoat() {
        id = UUID.randomUUID();
    }

    public List<BoatProxy> getOptions() {
        return options;
    }

    public void setOptions(List<BoatProxy> options) {
        this.options = options;
    }

    public String toString() {
        return "OperatorPathOptions [" + options + "]";
    }

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }
}
