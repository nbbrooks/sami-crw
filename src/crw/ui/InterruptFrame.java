package crw.ui;

import java.awt.BorderLayout;
import java.util.Hashtable;
import java.util.UUID;
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
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class InterruptFrame extends UiFrame implements PlanManagerListenerInt, UiClientListenerInt {

    private static final Logger LOGGER = Logger.getLogger(InterruptFrame.class.getName());
    Hashtable<PlanManager, InterruptPanel> pmToPanel = new Hashtable<PlanManager, InterruptPanel>();
    UiClientInt uiClient;
    UiServerInt uiServer;

    public InterruptFrame() {
        super("InterruptFrame");
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        Engine.getInstance().addListener(this);
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
        
        pack();
        setVisible(true);
    }

    public void generatePanel(final PlanManager planManager, MissionPlanSpecification mSpec) {
        boolean oirExists = false;

        for (Vertex v : mSpec.getTransientGraph().getVertices()) {
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

    @Override
    public void sharedSubPlanAtReturn(PlanManager planManager) {
    }

    public static void main(String[] args) {
        InterruptFrame interruptF = new InterruptFrame();
    }

    @Override
    public void toUiMessageReceived(ToUiMessage m) {
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
    }

    @Override
    public UiClientInt getUiClient() {
        return uiClient;
    }

    @Override
    public void setUiClient(UiClientInt uiClient) {
        if (this.uiClient != null) {
            this.uiClient.removeClientListener(this);
        }
        this.uiClient = uiClient;
        if (uiClient != null) {
            uiClient.addClientListener(this);
        }
    }

    @Override
    public UiServerInt getUiServer() {
        return uiServer;
    }

    @Override
    public void setUiServer(UiServerInt uiServer) {
        this.uiServer = uiServer;
    }
}
