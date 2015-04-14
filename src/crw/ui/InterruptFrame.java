package crw.ui;

import java.awt.BorderLayout;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.event.InputEvent;
import sami.event.OperatorInterruptReceived;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.uilanguage.UiFrame;

/**
 *
 * @author nbb
 */
public class InterruptFrame extends UiFrame implements PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(InterruptFrame.class.getName());
    Hashtable<PlanManager, InterruptPanel> pmToPanel = new Hashtable<PlanManager, InterruptPanel>();

    public InterruptFrame() {
        super("InterruptFrame");
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        pack();
        setVisible(true);

        Engine.getInstance().addListener(this);
    }

    public void generatePanel(final PlanManager planManager, MissionPlanSpecification mSpec) {
        boolean oirExists = false;

        for (Vertex v : mSpec.getGraph().getVertices()) {
            if (v instanceof Transition) {
                Transition transition = (Transition) v;
                for (InputEvent ie : transition.getInputEvents()) {
                    if (ie instanceof OperatorInterruptReceived) {
                        oirExists = true;
                        break;
                    }
                }
                if (oirExists) {
                    break;
                }
            }
        }

        if (oirExists) {
            InterruptPanel ir = new InterruptPanel(planManager, mSpec);
            pmToPanel.put(planManager, ir);
            getContentPane().add(ir, BorderLayout.CENTER);
            this.revalidate();
        }
    }

    public void removePanel(final PlanManager planManager) {
        if (pmToPanel.containsKey(planManager)) {
            getContentPane().remove(pmToPanel.get(planManager));
            pmToPanel.remove(planManager);
            this.revalidate();
        }
    }

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec) {
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planInstantiated(PlanManager planManager) {
        MissionPlanSpecification mSpec = planManager.getMSpec();
        generatePanel(planManager, mSpec);
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planExecutedTransition(PlanManager planManager, Transition transition) {
    }

    @Override
    public void planRepaint(PlanManager planManager) {
    }

    @Override
    public void planFinished(PlanManager planManager) {
        removePanel(planManager);
    }

    @Override
    public void planAborted(PlanManager planManager) {
        removePanel(planManager);
    }

    public static void main(String[] args) {
        InterruptFrame interruptF = new InterruptFrame();
    }
}
