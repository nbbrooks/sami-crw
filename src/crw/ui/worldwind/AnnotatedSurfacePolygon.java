package crw.ui.worldwind;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;

/**
 *
 * @author nbb
 */
public class AnnotatedSurfacePolygon extends SurfacePolygon {

    public AnnotatedSurfacePolygon(Iterable<? extends LatLon> itrbl) {
        super(itrbl);
    }

    public AnnotatedSurfacePolygon(ShapeAttributes sa, Iterable<? extends LatLon> itrbl) {
        super(sa, itrbl);
    }
}
