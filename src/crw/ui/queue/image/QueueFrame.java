package crw.ui.queue.image;

import crw.ui.queue.QueueContent;
import crw.ui.queue.QueueDatabase;
import sami.uilanguage.MarkupManager;
import sami.engine.Engine;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class QueueFrame extends UiFrame implements UiClientListenerInt, PlanManagerListenerInt {

    private final static Logger LOGGER = Logger.getLogger(QueueFrame.class.getName());
    private Dimension frameDim = new Dimension(DecisionQueuePanel.NUM_THUMBNAILS * QueueItem.THUMB_SCALED_WIDTH + 98, 600);
    // LRU ordered list of queue panels
    private LinkedList<QueuePanelInt> queuePanels = new LinkedList<QueuePanelInt>();
    private DecisionQueuePanel decisionQueuePanel;
    private QueuePanelInt activeQueuePanel = null;
    private QueueDatabase qdb;
    UiClientInt uiClient;
    UiServerInt uiServer;

    // Filmstrip for the active queue, shown at top of frame
    private JPanel activeQueueFilmstripP;
    // SP for active queue's current item's component, shown below active queue's filmstrip
    private JScrollPane activeQueueContentSP;
    // Filmstrips for all inactive queues, shown at bottom of frame
    private JPanel inActiveQueueFilmstripsP;
    private JScrollPane inactiveQueueFilmstripsSP;

    public QueueFrame() {
        this(new QueueDatabase());
    }

    public QueueFrame(QueueDatabase qdb) {
        this.qdb = qdb;

        activeQueueFilmstripP = new JPanel(new BorderLayout());
        activeQueueContentSP = new JScrollPane();
        inActiveQueueFilmstripsP = new JPanel();
        inactiveQueueFilmstripsSP = new JScrollPane(inActiveQueueFilmstripsP);

        //Create and set up the frame
        setTitle("OperatorInteractionF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(frameDim);
        setPreferredSize(frameDim);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().add(activeQueueFilmstripP);
        getContentPane().add(activeQueueContentSP);
        getContentPane().add(inactiveQueueFilmstripsSP);
        pack();
        setVisible(true);

        // Create decision queue panel and set it as active queue
        decisionQueuePanel = new DecisionQueuePanel(this, qdb);
        moveToTop(decisionQueuePanel);

        Engine.getInstance().addListener(this);

        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    public synchronized void moveToTop(QueuePanelInt curPanel) {
        curPanel.setIsActive(true);
        activeQueuePanel = curPanel;
        // Clear both filmstrip panels
        activeQueueFilmstripP.removeAll();
        inActiveQueueFilmstripsP.removeAll();

        // Update active queue filmstrip panel
        activeQueueFilmstripP.add((JComponent) curPanel, BorderLayout.NORTH);

        // Update active queue content panel
        if (curPanel.getCurrentContent() != null) {
            setActiveQueueContent(curPanel.getCurrentContent().getInteractionPanel());
        } else {
            setActiveQueueContent(null);
        }

        // Update LRU queue panel so it only contains inactive queues, which we will use to populate the inactive queue filmstrip panel
        // Remove currently active queue panel from ordered list of queue
        queuePanels.remove(activeQueuePanel);
        // Remove passed in queue panel from ordered list (if curPanel == expandedPanel, fails silently)
        queuePanels.remove(curPanel);
        // Update queue panel list by LRU
        if (curPanel != activeQueuePanel) {
            // If we are changing the active panel, add the previously active panel to the top of the list
            queuePanels.addFirst(activeQueuePanel);
        }
        // Finally add all the inactive filmstrips to the collapsed filmstrip panel
        for (QueuePanelInt qPanel : queuePanels) {
            qPanel.setIsActive(false);
            inActiveQueueFilmstripsP.add((JComponent) qPanel);
        }

        // After updating the inactive filmstrip p, add the active panel to the front of the list
        if (curPanel != decisionQueuePanel) {
            queuePanels.addFirst(curPanel);
        }
    }

    public void setActiveQueueContent(QueueContent content) {
        activeQueueContentSP.setViewportView(content);
    }

    @Override
    public void toUiMessageReceived(ToUiMessage toUiMsg) {
        if (toUiMsg instanceof SelectionMessage
                || toUiMsg instanceof CreationMessage) {
            LOGGER.info("@STAT QueueFrame ToUiMessage received: " + toUiMsg);
            MarkupManager manager = new MarkupManager(toUiMsg);
            manager.addComponent(this);
            qdb.addDecision(toUiMsg, manager);

        }
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
        // If this item is in the queue, remove it as it has been handled externally (ex: MI autonomy made the decision)
        boolean removed;
        removed = qdb.removeMessageId(toUiMessageId);
        if (removed) {
            LOGGER.fine("Removed 1+ items from queue DB due matching ToUiMessage id: " + toUiMessageId);
        }

        removed = activeQueuePanel.removeMessageId(toUiMessageId);
        if (removed) {
            LOGGER.fine("Removed 1+ items from " + activeQueuePanel + " due matching ToUiMessage id: " + toUiMessageId);
        }

        for (QueuePanelInt queuePanel : queuePanels) {
            removed = queuePanel.removeMessageId(toUiMessageId);
            if (removed) {
                LOGGER.fine("Removed 1+ items from " + queuePanel + " due matching ToUiMessage id: " + toUiMessageId);
            }
        }
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
        // Remove any items associated with this plan
        int numRemoved;
        numRemoved = qdb.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from queue DB due to plan " + planManager.getPlanName() + " finishing");

        numRemoved = activeQueuePanel.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed 1+ items from " + activeQueuePanel + " due to plan " + planManager.getPlanName() + " finishing");

        for (QueuePanelInt queuePanel : queuePanels) {
            numRemoved = queuePanel.removeMissionId(planManager.missionId);
            LOGGER.fine("Removed " + numRemoved + " items from " + queuePanel + " due to plan " + planManager.getPlanName() + " finishing");
        }
    }

    @Override
    public void planAborted(PlanManager planManager) {
        // Remove any items associated with this plan
        int numRemoved;
        numRemoved = qdb.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from queue DB due to plan " + planManager.getPlanName() + " aborting");

        numRemoved = activeQueuePanel.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from " + activeQueuePanel + " due to plan " + planManager.getPlanName() + " aborting");

        for (QueuePanelInt queuePanel : queuePanels) {
            numRemoved = queuePanel.removeMissionId(planManager.missionId);
            LOGGER.fine("Removed " + numRemoved + " items from " + queuePanel + " due to plan " + planManager.getPlanName() + " aborting");
        }
    }
}
