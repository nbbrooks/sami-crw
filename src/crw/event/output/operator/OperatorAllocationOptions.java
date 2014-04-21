package crw.event.output.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.allocation.ResourceAllocation;
import sami.event.OutputEvent;

public class OperatorAllocationOptions extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public ArrayList<ResourceAllocation> options = null;

    static {
        fieldNames.add("options");

        fieldNameToDescription.put("options", "Resource allocation options to show to operator?");
    }

    public OperatorAllocationOptions() {
        id = UUID.randomUUID();
    }

    public ArrayList<ResourceAllocation> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<ResourceAllocation> options) {
        this.options = options;
    }

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }

    public String toString() {
        return "OperatorAllocationOptions [" + options + "]";
    }
}
