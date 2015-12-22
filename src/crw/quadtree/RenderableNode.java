package crw.quadtree;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceQuad;
import java.awt.Color;
import sami.path.UTMCoordinate;

/**
 * Adapted from https://github.com/varunpant/Quadtree/
 * 
 * @author varunpant (https://github.com/varunpant/Quadtree/), nbb
 */
public class RenderableNode {

    private double x;
    private double y;
    private double w;
    private double h;
    private RenderableNode opt_parent;
    private Point point;
    private NodeType nodetype = NodeType.EMPTY;
    private RenderableNode nw;
    private RenderableNode ne;
    private RenderableNode sw;
    private RenderableNode se;
    private double value = -1;
    private int depth;
    private double childrenAvg;
    private double varianceDist;
    private double score = 0;
    private SurfaceQuad surfaceQuad;
    private ShapeAttributes rectAtt;
    private Color color = null;
    private UTMCoord utmCoord;
    // Opacity of markers that are fixed size in m (< ALT_THRESH)
    final double RECT_OPACITY = 0.5;

    /**
     * Constructs a new quad tree node.
     *
     * @param {double} x X-coordiate of node.
     * @param {double} y Y-coordinate of node.
     * @param {double} w Width of node.
     * @param {double} h Height of node.
     * @param {Node} opt_parent Optional parent node.
     * @constructor
     */
    public RenderableNode(UTMCoord utmCoord, double w, double h, RenderableNode opt_parent, int depth) {
//    public RenderableNode(double x, double y, double w, double h, RenderableNode opt_parent) {
//        this.x = x;
//        this.y = y;
//        System.out.println("### NEW RenderableNode " + (utmCoord.getEasting() - 553000.0) + ", " + (utmCoord.getNorthing() - 2804000.0) + ", " + w + ", " + h);

        this.utmCoord = utmCoord;
        this.w = w;
        this.h = h;
        this.opt_parent = opt_parent;
        this.depth = depth;

//        if (opt_parent != null) {
////     - score = 1/node depth * parent's avg SD
//            score = 1.0 / depth * opt_parent.getValue();
//        }

//        System.out.println("### NEW SURFACE " + utmCoord.getEasting() + ", " + utmCoord.getNorthing() + ", " + w + ", " + h);
        LatLon latLon
                = UTMCoord.locationFromUTMCoord(
                        utmCoord.getZone(),
                        utmCoord.getHemisphere(),
                        utmCoord.getEasting() + w / 2.0,
                        utmCoord.getNorthing() + h / 2.0,
                        null);

        surfaceQuad = new SurfaceQuad(latLon, w, h);
        rectAtt = new BasicShapeAttributes();
        rectAtt.setInteriorOpacity(RECT_OPACITY);
        rectAtt.setOutlineOpacity(RECT_OPACITY);
        rectAtt.setOutlineWidth(0);
        surfaceQuad.setAttributes(rectAtt);

    //    private int depth;
        //    private double childrenAvg;
        //    private double score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getDepth() {
        return depth;
    }

//    public double getX() {
//        return x;
//    }
//
//    public void setX(double x) {
//        this.x = x;
//    }
//
//    public double getY() {
//        return y;
//    }
//
//    public void setY(double y) {
//        this.y = y;
//    }
    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public RenderableNode getParent() {
        return opt_parent;
    }

    public void setParent(RenderableNode opt_parent) {
        this.opt_parent = opt_parent;
    }

    public void setPoint(Point point, double min, double max) {
        this.point = point;
        //@todo avoid Double cast
        if (point != null) {
            value = (Double) point.getValue();
            
//            score
                    
            updateColor(min, max);
        } else {
            //@todo how to have this mean null?
            value = -1;
            score = 0;
        }
        if (opt_parent != null) {
            opt_parent.upwardRecalculateValue(min, max);
        }
    }

    public Point getPoint() {
        return this.point;
    }

    public void setNodeType(NodeType nodetype) {
        this.nodetype = nodetype;
    }

    public NodeType getNodeType() {
        return this.nodetype;
    }

    public void setNw(RenderableNode nw) {
        this.nw = nw;
    }

    public void setNe(RenderableNode ne) {
        this.ne = ne;
    }

    public void setSw(RenderableNode sw) {
        this.sw = sw;
    }

    public void setSe(RenderableNode se) {
        this.se = se;
    }

    public RenderableNode getNe() {
        return ne;
    }

    public RenderableNode getNw() {
        return nw;
    }

    public RenderableNode getSw() {
        return sw;
    }

    public RenderableNode getSe() {
        return se;
    }

    // Altered a child, recalculate parent's "avereage child" color
    protected void upwardRecalculateValue(double min, double max) {
        double sum = 0;
        int num = 0;
        if (ne != null && ne.value != -1) {
            sum += ne.value;
            num++;
        }
        if (se != null && se.value != -1) {
            sum += se.value;
            num++;
        }
        if (sw != null && sw.value != -1) {
            sum += sw.value;
            num++;
        }
        if (nw != null && nw.value != -1) {
            sum += nw.value;
            num++;
        }
        if (sum > 0) {
            value = sum / num;
            
//            score
                   
//     - score = 1/node depth * parent's avg SD 
        } else {
            value = -1;
            score = 0;
        }
        if (depth == 1) {
//            System.out.println("### root value " + value);
        }
        updateColor(min, max);

        if (opt_parent != null) {
            opt_parent.upwardRecalculateValue(min, max);
        }
    }

//    // Altered min/max
//    protected void downwardRecalculateValue() {
//        double sum = 0;
//        int num = 0;
//        if (ne != null && ne.value != -1) {
//            sum += ne.value;
//            num++;
//        }
//        if (se != null && se.value != -1) {
//            sum += se.value;
//            num++;
//        }
//        if (sw != null && sw.value != -1) {
//            sum += sw.value;
//            num++;
//        }
//        if (nw != null && nw.value != -1) {
//            sum += nw.value;
//            num++;
//        }
//        value = sum / num;
//
//        if (opt_parent != null) {
//            opt_parent.upwardRecalculateValue();
//        }
//    }
    // New min/max, adjust renderable's color
    public void updateColor(double min, double max) {
//        if (surfaceQuad == null) {
//            color = null;
//            System.out.println("### null");
//            return;
//        }
        
        // Render all
        //if (value == -1) {
        // Render leafs only
        if (value == -1 || nodetype != NodeType.LEAF) {
            color = null;
            surfaceQuad.setVisible(false);
        } else {
            color = dataToColor(value, min, max);

            Material material = new Material(color);
            rectAtt.setInteriorMaterial(material);
            rectAtt.setOutlineMaterial(material);

            surfaceQuad.setVisible(true);
        }
    }

    public double getValue() {
        return value;
    }

    public Color getColor() {
        return color;
    }

    public SurfaceQuad getRenderable() {
        return surfaceQuad;
    }

    public void updateStatistics(double average, double variance) {
        if (opt_parent == null) {
            return;
        }
        if (depth == 2) {
//            System.out.println("### (" + average + " - " + opt_parent.value + ") / " + variance);
        }
        varianceDist = Math.abs((average - opt_parent.value) / variance);
        score = 1.0 / depth * varianceDist;
    }

    public double getScore() {
        return score;
    }

    /**
     * Heatmap color computation taken from
     * http://stackoverflow.com/questions/2374959/algorithm-to-convert-any-positive-integer-to-an-rgb-value
     *
     * @param value The value to compute the color for
     * @param min Min value of data range
     * @param max Max value of data range
     * @return Heatmap color
     */
    public Color dataToColor(double value, double min, double max) {

        double wavelength = 0.0, factor = 0.0, red = 0.0, green = 0.0, blue = 0.0, gamma = 1.0;
        double adjMin = min - 5;
        double adjMax = max - 5;

        if (value < adjMin) {
            wavelength = 0.0;
        } else if (value <= adjMax) {
            wavelength = (value - adjMin) / (adjMax - adjMin) * (750.0f - 350.0f) + 350.0f;
        } else {
            wavelength = 0.0;
        }

        if (wavelength == 0.0f) {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        } else if (wavelength < 440.0f) {
            red = -(wavelength - 440.0f) / (440.0f - 350.0f);
            green = 0.0;
            blue = 1.0;
        } else if (wavelength < 490.0f) {
            red = 0.0;
            green = (wavelength - 440.0f) / (490.0f - 440.0f);
            blue = 1.0;
        } else if (wavelength < 510.0f) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510.0f) / (510.0f - 490.0f);
        } else if (wavelength < 580.0f) {
            red = (wavelength - 510.0f) / (580.0f - 510.0f);
            green = 1.0;
            blue = 0.0;
        } else if (wavelength < 645) {
            red = 1.0;
            green = -(wavelength - 645.0f) / (645.0f - 580.0f);
            blue = 0.0;
        } else {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        }

        if (wavelength == 0.0f) {
            factor = 0.0;
        } else if (wavelength < 420) {
            factor = 0.3f + 0.7f * (wavelength - 350.0f) / (420.0f - 350.0f);
        } else if (wavelength < 680) {
            factor = 1.0;
        } else {
            factor = 0.3f + 0.7f * (750.0f - wavelength) / (750.0f - 680.0f);
        }

        Color color = new Color(
                (int) Math.floor(255.0 * Math.pow(red * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(green * factor, gamma)),
                (int) Math.floor(255.0 * Math.pow(blue * factor, gamma)));
        return color;
    }

    public UTMCoord getUtmCoord() {
        return utmCoord;
    }

    public static void main(String[] args) {
        UTMCoord coord;
        UTMCoordinate coordinate;
        
        coord = UTMCoord.fromUTM(
                39,
                AVKey.NORTH,
                553000,
                2804000,
                null);
        coordinate = new UTMCoordinate(553000, 2804000, "39R");
        
        System.out.println(coord.getZone() + ", " + coord.getHemisphere());
        
//        LatLon latLon;
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000,
//                2804000,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 500,
//                2804000,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 500,
//                2804000 + 500,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000,
//                2804000 + 500,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//
//        System.out.println("S");
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000,
//                2804000,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250,
//                2804000,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250,
//                2804000 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000,
//                2804000 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//
//        System.out.println("S");
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250,
//                2804000 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250 + 250,
//                2804000 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250 + 250,
//                2804000 + 250 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
//        latLon = UTMCoord.locationFromUTMCoord(
//                39,
//                AVKey.NORTH,
//                553000 + 250,
//                2804000 + 250 + 250,
//                null);
//        System.out.println("" + latLon.getLatitude().degrees + ", " + latLon.getLongitude().degrees);
    }
}
