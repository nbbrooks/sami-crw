package crw.ui.worldwind;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;

/**
 *
 * @author nbb
 */
public class AnnotatedMarker extends BasicMarker {

    public AnnotatedMarker(Position pstn, MarkerAttributes ma) {
        super(pstn, ma);
    }

    public AnnotatedMarker(Position pstn, MarkerAttributes ma, Angle angle) {
        super(pstn, ma, angle);
    }

}
