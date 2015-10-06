package crw.wizard;

import crw.event.output.proxy.ProxyExecutePath;
import dreaam.wizard.EventWizardInt;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import java.awt.geom.Point2D;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Vertex;

/**
 *
 * @author nbb
 */
public class CrwEventWizard implements EventWizardInt {

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point2D graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout) {
        if (eventClassname.equalsIgnoreCase(ProxyExecutePath.class.getName())) {
            
        }
        return false;
    }
}
