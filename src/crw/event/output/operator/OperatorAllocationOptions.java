package crw.event.output.operator;

import java.util.ArrayList;
import java.util.UUID;
import sami.allocation.ResourceAllocation;
import sami.event.OutputEvent;

public class OperatorAllocationOptions extends OutputEvent {

    public ArrayList<ResourceAllocation> options = null;

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
}
