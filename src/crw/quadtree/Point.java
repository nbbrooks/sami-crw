package crw.quadtree;

import gov.nasa.worldwind.geom.coords.UTMCoord;

/**
 * 
 * @author varunpant (https://github.com/varunpant/Quadtree/)
 */
public class Point implements Comparable {

    private UTMCoord utmCoord;
    private double value;

    /**
     * Creates a new point object.
     *
     * @param {double} x The x-coordinate of the point.
     * @param {double} y The y-coordinate of the point.
     * @param {Object} opt_value Optional value associated with the point.     
     */
    public Point(UTMCoord utmCoord, double value) {
        this.utmCoord = utmCoord;
        this.value = value;
    }

    public double getX() {
        return utmCoord.getEasting();
    }

    public double getY() {
        return utmCoord.getNorthing();
    }
    
    public UTMCoord getUtmCoord() {
        return utmCoord;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double opt_value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "[" + utmCoord.toString() + ", " + value + "]";
    }

    @Override
    public int compareTo(Object o) {
        Point tmp = (Point) o;
        if (this.utmCoord.getEasting() < tmp.utmCoord.getEasting()) {
            return -1;
        } else if (this.utmCoord.getEasting() > tmp.utmCoord.getEasting()) {
            return 1;
        } else {
            if (this.utmCoord.getNorthing() < tmp.utmCoord.getNorthing()) {
                return -1;
            } else if (this.utmCoord.getNorthing() > tmp.utmCoord.getNorthing()) {
                return 1;
            }
            return 0;
        }

    }

}
