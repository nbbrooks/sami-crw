package crw.ui.queue;

import sami.uilanguage.MarkupManager;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author owens
 */
public class QueueDatabase {

    private final static Logger LOGGER = Logger.getLogger(QueueDatabase.class.getName());
    UiClientInt uiClient;
    UiServerInt uiServer;
    private PriorityQueue<ToUiMessage> incomingDecisions = new PriorityQueue<ToUiMessage>(11, new MessageComparator());

    // private
    public Hashtable<ToUiMessage, MarkupManager> parentLookup = new Hashtable<ToUiMessage, MarkupManager>();

    public QueueDatabase() {
    }

    public int getHighPriorityInteractions(List<ToUiMessage> interactionList, int numToFetch) {
        int numFetched = 0;
//        LOGGER.log(Level.INFO, "Asked for interactions, have " + incomingMessages.size());
        while (!incomingDecisions.isEmpty() && (numFetched < numToFetch)) {
            interactionList.add(incomingDecisions.remove());
            numFetched++;
        }
        return numFetched;
    }

    public int getAllInteractionsCount() {
        return incomingDecisions.size();
    }

    public void addDecision(ToUiMessage decisionMessage, MarkupManager parent) {
        incomingDecisions.add(decisionMessage);
        parentLookup.put(decisionMessage, parent);
    }

    public MarkupManager getParent(ToUiMessage decisionMessage) {
        return parentLookup.get(decisionMessage);
    }

    public class MessageComparator implements Comparator<ToUiMessage> {

        @Override
        public int compare(ToUiMessage oi1, ToUiMessage oi2) {
            if (oi1 != null && oi2 != null) {
                if (oi1.getPriority() < oi2.getPriority()) {
                    return 1;
                } else if (oi1.getPriority() > oi2.getPriority()) {
                    return -1;
                } else {
                    return 0;
                }

            } else {
                // error
                return 0;
            }
        }
    }
}
