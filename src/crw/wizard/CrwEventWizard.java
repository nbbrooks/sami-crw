package crw.wizard;

import crw.event.input.operator.OperatorAcceptsAllocation;
import crw.event.input.operator.OperatorCreatedArea;
import crw.event.input.operator.OperatorRejectsAllocation;
import crw.event.input.operator.OperatorSelectsBoat;
import crw.event.input.operator.OperatorSelectsBoatList;
import crw.event.input.proxy.ProxyCreated;
import crw.event.input.proxy.ProxyPathCompleted;
import crw.event.input.service.AllocationResponse;
import crw.event.input.service.QuantityEqual;
import crw.event.input.service.QuantityGreater;
import crw.event.input.service.QuantityLess;
import crw.event.output.operator.OperatorAllocationOptions;
import crw.event.output.operator.OperatorCreateArea;
import crw.event.output.operator.OperatorPathOptions;
import crw.event.output.operator.OperatorSelectBoat;
import crw.event.output.operator.OperatorSelectBoatList;
import crw.event.output.proxy.ConnectExistingProxy;
import crw.event.output.proxy.CreateSimulatedProxy;
import crw.event.output.proxy.ProxyExecutePath;
import crw.event.output.service.AllocationRequest;
import crw.event.output.service.ProxyCompareDistanceRequest;
import dreaam.wizard.EventWizardInt;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import java.util.Hashtable;
import sami.DreaamHelper;
import sami.engine.Mediator;
import sami.event.ProxyStartTimer;
import sami.event.ProxyTimerExpired;
import sami.event.ReflectedEventSpecification;
import sami.event.RefreshTasks;
import sami.event.StartTimer;
import sami.event.TaskStarted;
import sami.event.TimerExpired;
import sami.mission.Edge;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.TaskSpecification;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

/**
 *
 * @author nbb
 */
public class CrwEventWizard implements EventWizardInt {

    private static final InTokenRequirement noReq = new InTokenRequirement(TokenRequirement.MatchCriteria.None, null);
    private static final InTokenRequirement hasAllRt = new InTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All);
    private static final InTokenRequirement noProxies = new InTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.None);
    private static final InTokenRequirement min1G = new InTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.GreaterThanEqualTo, 1);

    private static final OutTokenRequirement addAllRt = new OutTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Add);
    private static final OutTokenRequirement takeAllRt = new OutTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
    private static final OutTokenRequirement add1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Add, 1);
    private static final OutTokenRequirement con1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Consume, 1);
    private static final OutTokenRequirement takeAllP = new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
    private static final OutTokenRequirement take1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Take, 1);

    private static final Hashtable<String, RequiredTokenType> oeToTokenNeeded = new Hashtable<String, RequiredTokenType>();
    private static final Hashtable<String, RequiredInRequirement> ieToMinInReq = new Hashtable<String, RequiredInRequirement>();
    private static final Hashtable<String, RequiredOutRequirement> ieToMinOutReq = new Hashtable<String, RequiredOutRequirement>();

    private static final Hashtable<String, String[]> oeToParallelIe = new Hashtable<String, String[]>();
    private static final Hashtable<String, String[]> ieToParallelOe = new Hashtable<String, String[]>();

    static {
        // Output events
        oeToParallelIe.put(ProxyCompareDistanceRequest.class.getCanonicalName(), new String[]{QuantityGreater.class.getCanonicalName(), QuantityEqual.class.getCanonicalName(), QuantityLess.class.getCanonicalName()});
        oeToTokenNeeded.put(ProxyCompareDistanceRequest.class.getCanonicalName(), RequiredTokenType.Proxy);

        oeToParallelIe.put(OperatorCreateArea.class.getCanonicalName(), new String[]{OperatorCreatedArea.class.getCanonicalName()});
        oeToTokenNeeded.put(OperatorCreateArea.class.getCanonicalName(), RequiredTokenType.None);

        oeToParallelIe.put(OperatorSelectBoat.class.getCanonicalName(), new String[]{OperatorSelectsBoat.class.getCanonicalName()});
        oeToTokenNeeded.put(OperatorSelectBoat.class.getCanonicalName(), RequiredTokenType.Proxy);

        oeToParallelIe.put(OperatorSelectBoatList.class.getCanonicalName(), new String[]{OperatorSelectsBoatList.class.getCanonicalName()});
        oeToTokenNeeded.put(OperatorSelectBoatList.class.getCanonicalName(), RequiredTokenType.Proxy);

        oeToParallelIe.put(CreateSimulatedProxy.class.getCanonicalName(), new String[]{ProxyCreated.class.getCanonicalName()});
        oeToTokenNeeded.put(CreateSimulatedProxy.class.getCanonicalName(), RequiredTokenType.None);

        oeToParallelIe.put(ConnectExistingProxy.class.getCanonicalName(), new String[]{ProxyCreated.class.getCanonicalName()});
        oeToTokenNeeded.put(ConnectExistingProxy.class.getCanonicalName(), RequiredTokenType.None);

        oeToParallelIe.put(StartTimer.class.getCanonicalName(), new String[]{TimerExpired.class.getCanonicalName()});
        oeToTokenNeeded.put(StartTimer.class.getCanonicalName(), RequiredTokenType.None);

        oeToParallelIe.put(ProxyStartTimer.class.getCanonicalName(), new String[]{ProxyTimerExpired.class.getCanonicalName()});
        oeToTokenNeeded.put(ProxyStartTimer.class.getCanonicalName(), RequiredTokenType.Proxy);

        oeToParallelIe.put(AllocationRequest.class.getCanonicalName(), new String[]{AllocationResponse.class.getCanonicalName()});
        oeToTokenNeeded.put(AllocationRequest.class.getCanonicalName(), RequiredTokenType.Task);

        // Input events
        ieToMinInReq.put(QuantityGreater.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(QuantityGreater.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(QuantityEqual.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(QuantityEqual.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(QuantityLess.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(QuantityLess.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(OperatorCreatedArea.class.getCanonicalName(), RequiredInRequirement.None);
        ieToMinOutReq.put(OperatorCreatedArea.class.getCanonicalName(), RequiredOutRequirement.None);

        ieToMinInReq.put(ProxyCreated.class.getCanonicalName(), RequiredInRequirement.None);
        ieToMinOutReq.put(ProxyCreated.class.getCanonicalName(), RequiredOutRequirement.AddRt);

        ieToMinInReq.put(TimerExpired.class.getCanonicalName(), RequiredInRequirement.None);
        ieToMinOutReq.put(TimerExpired.class.getCanonicalName(), RequiredOutRequirement.None);

        ieToMinInReq.put(OperatorSelectsBoat.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(OperatorSelectsBoat.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(OperatorSelectsBoatList.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(OperatorSelectsBoatList.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(ProxyTimerExpired.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(ProxyTimerExpired.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(OperatorAcceptsAllocation.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(OperatorAcceptsAllocation.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(OperatorRejectsAllocation.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(OperatorRejectsAllocation.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(TaskStarted.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(TaskStarted.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(ProxyPathCompleted.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(ProxyPathCompleted.class.getCanonicalName(), RequiredOutRequirement.TakeRt);

        ieToMinInReq.put(AllocationResponse.class.getCanonicalName(), RequiredInRequirement.HasRt);
        ieToMinOutReq.put(AllocationResponse.class.getCanonicalName(), RequiredOutRequirement.TakeRt);
    }

    public CrwEventWizard() {
    }

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Place p1, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        if (!p1.getEventSpecs().isEmpty()) {
            // Don't act on places which already have event specs
            return false;
        }
        Point graphPoint = new Point((int) layout.getX(p1), (int) layout.getY(p1));
        ReflectedEventSpecification eventSpec;
        if (eventClassname.equalsIgnoreCase(ProxyExecutePath.class.getName())) {
            // Create vertices
            // P1: Do Path [ProxyExecutePath]
            p1.setName("Do path");
            eventSpec = new ReflectedEventSpecification(ProxyExecutePath.class.getName());
            p1.addEventSpec(eventSpec, true);
            mSpec.updateEventSpecList(p1, eventSpec);
            // T1_2a
            Transition t1_2a = new Transition("Path completed", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            eventSpec = new ReflectedEventSpecification(ProxyPathCompleted.class.getName());
            t1_2a.addEventSpec(eventSpec, true);
            mSpec.updateEventSpecList(t1_2a, eventSpec);
            Point upperPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{1});
            mSpec.addTransition(t1_2a, upperPoint);
            // T1_2b
            Transition t1_2b = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            Point lowerPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
            mSpec.addTransition(t1_2b, lowerPoint);
            // P2a: Proxy collector
            Place p2a = new Place("Proxy collector", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            upperPoint = DreaamHelper.getVertexFreePoint(vv, upperPoint.getX(), upperPoint.getY(), new int[]{1});
            mSpec.addPlace(p2a, upperPoint);
            // P2b: All done
            Place p2b = new Place("All done", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            lowerPoint = DreaamHelper.getVertexFreePoint(vv, lowerPoint.getX(), lowerPoint.getY(), new int[]{3});
            mSpec.addPlace(p2b, lowerPoint);
            // T2_3
            Transition t2_3 = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, upperPoint.getX(), upperPoint.getY(), new int[]{3});
            mSpec.addTransition(t2_3, graphPoint);
            // P3: All proxies now done
            Place p3 = new Place("All proxies", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p3, graphPoint);

            // Create Edges
            // IE-P1-T1_2a: has RT
            InEdge ie_P1_T1_2a = new InEdge(p1, t1_2a, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P1_T1_2a.addTokenRequirement(hasAllRt, true);
            mSpec.addEdge(ie_P1_T1_2a, p1, t1_2a);
            // OE-T1_2a-P2a: take RT
            OutEdge oe_T1_2a_P2a = new OutEdge(t1_2a, p2a, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T1_2a_P2a.addTokenRequirement(takeAllRt, true);
            mSpec.addEdge(oe_T1_2a_P2a, t1_2a, p2a);
            // IE-P1-T1_2b: empty P
            InEdge ie_P1_T1_2b = new InEdge(p1, t1_2b, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P1_T1_2b.addTokenRequirement(noProxies, true);
            mSpec.addEdge(ie_P1_T1_2b, p1, t1_2b);
            // OE-T1_2b-P2b: add G
            OutEdge oe_T1_2b_P2b = new OutEdge(t1_2b, p2b, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T1_2b_P2b.addTokenRequirement(take1G, true);
            mSpec.addEdge(oe_T1_2b_P2b, t1_2b, p2b);
            // IE-P2a-T2_3: no req
            InEdge ie_P2a_T2_3 = new InEdge(p2a, t2_3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P2a_T2_3.addTokenRequirement(noReq, true);
            mSpec.addEdge(ie_P2a_T2_3, p2a, t2_3);
            // IE-P2b-T2_3: has G
            InEdge ie_P2b_T2_3 = new InEdge(p2b, t2_3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P2b_T2_3.addTokenRequirement(min1G, true);
            mSpec.addEdge(ie_P2b_T2_3, p2b, t2_3);
            // OE-T2_3-P3: [consume G, take P]
            OutEdge oe_T2_3_P3 = new OutEdge(t2_3, p3, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T2_3_P3.addTokenRequirement(take1G, true);
            oe_T2_3_P3.addTokenRequirement(takeAllP, true);
            mSpec.addEdge(oe_T2_3_P3, t2_3, p3);
            return true;
        } else if (eventClassname.equalsIgnoreCase(RefreshTasks.class.getName())) {
            //@todo how to refresh this when new tasks are created after adding this sequence?

            // Create vertices
            // P1: Create [RefreshTasks]
            p1.setName("Refresh");
            eventSpec = new ReflectedEventSpecification(RefreshTasks.class.getName());
            p1.addEventSpec(eventSpec);
            mSpec.updateEventSpecList(p1, eventSpec);
            // T1_2: Created [TaskStarted]
            Transition t1_2 = new Transition("Started", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            eventSpec = new ReflectedEventSpecification(TaskStarted.class.getName());
            t1_2.addEventSpec(eventSpec);
            mSpec.updateEventSpecList(t1_2, eventSpec);
            Point upperPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{1});
            mSpec.addTransition(t1_2, upperPoint);
            // P2
            Place p2 = new Place("Collect", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
            mSpec.addPlace(p2, graphPoint);
            // Create Edges
            // IE-P1-T1_2: has RT
            InEdge ie_P1_T1_2 = new InEdge(p1, t1_2, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            ie_P1_T1_2.addTokenRequirement(hasAllRt, true);
            mSpec.addEdge(ie_P1_T1_2, p1, t1_2);
            // OE-T1_2-P2: take RT
            OutEdge oe_T1_2_P2 = new OutEdge(t1_2, p2, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            oe_T1_2_P2.addTokenRequirement(takeAllRt, true);
            mSpec.addEdge(oe_T1_2_P2, t1_2, p2);

            // Create a transition and place for each task in the mSpec
            for (TaskSpecification ts : mSpec.getTaskSpecList()) {
                //@todo better calls to getVertexFreePoint
                // T2_3n: Started
                Transition t2_3n = new Transition("Started", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                mSpec.updateEventSpecList(t2_3n, eventSpec);
                Point point = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{1});
                mSpec.addTransition(t2_3n, point);
                // P3n
                Place p3n = new Place("Begin task", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
                mSpec.addPlace(p3n, graphPoint);
                // Create Edges
                // IE-P1-T1_2: has RT
                InEdge ie_P2_T2_3n = new InEdge(p2, t2_3n, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                ie_P2_T2_3n.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.SpecificTask, TokenRequirement.MatchQuantity.Number, 1, ts.getName()), true);
                mSpec.addEdge(ie_P2_T2_3n, p2, t2_3n);
                // OE-T2_3n-P3n: take RT
                OutEdge oe_T2_3n_P3n = new OutEdge(t2_3n, p3n, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                oe_T2_3n_P3n.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.SpecificTask, TokenRequirement.MatchQuantity.GreaterThanEqualTo, TokenRequirement.MatchAction.Take, 1, ts.getName()), true);
                mSpec.addEdge(oe_T2_3n_P3n, t2_3n, p3n);
            }
            return true;
        } else if (eventClassname.equalsIgnoreCase(OperatorAllocationOptions.class.getName())) {
        } else if (eventClassname.equalsIgnoreCase(OperatorPathOptions.class.getName())) {
        } else if (oeToParallelIe.containsKey(eventClassname)) {
            // Create place for OE
            // P1
            p1.setName("");
            eventSpec = new ReflectedEventSpecification(eventClassname);
            p1.addEventSpec(eventSpec);
            mSpec.updateEventSpecList(p1, eventSpec);
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p1, graphPoint);

            // Create a transition and for each IE and places for its OEs
            for (String ieClass : oeToParallelIe.get(eventClassname)) {
                //@todo better calls to getVertexFreePoint
                // T1_2n: Started
                Transition t1_2n = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                eventSpec = new ReflectedEventSpecification(ieClass);
                t1_2n.addEventSpec(eventSpec);
                mSpec.updateEventSpecList(t1_2n, eventSpec);
                Point point = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{1});
                mSpec.addTransition(t1_2n, point);
                // Create Edges
                // IE-P1-T1_2n: has RT
                InEdge ie_P1_T1_2n = new InEdge(p1, t1_2n, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                mSpec.addEdge(ie_P1_T1_2n, p1, t1_2n);

                // Create a place and for each IE's OEs
                if (ieToParallelOe.containsKey(ieClass) && ieToParallelOe.get(ieClass).length > 0) {
                    for (String oeClass : ieToParallelOe.get(ieClass)) {
                        // P2n
                        Place p2n = new Place("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                        eventSpec = new ReflectedEventSpecification(oeClass);
                        p2n.addEventSpec(eventSpec);
                        mSpec.updateEventSpecList(p2n, eventSpec);
                        graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
                        mSpec.addPlace(p2n, graphPoint);
                        // OE-T2_3n-P3n: take RT
                        OutEdge oe_T1_2n_P2n = new OutEdge(t1_2n, p2n, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                        mSpec.addEdge(oe_T1_2n_P2n, t1_2n, p2n);
                    }
                } else {
                    // If none, create an empty place
                    // P2n
                    Place p2n = new Place("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                    graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{3});
                    mSpec.addPlace(p2n, graphPoint);
                    // OE-T2_3n-P3n: take RT
                    OutEdge oe_T1_2n_P2n = new OutEdge(t1_2n, p2n, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
                    mSpec.addEdge(oe_T1_2n_P2n, t1_2n, p2n);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        if (eventClassname.equalsIgnoreCase(ProxyExecutePath.class.getName())
                || eventClassname.equalsIgnoreCase(RefreshTasks.class.getName())
                || eventClassname.equalsIgnoreCase(OperatorAllocationOptions.class.getName())
                || eventClassname.equalsIgnoreCase(OperatorPathOptions.class.getName())
                || oeToParallelIe.containsKey(eventClassname)) {
            // We can handle this - create P1
            Place p1 = new Place("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p1, graphPoint);
            // Call overloaded method
            return runWizard(eventClassname, mSpec, p1, dsgGraph, layout, vv);
        }
        return false;
    }

    @Override
    public RequiredTokenType getOeTokenNeeded(String oe) {
        return oeToTokenNeeded.get(oe);
    }

    @Override
    public RequiredInRequirement getIeMinInReq(String ie) {
        return ieToMinInReq.get(ie);
    }

    @Override
    public RequiredOutRequirement getIeMinOutReq(String ie) {
        return ieToMinOutReq.get(ie);
    }
}
