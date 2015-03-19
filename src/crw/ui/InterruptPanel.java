package crw.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.InputEvent;
import sami.event.OperatorInterruptReceived;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.service.information.InformationServiceProviderInt;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiServerInt;

/**
 *
 * @author nbb
 */
public class InterruptPanel extends javax.swing.JPanel implements InformationServiceProviderInt, PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(InterruptPanel.class.getName());
    UiClientInt uiClient;
    UiServerInt uiServer;
    Hashtable<PlanManager, JPanel> pmToPanel = new Hashtable<PlanManager, JPanel>();

    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public InterruptPanel(PlanManager planManager, MissionPlanSpecification mSpec) {
        generateButtons(planManager, mSpec);
    }

    public void generateButtons(final PlanManager planManager, MissionPlanSpecification mSpec) {
        ArrayList<JButton> buttons = new ArrayList<JButton>();

        for (Vertex v : mSpec.getGraph().getVertices()) {
            if (v instanceof Transition) {
                Transition transition = (Transition) v;
                for (InputEvent ie : transition.getInputEvents()) {
                    if (ie instanceof OperatorInterruptReceived) {
                        final OperatorInterruptReceived oir = (OperatorInterruptReceived) ie;
                        JButton interruptButton = new JButton(oir.getInterruptName());
                        interruptButton.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                OperatorInterruptReceived received = new OperatorInterruptReceived(oir.getId(), oir.getMissionId(), oir.getRelevantOutputEventId());
                                planManager.eventGenerated(received);
                            }
                        });
                        buttons.add(interruptButton);
                    }
                }
            }
        }
        if (!buttons.isEmpty()) {
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(new JLabel(planManager.getPlanName()));
            for (JButton button : buttons) {
                buttonPanel.add(button);
            }
            pmToPanel.put(planManager, buttonPanel);
            add(buttonPanel);
        }
    }

    public void removePlan(PlanManager planManager) {
    }

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec) {
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planInstantiated(PlanManager planManager) {
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
    }

    @Override
    public void planAborted(PlanManager planManager) {
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "InterruptFrame offered subscription: " + sub);
        if (sub.getSubscriptionClass() == OperatorInterruptReceived.class) {
            LOGGER.log(Level.FINE, "\tInterruptFrame taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tInterruptFrame adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tInterruptFrame incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "InterruptFrame asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == OperatorInterruptReceived.class)
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "\tInterruptFrame canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tInterruptFrame removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tInterruptFrame decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}
