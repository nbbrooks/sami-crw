package crw.event.output.service;

import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;

public class PathUtmRequest extends OutputEvent {

//    HashMap<String, Object> params = new HashMap<String, Object>();
    public Location endLocation;

    public PathUtmRequest() {
        id = UUID.randomUUID();
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }
    
    public String toString() {
        return "PathUtmRequest: endLocation = " + endLocation;
    }
}
