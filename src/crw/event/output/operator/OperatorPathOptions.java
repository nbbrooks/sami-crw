package crw.event.output.operator;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Path;
import sami.proxy.ProxyInt;

public class OperatorPathOptions extends OutputEvent {

    public List<Hashtable<ProxyInt, Path>> options;

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
}
