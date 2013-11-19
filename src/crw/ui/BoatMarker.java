package crw.ui;

import crw.proxy.BoatProxy;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;

/**
 *
 * @author pscerri
 */
public class BoatMarker extends BasicMarker {

    private final BoatProxy proxy;
    private Angle heading = Angle.ZERO;

    public BoatMarker(BoatProxy proxy, Position p, BasicMarkerAttributes attr) {
        super(p, attr);
        this.proxy = proxy;
        setHeading(heading);
    }

    public BoatProxy getProxy() {
        return proxy;
    }
}
