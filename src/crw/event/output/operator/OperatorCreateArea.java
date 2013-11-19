package crw.event.output.operator;

import sami.event.OutputEvent;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorCreateArea extends OutputEvent {

    public OperatorCreateArea() {
        id = UUID.randomUUID();
    }

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }
}
