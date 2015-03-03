
package crw.ui.worldwind;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Polyline;

/**
 *
 * @author nbb
 */
public class AnnotatedPolyline extends Polyline {

    public AnnotatedPolyline(Iterable<? extends Position> itrbl) {
        super(itrbl);
    }
}
