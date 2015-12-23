package crw.quadtree;

import java.util.Comparator;

/**
 *
 * @author nbb
 */
public class NodeComparator implements Comparator<RenderableNode> {

    public NodeComparator() {
    }

    @Override
    public int compare(RenderableNode rn1, RenderableNode rn2) {
        if (rn1 != null && rn2 != null) {
            if (rn1.getScore() < rn2.getScore()) {
                return 1;
            } else if (rn1.getScore() > rn2.getScore()) {
                return -1;
            } else {
                return 0;
            }

        } else {
            // error
            return 0;
        }
    }
}
