package crw.ui.queue.text;

import crw.ui.queue.QueueContent;
import crw.ui.queue.QueueDatabase;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.uilanguage.MarkupManager;
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
    private Dimension frameDim = new Dimension(600 + 98, 600);
    private Dimension activeQueueDim = new Dimension(Integer.MAX_VALUE, 150);
    private Dimension inactiveQueueDim = new Dimension(Integer.MAX_VALUE, 150);
    // LRU ordered list of queue panels
    private LinkedList<QueuePanelInt> queuePanels = new LinkedList<QueuePanelInt>();
    private DecisionQueuePanel nominalQueuePanel;
    private QueuePanelInt activeQueuePanel = null;
    private QueueDatabase nominalQdb;
    UiClientInt uiClient;
    UiServerInt uiServer;

    // Filmstrip for the active queue, shown at top of frame
    private JPanel activeQueueFilmstripP;
    // SP for active queue's current item's component, shown below active queue's filmstrip
    private JScrollPane activeQueueContentSP;
    // Filmstrips for all inactive queues, shown at bottom of frame
    private JPanel inActiveQueueFilmstripsP;
    private JScrollPane inactiveQueueFilmstripsSP;

    private final QueueContent blankContent = new QueueContent();

    public QueueFrame() {
        nominalQdb = new QueueDatabase();

        activeQueueFilmstripP = new JPanel(new BorderLayout());
        activeQueueFilmstripP.setPreferredSize(activeQueueDim);
        activeQueueFilmstripP.setMaximumSize(activeQueueDim);

        activeQueueContentSP = new JScrollPane();
//        activeQueueContentSP.setPreferredSize(contentDim);

        inActiveQueueFilmstripsP = new JPanel();
        inActiveQueueFilmstripsP.setLayout(new BoxLayout(inActiveQueueFilmstripsP, BoxLayout.Y_AXIS));
        inActiveQueueFilmstripsP.setMaximumSize(inactiveQueueDim);

        inactiveQueueFilmstripsSP = new JScrollPane();
        inactiveQueueFilmstripsSP.setViewportView(inActiveQueueFilmstripsP);

        // Create and set up the frame
        setTitle("OperatorInteractionF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(frameDim);
        setPreferredSize(frameDim);

        // Add content
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().add(activeQueueFilmstripP);
        getContentPane().add(activeQueueContentSP);
        getContentPane().add(inactiveQueueFilmstripsSP);
//        // Add content
//        getContentPane().setLayout(new GridBagLayout());
//        GridBagConstraints constraints = new GridBagConstraints();
//        constraints.fill = GridBagConstraints.HORIZONTAL;
//        constraints.gridx = 0;
//        constraints.gridy = 0;
//        constraints.weightx = 1.0;
//        getContentPane().add(activeQueueFilmstripP, constraints);
//        constraints.gridy = constraints.gridy + 1;
//        getContentPane().add(activeQueueContentSP, constraints);
//        constraints.gridy = constraints.gridy + 1;
//        getContentPane().add(inactiveQueueFilmstripsSP, constraints);
//        constraints.gridy = constraints.gridy + 1;

        pack();
        setVisible(true);

        // Create decision queue panel and set it as active queue
        nominalQueuePanel = new DecisionQueuePanel(this, nominalQdb, "NOMINAL");

        queuePanels.add(nominalQueuePanel);
        moveToTop(nominalQueuePanel);

        Engine.getInstance().addListener(this);

        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    public synchronized void moveToTop(QueuePanelInt curPanel) {
        activeQueuePanel = curPanel;
        activeQueueFilmstripP.removeAll();
        inActiveQueueFilmstripsP.removeAll();
        queuePanels.remove(nominalQueuePanel);
        queuePanels.remove(curPanel);   // in case of curPanel == mainQueuePanel, fails silently
        curPanel.setIsActive(true);
        activeQueueFilmstripP.add((JComponent) curPanel);
        if (curPanel != nominalQueuePanel) {   // don't add mainQueuePanel to bottom if it's on top
            queuePanels.addFirst(nominalQueuePanel);
        }
        for (QueuePanelInt qPanel : queuePanels) {
            qPanel.setIsActive(false);
            inActiveQueueFilmstripsP.add((JComponent) qPanel);
        }
        if (curPanel != nominalQueuePanel) {   // added at front for LRU reasons
            queuePanels.addFirst(curPanel);
        }
        activeQueueContentSP.getViewport().setViewPosition(new Point(0, 0));

//        nominalQueuePanel.revalidate();
//        recoveryQueuePanel.revalidate();
        activeQueueFilmstripP.repaint();
        inActiveQueueFilmstripsP.repaint();
    }

    public void setActiveQueueContent(QueueContent content) {
        if (content == null) {
            content = blankContent;
        }
        activeQueueContentSP.setViewportView(content);
        Dimension expandedDim = content.getPreferredSize();
        expandedDim.height += 10;
        activeQueueContentSP.setPreferredSize(expandedDim);
        revalidate();
    }

    @Override
    public void toUiMessageReceived(ToUiMessage toUiMsg) {
        if (toUiMsg instanceof SelectionMessage
                || toUiMsg instanceof CreationMessage) {
            MarkupManager manager = new MarkupManager(toUiMsg);
            manager.addComponent(this);

            // Determine which database to add the message to
            //  By default, add to nominal db
            QueueDatabase qdb = nominalQdb;
            qdb.addDecision(toUiMsg, manager);
        }
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
        // If this item is in the queue, remove it as it has been handled externally (ex: MI autonomy made the decision)
        boolean removed;
        removed = nominalQdb.removeMessageId(toUiMessageId);
        if (removed) {
            LOGGER.fine("Removed 1+ items from nominal queue DB due matching ToUiMessage id: " + toUiMessageId);
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
        numRemoved = nominalQdb.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from nominal queue DB due to plan " + planManager.getPlanName() + " finishing");

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
        numRemoved = nominalQdb.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from nominal queue DB due to plan " + planManager.getPlanName() + " aborting");

        numRemoved = activeQueuePanel.removeMissionId(planManager.missionId);
        LOGGER.fine("Removed " + numRemoved + " items from " + activeQueuePanel + " due to plan " + planManager.getPlanName() + " aborting");

        for (QueuePanelInt queuePanel : queuePanels) {
            numRemoved = queuePanel.removeMissionId(planManager.missionId);
            LOGGER.fine("Removed " + numRemoved + " items from " + queuePanel + " due to plan " + planManager.getPlanName() + " aborting");
        }
    }

    @Override
    public void sharedSubPlanAtReturn(PlanManager planManager) {
    }
}
