package crw.wizard;

import crw.event.input.proxy.ProxyPathCompleted;
import crw.event.output.proxy.ProxyExecutePath;
import dreaam.wizard.EventWizardInt;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import sami.DreaamHelper;
import sami.engine.Mediator;
import sami.event.ReflectedEventSpecification;
import sami.mission.Edge;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

/**
 *
 * @author nbb
 */
public class CrwEventWizard implements EventWizardInt {

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        ReflectedEventSpecification eventSpec;
        if (eventClassname.equalsIgnoreCase(ProxyExecutePath.class.getName())) {
            // Create vertices
            // P1: Do Path [ProxyExecutePath]
            Place p1 = new Place("Do path", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            eventSpec = new ReflectedEventSpecification(ProxyExecutePath.class.getName());
            p1.addEventSpec(eventSpec);
            mSpec.updateEventSpecList(p1, eventSpec);
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p1, graphPoint);
            layout.setLocation(p1, graphPoint);
            // T1_2a
            Transition t1_2a = new Transition("Path completed", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            eventSpec = new ReflectedEventSpecification(ProxyPathCompleted.class.getName());
            t1_2a.addEventSpec(eventSpec);
            mSpec.updateEventSpecList(t1_2a, eventSpec);
            Point upperPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{1});
            mSpec.addTransition(t1_2a, upperPoint);
            layout.setLocation(t1_2a, upperPoint);
            vv.repaint();
            // T1_2b
            Transition t1_2b = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            Point lowerPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
            mSpec.addTransition(t1_2b, lowerPoint);
            layout.setLocation(t1_2b, lowerPoint);
            // P2a: Proxy collector
            Place p2a = new Place("Proxy collector", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            upperPoint = DreaamHelper.getVertexFreePoint(vv, upperPoint.getX(), upperPoint.getY(), new int[]{1});
            mSpec.addPlace(p2a, upperPoint);
            layout.setLocation(p2a, upperPoint);
            // P2b: All done
            Place p2b = new Place("All done", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            lowerPoint = DreaamHelper.getVertexFreePoint(vv, lowerPoint.getX(), lowerPoint.getY(), new int[]{3});
            mSpec.addPlace(p2b, lowerPoint);
            layout.setLocation(p2b, lowerPoint);
            // T2_3
            Transition t2_3 = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, upperPoint.getX(), upperPoint.getY(), new int[]{3});
            mSpec.addTransition(t2_3, graphPoint);
            layout.setLocation(t2_3, graphPoint);
            // P3: All proxies now done
            Place p3 = new Place("All proxies", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p3, graphPoint);
            layout.setLocation(p3, graphPoint);

            // Create Edges
            // IE-P1-T1_2a: has RP
            InEdge ie_P1_T1_2a = new InEdge(p1, t1_2a, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P1_T1_2a.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All));
            mSpec.addEdge(ie_P1_T1_2a, p1, t1_2a);
            // OE-T1_2a-P2a: take RP
            OutEdge oe_T1_2a_P2a = new OutEdge(t1_2a, p2a, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T1_2a_P2a.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take));
            mSpec.addEdge(oe_T1_2a_P2a, t1_2a, p2a);
            // IE-P1-T1_2b: empty P
            InEdge ie_P1_T1_2b = new InEdge(p1, t1_2b, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P1_T1_2b.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.None));
            mSpec.addEdge(ie_P1_T1_2b, p1, t1_2b);
            // OE-T1_2b-P2b: add G
            OutEdge oe_T1_2b_P2b = new OutEdge(t1_2b, p2b, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T1_2b_P2b.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Add, 1));
            mSpec.addEdge(oe_T1_2b_P2b, t1_2b, p2b);
            // IE-P2a-T2_3: none
            InEdge ie_P2a_T2_3 = new InEdge(p2a, t2_3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P2a_T2_3.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.None));
            mSpec.addEdge(ie_P2a_T2_3, p2a, t2_3);
            // IE-P2b-T2_3: has G
            InEdge ie_P2b_T2_3 = new InEdge(p2b, t2_3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P2b_T2_3.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.GreaterThanEqualTo, 1));
            mSpec.addEdge(ie_P2b_T2_3, p2b, t2_3);
            // OE-T2_3-P3: [consume G, take P]
            OutEdge oe_T2_3_P3 = new OutEdge(t2_3, p3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T2_3_P3.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Consume, 1));
            oe_T2_3_P3.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take));
            mSpec.addEdge(oe_T2_3_P3, t2_3, p3);
        }
        return false;
    }
}
