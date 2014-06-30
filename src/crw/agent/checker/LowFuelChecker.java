package crw.agent.checker;

import crw.event.input.service.BatteryCritical;
import dreaam.agent.checker.CheckerAgent;
import dreaam.agent.checker.CheckerAgent.AgentMessage;
import java.util.ArrayList;
import sami.event.InputEvent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;

/**
 *
 * @author pscerri
 */
public class LowFuelChecker extends CheckerAgent {

    public LowFuelChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        boolean hasLowFuelTransition;
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        // Check that each place has a transition with a LowFuelEvent trigger
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProjectSpec().getAllMissionPlans()) {
            for (Vertex v : missionPlanSpecification.getGraph().getVertices()) {
                if (v instanceof Place && !((Place) v).isEnd()) {
                    hasLowFuelTransition = false;
                    for (Transition t : ((Place) v).getOutTransitions()) {
                        for (InputEvent ie : t.getInputEvents()) {
                            if (ie instanceof BatteryCritical) {
                                hasLowFuelTransition = true;
                            }
                        }
                    }
                    if (!hasLowFuelTransition) {
                        Object[] o = new Object[1];
                        o[0] = (Place) v;
                        AgentMessage m = new AgentMessage(this, "Missing transition handling Critical Fuel!", o);
                        msgs.add(m);
                    }
                }
            }
        }
        return msgs;
    }
}
