package crw.ui.component;

import crw.ui.queue.DecisionQueuePanel;
import crw.ui.queue.QueueDatabase;
import crw.ui.queue.QueueItem;
import crw.ui.queue.QueuePanelInt;
import sami.uilanguage.MarkupManager;
import sami.engine.Engine;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
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
    private JPanel bottomPanel = new JPanel();
    private JPanel topPanel = new JPanel();
    private JScrollPane scrollingPane = new JScrollPane(bottomPanel);
    private LinkedList<QueuePanelInt> queuePanels = new LinkedList<QueuePanelInt>();
    private DecisionQueuePanel decisionQueuePanel;
    private QueuePanelInt expandedPanel = null;
    private QueueDatabase qdb;
    UiClientInt uiClient;
    UiServerInt uiServer;

    public QueueFrame() {
        this(new QueueDatabase());
    }

    public QueueFrame(QueueDatabase qdb) {
        this.qdb = qdb;
        setTitle("OperatorInteractionF");

        //Create and set up the window.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(frameDim);
        setPreferredSize(frameDim);
        setDefaultLookAndFeelDecorated(true);
        setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);

        decisionQueuePanel = new DecisionQueuePanel(this, qdb);
        topPanel.add(decisionQueuePanel, BorderLayout.NORTH);
        topPanel.setPreferredSize(decisionQueuePanel.getPreferredSize());
        topPanel.setMaximumSize(decisionQueuePanel.getPreferredSize());
        expandedPanel = decisionQueuePanel;

        //Display the window.
        pack();
        setVisible(true);

        Engine.getInstance().addListener(this);

        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    public synchronized void moveToTop(QueuePanelInt curPanel) {
        expandedPanel = curPanel;
        topPanel.removeAll();
        bottomPanel.removeAll();
        queuePanels.remove(decisionQueuePanel);
        queuePanels.remove(curPanel);   // in case of curPanel == decisionQueuePanel, fails silently
        curPanel.showContentPanel();
        topPanel.add((JComponent) curPanel);
        if (curPanel != decisionQueuePanel) {   // don't add decisionQueuePanel to bottom if it's on top
            queuePanels.addFirst(decisionQueuePanel);
        }
        for (QueuePanelInt qPanel : queuePanels) {
            qPanel.hideContentPanel();
            bottomPanel.add((JComponent) qPanel);
        }
        if (curPanel != decisionQueuePanel) {   // added at front for LRU reasons
            queuePanels.addFirst(curPanel);
        }
        scrollingPane.getViewport().setViewPosition(new Point(0, 0));
        bottomPanel.revalidate();
        topPanel.revalidate();
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

        removed = expandedPanel.removeMessageId(toUiMessageId);
        if (removed) {
            LOGGER.fine("Removed 1+ items from " + expandedPanel + " due matching ToUiMessage id: " + toUiMessageId);
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
    public void planEnteredPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planFinished(PlanManager planManager) {
        // Remove any items associated with this plan
        int numRemoved;
        numRemoved = qdb.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from queue DB due to plan " + planManager.getPlanName() + " finishing");

        numRemoved = expandedPanel.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed 1+ items from " + expandedPanel + " due to plan " + planManager.getPlanName() + " finishing");

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

        numRemoved = expandedPanel.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from " + expandedPanel + " due to plan " + planManager.getPlanName() + " aborting");

        for (QueuePanelInt queuePanel : queuePanels) {
            numRemoved = queuePanel.removeMissionId(planManager.missionId);
            LOGGER.fine("Removed " + numRemoved + " items from " + queuePanel + " due to plan " + planManager.getPlanName() + " aborting");
        }
    }
}
