package crw.quadtree;

import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfaceQuad;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Adapted from https://github.com/varunpant/Quadtree/
 *
 * Datastructure: A point Quad Tree for representing 2D data. Each region has
 * the same ratio as the bounds for the tree.
 * <p/>
 * The implementation currently requires pre-determined bounds for data as it
 * can not rebalance itself to that degree.
 *
 * @author varunpant (https://github.com/varunpant/Quadtree/), nbb
 */
public class RenderableQuadTree {
    /*
     STATISTICS
     score is a combination of
     - spatial density
     - value uniqueness
    
     track 
     - global avg
     - global SD
    
     per bucket
     - avg
     - num points
    
     score
     - tree depth
     - parent value SD from global?
     - score = 1/node depth * parent's avg SD
    
     node expansion
     - when we fill up a node's children, split it so each has its 4 empty children
     */

    private double sum = 0;
    private int numValues = 0;
    private double averageValue;
    private double standardDeviation;
    private double variance;

    private RenderableNode root_;
    private int count_ = 0;
//    private ArrayList<RenderableNode> leafEmptyNodes = new ArrayList<RenderableNode>();
    private ArrayList<RenderableNode> leafValueNodes = new ArrayList<RenderableNode>();
//    private ArrayList<RenderableNode> fullPointerNodes = new ArrayList<RenderableNode>();
    // Keeps track of the renderable nodes we have a renderable for, so we can adjust their colors without rebuilding the renderable list
    private HashMap<RenderableNode, Renderable> nodeToRenderable = new HashMap<RenderableNode, Renderable>();
    double minValue = Double.MAX_VALUE;
    double maxValue = Double.MIN_VALUE;
    boolean minMaxChanged = false;
    long lastRenderableUpdate, lastRenderableRebuild;
    final long MAX_RENDERABLE_UPDATE_RATE = 2500; // ms
    final long MAX_RENDERABLE_REBUILD_RATE = 5000; // ms
    final double SMALLEST_DIM = 1.0; // m
    // What percent difference in data range to trigger a recalculation of heatmap colors for data values
    static final double HEATMAP_THRESH = 10.0;
    private final PriorityQueue<RenderableNode> leafEmptyNodes = new PriorityQueue<RenderableNode>(11, new NodeComparator());
//    private final PriorityQueue<RenderableNode> leafEmptyNodes = new PriorityQueue<RenderableNode>(11, new NodeComparator(this));

    /**
     * Constructs a new quad tree.
     *
     * @param {double} minX Minimum x-value that can be held in tree.
     * @param {double} minY Minimum y-value that can be held in tree.
     * @param {double} maxX Maximum x-value that can be held in tree.
     * @param {double} maxY Maximum y-value that can be held in tree.
     */
//    public RenderableQuadTree(double minX, double minY, double maxX, double maxY) {
    public RenderableQuadTree(UTMCoord minUtm, UTMCoord maxUtm) {
        //@todo check utm have same zones, etc
        this.root_ = new RenderableNode(minUtm, maxUtm.getEasting() - minUtm.getEasting(), maxUtm.getNorthing() - minUtm.getNorthing(), null, 1);
        
        split(root_);
                
//        leafEmptyNodes.add(root_);
//        emptyNodes.add(root_);
        lastRenderableUpdate = System.currentTimeMillis();
        lastRenderableRebuild = System.currentTimeMillis();
    }

    /**
     * Returns a reference to the tree's root node. Callers shouldn't modify
     * nodes, directly. This is a convenience for visualization and debugging
     * purposes.
     *
     * @return {Node} The root node.
     */
    public RenderableNode getRootNode() {
        return this.root_;
    }

    /**
     * Sets the value of an (x, y) point within the quad-tree.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @param {Object} value The value associated with the point.
     */
    public void set(UTMCoord utmCoord, double value) {
//        System.out.println("### SET " + (utmCoord.getEasting() - 553000.0) + ", " + (utmCoord.getNorthing() - 2804000.0) + " to " + value);
        RenderableNode root = this.root_;
        if (utmCoord.getEasting() < root.getUtmCoord().getEasting() || utmCoord.getNorthing() < root.getUtmCoord().getNorthing()
                || utmCoord.getEasting() > root.getUtmCoord().getEasting() + root.getW() || utmCoord.getNorthing() > root.getUtmCoord().getNorthing() + root.getH()) {
            throw new QuadTreeException("Out of bounds : (" + utmCoord.getEasting() + ", " + utmCoord.getNorthing() + ")");
        }

        //statistics
        sum += value;
        numValues++;
        averageValue = value / numValues;
//        System.out.println("### averageValue = " + averageValue);
        recalculateStandardDeviation();

        if (value < minValue) {
//            System.out.println("###\t new min " + minValue + " -> " + value);
            minValue = value;
            minMaxChanged = true;
        }
        if (value > maxValue) {
//            System.out.println("###\t new max " + maxValue + " -> " + value);
            maxValue = value;
            minMaxChanged = true;
        }
        prefix = "";
        if (this.insert(root, new Point(utmCoord, value))) {
            this.count_++;
        }

        checkUpdateColors();
    }

    private void recalculateStandardDeviation() {
        variance = 0;
        for (RenderableNode leafNode : leafValueNodes) {
            variance += Math.pow((leafNode.getValue() - averageValue), 2);
        }
//        System.out.println("### variance = " + variance);
//        standardDeviation = Math.sqrt(variance);
    }

    private void updateScores() {
//        for (RenderableNode leafNode : leafEmptyNodes) {
//            leafNode.updateStatistics(averageValue, variance);
//        }
    }

    String prefix = "";

    /**
     * Gets the value of the point at (x, y) or null if the point is empty.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @param {Object} opt_default The default value to return if the node
     * doesn't exist.
     * @return {*} The value of the node, the default value if the node doesn't
     * exist, or undefined if the node doesn't exist and no default has been
     * provided.
     */
    public Object get(double x, double y, Object opt_default) {
        RenderableNode node = this.find(this.root_, x, y);
        return node != null ? node.getPoint().getValue() : opt_default;
    }

    public RenderableNode getNode(double x, double y) {
        RenderableNode node = this.find(this.root_, x, y);
        return node;
    }

    public int getDepth(RenderableNode node) {
        return getDepth(node, 0);
    }

    private int getDepth(RenderableNode node, int depth) {
        if (node.getParent() == null) {
            return depth;
        }
        return getDepth(node.getParent(), depth++);
    }

    /**
     * Removes a point from (x, y) if it exists.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @return {Object} The value of the node that was removed, or null if the
     * node doesn't exist.
     */
    public Object remove(double x, double y) {
        RenderableNode node = this.find(this.root_, x, y);
        if (node != null) {
            Object value = node.getPoint().getValue();
            node.setPoint(null, minValue, maxValue);
//            leafNodes.remove(node);
            node.setNodeType(NodeType.EMPTY);
            this.balance(node);
            this.count_--;

            // Statistics update
            synchronized (leafValueNodes) {
                leafValueNodes.remove(node);
            }
            return value;
        } else {
            return null;
        }
    }

    /**
     * Returns true if the point at (x, y) exists in the tree.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @return {boolean} Whether the tree contains a point at (x, y).
     */
    public boolean contains(double x, double y) {
        return this.get(x, y, null) != null;
    }

    /**
     * @return {boolean} Whether the tree is empty.
     */
    public boolean isEmpty() {
        return this.root_.getNodeType() == NodeType.EMPTY;
    }

    /**
     * @return {number} The number of items in the tree.
     */
    public int getCount() {
        return this.count_;
    }

    /**
     * Removes all items from the tree.
     */
    public void clear() {
        this.root_.setNw(null);
        this.root_.setNe(null);
        this.root_.setSw(null);
        this.root_.setSe(null);
        this.root_.setNodeType(NodeType.EMPTY);
        this.root_.setPoint(null, minValue, maxValue);
        this.count_ = 0;

        // Statistics update
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.clear();
        }
        synchronized (leafValueNodes) {
            leafValueNodes.clear();
        }
    }

    /**
     * Returns an array containing the coordinates of each point stored in the
     * tree.
     *
     * @return {Array.<Point>} Array of coordinates.
     */
    public Point[] getKeys() {
        final List<Point> arr = new ArrayList<Point>();
        this.traverse(this.root_, new Func() {
            @Override
            public void call(RenderableQuadTree quadTree, RenderableNode node) {
                arr.add(node.getPoint());
            }
        });
        return arr.toArray(new Point[arr.size()]);
    }

    /**
     * Returns an array containing all values stored within the tree.
     *
     * @return {Array.<Object>} The values stored within the tree.
     */
    public Object[] getValues() {
        final List<Object> arr = new ArrayList<Object>();
        this.traverse(this.root_, new Func() {
            @Override
            public void call(RenderableQuadTree quadTree, RenderableNode node) {
                arr.add(node.getPoint().getValue());
            }
        });

        return arr.toArray(new Object[arr.size()]);
    }

    public Point[] searchIntersect(final double xmin, final double ymin, final double xmax, final double ymax) {
        final List<Point> arr = new ArrayList<Point>();
        this.navigate(this.root_, new Func() {
            @Override
            public void call(RenderableQuadTree quadTree, RenderableNode node) {
                Point pt = node.getPoint();
                if (pt.getX() < xmin || pt.getX() > xmax || pt.getY() < ymin || pt.getY() > ymax) {
                    // Definitely not within the polygon!
                } else {
                    arr.add(node.getPoint());
                }

            }
        }, xmin, ymin, xmax, ymax);
        return arr.toArray(new Point[arr.size()]);
    }

    public Point[] searchWithin(final double xmin, final double ymin, final double xmax, final double ymax) {
        final List<Point> arr = new ArrayList<Point>();
        this.navigate(this.root_, new Func() {
            @Override
            public void call(RenderableQuadTree quadTree, RenderableNode node) {
                Point pt = node.getPoint();
                if (pt.getX() > xmin && pt.getX() < xmax && pt.getY() > ymin && pt.getY() < ymax) {
                    arr.add(node.getPoint());
                }
            }
        }, xmin, ymin, xmax, ymax);
        return arr.toArray(new Point[arr.size()]);
    }

    public void navigate(RenderableNode node, Func func, double xmin, double ymin, double xmax, double ymax) {
        switch (node.getNodeType()) {
            case LEAF:
                func.call(this, node);
                break;

            case POINTER:
                if (intersects(xmin, ymax, xmax, ymin, node.getNe())) {
                    this.navigate(node.getNe(), func, xmin, ymin, xmax, ymax);
                }
                if (intersects(xmin, ymax, xmax, ymin, node.getSe())) {
                    this.navigate(node.getSe(), func, xmin, ymin, xmax, ymax);
                }
                if (intersects(xmin, ymax, xmax, ymin, node.getSw())) {
                    this.navigate(node.getSw(), func, xmin, ymin, xmax, ymax);
                }
                if (intersects(xmin, ymax, xmax, ymin, node.getNw())) {
                    this.navigate(node.getNw(), func, xmin, ymin, xmax, ymax);
                }
                break;
        }
    }

    private boolean intersects(double left, double bottom, double right, double top, RenderableNode node) {
        return !(node.getUtmCoord().getEasting() > right
                || (node.getUtmCoord().getEasting() + node.getW()) < left
                || node.getUtmCoord().getNorthing() > bottom
                || (node.getUtmCoord().getNorthing() + node.getH()) < top);
    }

    /**
     * Clones the quad-tree and returns the new instance.
     *
     * @return {QuadTree} A clone of the tree.
     */
    public RenderableQuadTree clone() {
//        double x1 = this.root_.getX();
//        double y1 = this.root_.getY();
//        double x2 = x1 + this.root_.getW();
//        double y2 = y1 + this.root_.getH();

        //@todo finish
//        UTMCoord utmCoordCopy = new UTMCoord(Angle.ZERO, Angle.ZERO, count_, null, y2, y1)
//        this.root_.getUtmCoord();
//        final RenderableQuadTree clone = new RenderableQuadTree(x1, y1, x2, y2);
//        // This is inefficient as the clone needs to recalculate the structure of the
//        // tree, even though we know it already.  But this is easier and can be
//        // optimized when/if needed.
//        this.traverse(this.root_, new Func() {
//            @Override
//            public void call(RenderableQuadTree quadTree, RenderableNode node) {
//                clone.set(node.getPoint().getUtmCoord(), node.getPoint().getValue());
//            }
//        });
//
//        return clone;
        return null;
    }

    /**
     * Traverses the tree depth-first, with quadrants being traversed in
     * clockwise order (NE, SE, SW, NW). The provided function will be called
     * for each leaf node that is encountered.
     *
     * @param {QuadTree.Node} node The current node.
     * @param {function(QuadTree.Node)} fn The function to call for each leaf
     * node. This function takes the node as an argument, and its return value
     * is irrelevant.
     * @private
     */
    public void traverse(RenderableNode node, Func func) {
        switch (node.getNodeType()) {
            case LEAF:
                func.call(this, node);
                break;

            case POINTER:
                this.traverse(node.getNe(), func);
                this.traverse(node.getSe(), func);
                this.traverse(node.getSw(), func);
                this.traverse(node.getNw(), func);
                break;
        }
    }

    /**
     * Finds a leaf node with the same (x, y) coordinates as the target point,
     * or null if no point exists.
     *
     * @param {QuadTree.Node} node The node to search in.
     * @param {number} x The x-coordinate of the point to search for.
     * @param {number} y The y-coordinate of the point to search for.
     * @return {QuadTree.Node} The leaf node that matches the target, or null if
     * it doesn't exist.
     * @private
     */
    public RenderableNode find(RenderableNode node, double x, double y) {
        RenderableNode response = null;
        switch (node.getNodeType()) {
            case EMPTY:
                break;

            case LEAF:
                response = node.getPoint().getX() == x && node.getPoint().getY() == y ? node : null;
                break;

            case POINTER:
                response = this.find(this.getQuadrantForPoint(node, x, y), x, y);
                break;

            default:
                throw new QuadTreeException("Invalid nodeType");
        }
        return response;
    }

    private void checkForFullPointerNode(RenderableNode node) {
        if (node.getNe().getPoint() != null
                && node.getSe().getPoint() != null
                && node.getSw().getPoint() != null
                && node.getNw().getPoint() != null //                && !fullPointerNodes.contains(node)
                ) {
//            fullPointerNodes.add(node);
        }
    }

    private void removeFullPointerNode(RenderableNode node) {
//        fullPointerNodes.remove(node);
    }

    /**
     * Inserts a point into the tree, updating the tree's structure if
     * necessary.
     *
     * @param {.QuadTree.Node} parent The parent to insert the point into.
     * @param {QuadTree.Point} point The point to insert.
     * @return {boolean} True if a new node was added to the tree; False if a
     * node already existed with the correpsonding coordinates and had its value
     * reset.
     * @private
     */
    private boolean insert(RenderableNode parent, Point point) {
        Boolean result = false;
        switch (parent.getNodeType()) {
            case EMPTY:
                // Leaf node with no value
//                System.out.println("###\t" + prefix + " INSERT on EMPTY node");

                // Quadtree manipulation
                this.splitAndSetPointForNode(parent, point);
                result = true;

                // Statistics updates
                // Not setting it on this anymore
//                leafValueNodes.add(parent);
                break;
            case LEAF:
                // Leaf node with value
//                System.out.println("###\t" + prefix + " INSERT on LEAF node");

                if (parent.getW() <= SMALLEST_DIM || parent.getH() <= SMALLEST_DIM) {
                    // If we are at max depth, overwrite the value here instead of splitting

                    // Quadtree manipulation
                    this.setPointForNode(parent, point);
                    result = false;

                    // Statistics updates
//                emptyNodes.remove(parent);
                    if (parent.getParent() != null) {
                        checkForFullPointerNode(parent.getParent());
                        parent.getParent().upwardRecalculateValue(minValue, maxValue);
                    }

                } else {
                    if (parent.getPoint().getX() == point.getX() && parent.getPoint().getY() == point.getY()) {
                        // If the new value is at the exact same location as the old value, just overwrite the value 

                        // Quadtree manipulation
                        this.setPointForNode(parent, point);
                        result = false;

                        // Statistics updates
                        if (parent.getParent() != null) {
                            checkForFullPointerNode(parent.getParent());
                            parent.getParent().upwardRecalculateValue(minValue, maxValue);
                        }
                    } else {
                        // Otherwise, split this node and insert this node's value and the received value into its new children

                        // Quadtree manipulation
                        this.split(parent);
                        leafValueNodes.remove(parent);
                        removeFullPointerNode(parent.getParent());
                        if (prefix.indexOf("\t") != -1) {
                            prefix = prefix.substring(0, prefix.length() - 1);
                        }
                        result = this.insert(parent, point);

                        // Statistics updates
                        parent.getNe().updateStatistics(averageValue, variance);
                        parent.getSe().updateStatistics(averageValue, variance);
                        parent.getSw().updateStatistics(averageValue, variance);
                        parent.getNw().updateStatistics(averageValue, variance);
                    }
                }
                break;
            case POINTER:
                // Non-leaf node
                //  Send to appropriate child node
//                System.out.println("###\t " + prefix + " INSERT on POINTER node");

                // Quadtree manipulation
                prefix += "\t";
                result = this.insert(
                        this.getQuadrantForPoint(parent, point.getX(), point.getY()), point);
                break;

            default:
                throw new QuadTreeException("Invalid nodeType in parent");
        }
        return result;
    }

    private void split(RenderableNode node) {
        Point oldPoint = node.getPoint();
//        System.out.println("### split " + node.getDepth());
        node.setPoint(null, minValue, maxValue);
        node.setNodeType(NodeType.POINTER);

        // Statistics update
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.remove(node);
        }
        synchronized (leafValueNodes) {
            leafValueNodes.remove(node);
        }

        int zone = node.getUtmCoord().getZone();
        String hemisphere = node.getUtmCoord().getHemisphere();
        double easting = node.getUtmCoord().getEasting();
        double northing = node.getUtmCoord().getNorthing();
        double hw = node.getW() / 2.0;
        double hh = node.getH() / 2.0;

        double score;
//        if (variance == 0) {
//            score = 0;
//        } else {
            score = 1.0 / (node.getDepth() + 1) * (Math.pow(node.getValue() - averageValue, 2) / variance);
//        }

        RenderableNode n;
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting, northing + hh), hw, hh, node, node.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        node.setNw(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting + hw, northing + hh), hw, hh, node, node.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        node.setNe(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting, northing), hw, hh, node, node.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        node.setSw(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting + hw, northing), hw, hh, node, node.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        node.setSe(n);

        if(oldPoint != null) {
            this.insert(node, oldPoint);
        }
    }

    /**
     * Attempts to balance a node. A node will need balancing if all its
     * children are empty or it contains just one leaf.
     *
     * @param {QuadTree.Node} node The node to balance.
     * @private
     */
    private void balance(RenderableNode node) {
        System.out.println("### BALANCING");
        //@todo update statistics

        switch (node.getNodeType()) {
            case EMPTY:
            case LEAF:
                if (node.getParent() != null) {
                    this.balance(node.getParent());
                }
                break;

            case POINTER: {
                RenderableNode nw = node.getNw();
                RenderableNode ne = node.getNe();
                RenderableNode sw = node.getSw();
                RenderableNode se = node.getSe();
                RenderableNode firstLeaf = null;

                // Look for the first non-empty child, if there is more than one then we
                // break as this node can't be balanced.
                if (nw.getNodeType() != NodeType.EMPTY) {
                    firstLeaf = nw;
                }
                if (ne.getNodeType() != NodeType.EMPTY) {
                    if (firstLeaf != null) {
                        break;
                    }
                    firstLeaf = ne;
                }
                if (sw.getNodeType() != NodeType.EMPTY) {
                    if (firstLeaf != null) {
                        break;
                    }
                    firstLeaf = sw;
                }
                if (se.getNodeType() != NodeType.EMPTY) {
                    if (firstLeaf != null) {
                        break;
                    }
                    firstLeaf = se;
                }

                if (firstLeaf == null) {
                    // All child nodes are empty: so make this node empty.
                    node.setNodeType(NodeType.EMPTY);
                    node.setNw(null);
                    node.setNe(null);
                    node.setSw(null);
                    node.setSe(null);

                } else if (firstLeaf.getNodeType() == NodeType.POINTER) {
                    // Only child was a pointer, therefore we can't rebalance.
                    break;

                } else {
                    // Only child was a leaf: so update node's point and make it a leaf.
                    node.setNodeType(NodeType.LEAF);
                    node.setNw(null);
                    node.setNe(null);
                    node.setSw(null);
                    node.setSe(null);
//                    System.out.println("### balance setPoint");
                    node.setPoint(firstLeaf.getPoint(), minValue, maxValue);

                    // Statistics update
                    if (firstLeaf.getPoint() != null) {
                        synchronized (leafEmptyNodes) {
                            leafEmptyNodes.remove(node);
                        }
                        synchronized (leafValueNodes) {
                            leafValueNodes.add(node);
                        }
                    } else {
                        synchronized (leafEmptyNodes) {
                            leafEmptyNodes.add(node);
                        }
                        synchronized (leafValueNodes) {
                            leafValueNodes.remove(node);
                        }
                    }
                }

                // Try and balance the parent as well.
                if (node.getParent() != null) {
                    this.balance(node.getParent());
                }
            }
            break;
        }
    }

    /**
     * Returns the child quadrant within a node that contains the given (x, y)
     * coordinate.
     *
     * @param {QuadTree.Node} parent The node.
     * @param {number} x The x-coordinate to look for.
     * @param {number} y The y-coordinate to look for.
     * @return {QuadTree.Node} The child quadrant that contains the point.
     * @private
     */
    private RenderableNode getQuadrantForPoint(RenderableNode parent, double x, double y) {
        double mx = parent.getUtmCoord().getEasting() + parent.getW() / 2;
        double my = parent.getUtmCoord().getNorthing() + parent.getH() / 2;
        if (x < mx) {
            return y < my ? parent.getSw() : parent.getNw();
        } else {
            return y < my ? parent.getSe() : parent.getNe();
        }
    }

    /**
     * Sets the point for a node, as long as the node is a leaf or empty.
     *
     * @param {QuadTree.Node} node The node to set the point for.
     * @param {QuadTree.Point} point The point to set.
     * @private
     */
    private void setPointForNode(RenderableNode node, Point point) {
        if (node.getNodeType() == NodeType.POINTER) {
            throw new QuadTreeException("Can not set point for node of type POINTER");
        }
//        System.out.println("### set point for node " + node.getDepth());
        node.setNodeType(NodeType.LEAF);
        node.setPoint(point, minValue, maxValue);

        // Statistics update
        if (point != null) {
            synchronized (leafEmptyNodes) {
                leafEmptyNodes.remove(node);
            }
            synchronized (leafValueNodes) {
                leafValueNodes.add(node);
            }
        } else {
            synchronized (leafEmptyNodes) {
                leafEmptyNodes.add(node);
            }
            synchronized (leafValueNodes) {
                leafValueNodes.remove(node);
            }
        }
    }

    /**
     * Splits an empty node which we want to set a point on and instead set the
     * point on its appropriate child.
     *
     * @param parent
     * @param point
     */
    private void splitAndSetPointForNode(RenderableNode parent, Point point) {
        // Split
        parent.setNodeType(NodeType.POINTER);

        synchronized (leafEmptyNodes) {
            leafEmptyNodes.remove(parent);
        }

        int zone = parent.getUtmCoord().getZone();
        String hemisphere = parent.getUtmCoord().getHemisphere();
        double easting = parent.getUtmCoord().getEasting();
        double northing = parent.getUtmCoord().getNorthing();
        double hw = parent.getW() / 2.0;
        double hh = parent.getH() / 2.0;

        double score;
        if(parent.getDepth() == 1) {
            score = Double.MAX_VALUE;
        } else {
            score = 1.0 / (parent.getDepth() + 1) * (Math.pow(parent.getValue() - averageValue, 2) / variance);
        }

        RenderableNode n;
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting, northing + hh), hw, hh, parent, parent.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
//        emptyNodes.add(n);
        parent.setNw(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting + hw, northing + hh), hw, hh, parent, parent.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        parent.setNe(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting, northing), hw, hh, parent, parent.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        parent.setSw(n);
        n = new RenderableNode(UTMCoord.fromUTM(zone, hemisphere, easting + hw, northing), hw, hh, parent, parent.getDepth() + 1);
        n.setScore(score);
//        n.updateStatistics(averageValue, variance);
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.add(n);
        }
        parent.setSe(n);

        // Insert
        n = this.getQuadrantForPoint(parent, point.getX(), point.getY());
//        System.out.println("### set point for node " + node.getDepth());
        n.setNodeType(NodeType.LEAF);
//        System.out.println("### setPointForNode setPoint");
        n.setPoint(point, minValue, maxValue);

        // Statistics update
        synchronized (leafEmptyNodes) {
            leafEmptyNodes.remove(n);
        }
        synchronized (leafValueNodes) {
            leafValueNodes.add(n);
        }

        if (parent.getParent() != null) {
//            System.out.println("### splitAndSetPointForNode: parent.getParent() != null");
            checkForFullPointerNode(parent.getParent());
            parent.getParent().upwardRecalculateValue(minValue, maxValue);
        }
    }

    public void checkUpdateColors() {
//        System.out.println("###\t checkUpdateColors");
        if (!minMaxChanged) {
            return;
        }
        long curTime = System.currentTimeMillis();
        if (curTime - lastRenderableRebuild < MAX_RENDERABLE_REBUILD_RATE) {
            lastRenderableRebuild = curTime;
            lastRenderableUpdate = curTime;
//            rebuildRenderables(5);
        }
        if (curTime - lastRenderableUpdate < MAX_RENDERABLE_UPDATE_RATE) {
            lastRenderableUpdate = curTime;
            updateColors();
        }
    }

    /**
     * Recomputes the currently viewed sensor's heatmap values in a separate
     * thread
     */
    public void updateColors() {
//        System.out.println("###\t\t updateColors");
//        System.out.println("### RECOMPUTE ###");
        (new Thread() {
            public void run() {
                Iterator<Map.Entry<RenderableNode, Renderable>> it = nodeToRenderable.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<RenderableNode, Renderable> entry = it.next();
                    if (entry.getValue() instanceof SurfaceQuad) {
                        SurfaceQuad surface = (SurfaceQuad) entry.getValue();
                        entry.getKey().updateColor(minValue, maxValue);
//                        Color color = entry.getKey().getColor();
//                        Material material = new Material(color);
//                        ShapeAttributes attributes = surface.getAttributes();
//                        attributes.setInteriorMaterial(material);
//                        attributes.setOutlineMaterial(material);
                    }
                }
            }
        }).start();
    }

    public void rebuildRenderables() {
//        System.out.println("### rebuildRenderables");
        (new Thread() {
            public void run() {
                //@todo clone or synchronize?
                HashMap<RenderableNode, Renderable> newNodeToRenderable = new HashMap<RenderableNode, Renderable>();
                rebuildRenderables(root_, 0, 8, newNodeToRenderable);

//                Iterator<Map.Entry<RenderableNode, Renderable>> it = nodeToRenderable.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry<RenderableNode, Renderable> entry = it.next();
//                    if (entry.getValue() instanceof SurfaceQuad) {
//                        SurfaceQuad surface = (SurfaceQuad) entry.getValue();
//                        entry.getKey().recalculateRenderable(minValue, maxValue);
//                        Color color = entry.getKey().getColor();
//                        Material material = new Material(color);
//                        ShapeAttributes attributes = surface.getAttributes();
//                        attributes.setInteriorMaterial(material);
//                        attributes.setOutlineMaterial(material);
//                    }
//                }
//                System.out.println("###\t " + nodeToRenderable.size() + " -> " + newNodeToRenderable.size() + " renderables");
                nodeToRenderable = newNodeToRenderable;
            }
        }).start();
    }

//    private void rebuildRenderables(RenderableNode node, int depth, int maxDepth, HashMap<RenderableNode, Renderable> newNodeToRenderable) {
//        if (node == null) {
//            return;
//        }
//        if (depth == maxDepth) {
//            if (node.getColor() != null) {
////            if (node.getValue() != -1) {
//                newNodeToRenderable.put(node, node.getRenderable());
//            }
//            return;
//        }
//
//        if (node.getColor() != null) {
////        if (node.getValue() != -1) {
//            // If there is a quadtree descendant that is null/has no value, use this node's renderable
//            newNodeToRenderable.put(node, node.getRenderable());
//        }
//        rebuildRenderables(node.getNe(), depth + 1, maxDepth, newNodeToRenderable);
//        rebuildRenderables(node.getSe(), depth + 1, maxDepth, newNodeToRenderable);
//        rebuildRenderables(node.getSw(), depth + 1, maxDepth, newNodeToRenderable);
//        rebuildRenderables(node.getNw(), depth + 1, maxDepth, newNodeToRenderable);
//    }
//    private void rebuildRenderables(RenderableNode node, int depth, int maxDepth, HashMap<RenderableNode, Renderable> newNodeToRenderable) {
//        if (node == null) {
//            return;
//        }
//        if (depth == maxDepth) {
//            if (node.getValue() != -1) {
//                newNodeToRenderable.put(node, node.getRenderable());
//            }
//            return;
//        }
////        node.updateColor(minValue, maxValue);
//
////        if (node.getNe() == null || node.getNe().getValue() == -1
////                || node.getSe() == null || node.getSe().getValue() == -1
////                || node.getSw() == null || node.getSw().getValue() == -1
////                || node.getNw() == null || node.getNw().getValue() == -1) {
////            // If there is a quadtree descendant that is null/has no value, use this node's renderable
////            newNodeToRenderable.put(node, node.getRenderable());
//        if ((node.getNe() == null || node.getNe().getValue() == -1)
//                && (node.getSe() == null || node.getSe().getValue() == -1)
//                && (node.getSw() == null || node.getSw().getValue() == -1)
//                && (node.getNw() == null || node.getNw().getValue() == -1)
//                && node.getValue() != -1) {
//            // If there is a quadtree descendant that is null/has no value, use this node's renderable
//            newNodeToRenderable.put(node, node.getRenderable());
//            return;
//        } else {
//            // Otherwise recurse into descendants
//
////            if (node.getNe() != null) {
////            node.getNe().recalculateRenderable(minValue, maxValue);
////            newNodeToRenderable = node.getNe().get
//            rebuildRenderables(node.getNe(), depth + 1, maxDepth, newNodeToRenderable);
////            }
////            if (node.getSe() != null) {
////            node.getSe().recalculateRenderable(minValue, maxValue);
//            rebuildRenderables(node.getSe(), depth + 1, maxDepth, newNodeToRenderable);
////            }
////            if (node.getSw() != null) {
////            node.getSw().recalculateRenderable(minValue, maxValue);
//            rebuildRenderables(node.getSw(), depth + 1, maxDepth, newNodeToRenderable);
////            }
////            if (node.getNw() != null) {
////            node.getNw().recalculateRenderable(minValue, maxValue);
//            rebuildRenderables(node.getNw(), depth + 1, maxDepth, newNodeToRenderable);
////            }
//        }
//    }
    private void rebuildRenderables(RenderableNode node, int depth, int maxDepth, HashMap<RenderableNode, Renderable> newNodeToRenderable) {

        /*
         - if all children defined, do not use
         - if all children undefined, use
         - if undefined, use parent's color, lighter opacity
         */
        if (node == null) {
            return;
        }
        if (depth == maxDepth) {
            if (node.getValue() != -1) {
                newNodeToRenderable.put(node, node.getRenderable());
            }
            return;
        }
//        node.updateColor(minValue, maxValue);

//        if (node.getNe() == null || node.getNe().getValue() == -1
//                || node.getSe() == null || node.getSe().getValue() == -1
//                || node.getSw() == null || node.getSw().getValue() == -1
//                || node.getNw() == null || node.getNw().getValue() == -1) {
//            // If there is a quadtree descendant that is null/has no value, use this node's renderable
//            newNodeToRenderable.put(node, node.getRenderable());
        if ((node.getNe() == null || node.getNe().getValue() == -1)
                && (node.getSe() == null || node.getSe().getValue() == -1)
                && (node.getSw() == null || node.getSw().getValue() == -1)
                && (node.getNw() == null || node.getNw().getValue() == -1)
                && node.getValue() != -1) {
            // If all descendants are null/have no value, use this node's renderable and return
            newNodeToRenderable.put(node, node.getRenderable());
            return;
        }

        if ((node.getNe() == null || node.getNe().getValue() == -1)
                || (node.getSe() == null || node.getSe().getValue() == -1)
                || (node.getSw() == null || node.getSw().getValue() == -1)
                || (node.getNw() == null || node.getNw().getValue() == -1)
                || node.getValue() != -1) {
            // If there is a descendant that is null/has no value, use this node's renderable
            newNodeToRenderable.put(node, node.getRenderable());
        }

        // Recurse into descendants
        rebuildRenderables(node.getNe(), depth + 1, maxDepth, newNodeToRenderable);
        rebuildRenderables(node.getSe(), depth + 1, maxDepth, newNodeToRenderable);
        rebuildRenderables(node.getSw(), depth + 1, maxDepth, newNodeToRenderable);
        rebuildRenderables(node.getNw(), depth + 1, maxDepth, newNodeToRenderable);
    }

    public ArrayList<Renderable> getRenderables() {
        ArrayList<Renderable> renderables = new ArrayList<Renderable>();

//        LatLon latLon
//                = UTMCoord.locationFromUTMCoord(
//                        39,
//                        AVKey.NORTH,
//                        553327.0,
//                        2804466.0,
//                        null);
//        System.out.println("Lat lon " + latLon);
//
//        SurfaceQuad testSurfaceQuad = new SurfaceQuad(latLon, 500, 500);
//        BasicShapeAttributes testAtt = new BasicShapeAttributes();
//        testAtt.setOutlineMaterial(Material.GREEN);
//        testAtt.setInteriorMaterial(Material.GREEN);
//        testAtt.setInteriorOpacity(0.5);
//        testAtt.setOutlineOpacity(0.5);
//        testAtt.setOutlineWidth(0);
//        testSurfaceQuad.setAttributes(testAtt);
//        renderables.add(testSurfaceQuad);
//        System.out.println("### getRenderables");
        for (RenderableNode rn : nodeToRenderable.keySet()) {
            Renderable r = nodeToRenderable.get(rn);
            renderables.add(r);
//            System.out.println("###\t " + (rn.getUtmCoord().getEasting() - 553000.0) + ", " + (rn.getUtmCoord().getNorthing() - 2804000.0) + ", " + rn.getW() + ", " + rn.getH());
//            System.out.println("###\t " + String.format("%4.4f \t %4.0f \t %4.4f \t %4.0f", (rn.getUtmCoord().getEasting() - 553000.0), rn.getW(), (rn.getUtmCoord().getNorthing() - 2804000.0), rn.getH()));
        }
//        for (Renderable r : nodeToRenderable.values()) {
//            renderables.add(r);
//        }
        return renderables;
    }

//    public class NodeComparator implements Comparator<RenderableNode> {
//
//        private RenderableQuadTree quadTree;
//
//        public NodeComparator(RenderableQuadTree quadTree) {
//            this.quadTree = quadTree;
//        }
//
//        @Override
//        public int compare(RenderableNode rn1, RenderableNode rn2) {
//            if (rn1 != null && rn2 != null) {
//                if (rn1.getScore() < rn2.getScore()) {
//                    return 1;
//                } else if (rn1.getScore() > rn2.getScore()) {
//                    return -1;
//                } else {
//                    return 0;
//                }
//
//            } else {
//                // error
//                return 0;
//            }
//        }
//    }

    public double getAverage() {
        return averageValue;
    }

    public double getVariance() {
        return variance;
    }

    public RenderableNode[] getLeafEmptyNodesCopy() {
        synchronized (leafEmptyNodes) {
            return leafEmptyNodes.toArray(new RenderableNode[0]);
        }
    }

    public RenderableNode peekLeafEmptyNode() {
        synchronized (leafEmptyNodes) {
            return leafEmptyNodes.peek();
        }
    }

    public RenderableNode pollLeafEmptyNode() {
        synchronized (leafEmptyNodes) {
            return leafEmptyNodes.poll();
        }
    }

    public boolean noEmptyLeafNodes() {
        synchronized (leafEmptyNodes) {
            return leafEmptyNodes.isEmpty();
        }
    }

    public RenderableNode[] getLeafValueNodesCopy() {
        synchronized (leafValueNodes) {
            return leafValueNodes.toArray(new RenderableNode[0]);
        }
    }
}

/*
 Always store value
 - if we are an empty node from a split, value is -1
 - if we are a pointer node, value is average of direct child nodes with values != -1 (may revisit this to weighted average based on size/num defined leafs)
 - update values upwards whenever we call setPoint on a node

 Renerable
 - is a square covering over dimensions with value's heatmap color
 */
