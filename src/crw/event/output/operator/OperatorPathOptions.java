package crw.event.output.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Path;
import sami.proxy.ProxyInt;

public class OperatorPathOptions extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public List<Hashtable<ProxyInt, Path>> options;

    static {
        fieldNames.add("options");

        fieldNameToDescription.put("options", "Path options to show to operator?");
    }

    public OperatorPathOptions() {
        id = UUID.randomUUID();
    }

    public List<Hashtable<ProxyInt, Path>> getOptions() {
        return options;
    }

    public void setOptions(List<Hashtable<ProxyInt, Path>> options) {
        this.options = options;
    }

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }

    public String toString() {
        return "OperatorPathOptions [" + options + "]";
    }

    public static void main(String[] args) {

    }
}
