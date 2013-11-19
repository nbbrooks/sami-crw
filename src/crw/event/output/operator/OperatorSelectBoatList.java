package crw.event.output.operator;

import sami.event.OutputEvent;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorSelectBoatList extends OutputEvent {

    public ArrayList<BoatProxy> options;

    public OperatorSelectBoatList() {
        id = UUID.randomUUID();
    }

    public List<BoatProxy> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<BoatProxy> options) {
        this.options = options;
    }

    //@todo ugly
    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }
}
