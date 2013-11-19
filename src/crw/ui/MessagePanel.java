package crw.ui;

import sami.engine.Engine;
import java.util.Comparator;
import java.util.PriorityQueue;
import sami.markup.Priority;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.InformationMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class MessagePanel extends javax.swing.JPanel implements UiClientListenerInt {

    UiClientInt uiClient;
    UiServerInt uiServer;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList messageList;
    private PriorityQueue<ToUiMessage> messageQueue = new PriorityQueue<ToUiMessage>(1, new MessageComparator());

    public MessagePanel() {
        initComponents();
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    public void fakeMessages() {
        addMessage(new InformationMessage(null, null, Priority.getPriority(Priority.Ranking.LOW), "Area exploration complete!"));
        addMessage(new InformationMessage(null, null, Priority.getPriority(Priority.Ranking.HIGH), "Boat2 battery is low!"));
    }

    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        messageList = new javax.swing.JList();
        messageList.setListData(messageQueue.toArray());
        jScrollPane1.setViewportView(messageList);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 633, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)));
    }

    public void addMessage(ToUiMessage message) {
        messageQueue.add(message);
        messageList.setListData(messageQueue.toArray());
    }

    @Override
    public void ToUiMessage(sami.uilanguage.toui.ToUiMessage m) {
        if (m instanceof InformationMessage) {
            addMessage((InformationMessage) m);
        }
    }

    public UiClientInt getUiClient() {
        return uiClient;
    }

    public void setUiClient(UiClientInt uiClient) {
        if (this.uiClient != null) {
            this.uiClient.removeClientListener(this);
        }
        this.uiClient = uiClient;
        if (uiClient != null) {
            uiClient.addClientListener(this);
        }
    }

    public UiServerInt getUiServer() {
        return uiServer;
    }

    public void setUiServer(UiServerInt uiServer) {
        this.uiServer = uiServer;
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
