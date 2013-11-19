package crw.event.output.operator;

import sami.event.OutputEvent;
import crw.proxy.BoatProxy;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorSelectBoat extends OutputEvent {


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

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }
}
