package crw.event.output.proxy;

import sami.event.OutputEvent;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class ProxyStationKeep extends OutputEvent {

    public Location point;
    public double minRadius;
    public double maxRadius;

    public ProxyStationKeep() {
    }
}
