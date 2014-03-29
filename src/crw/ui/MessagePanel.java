package crw.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import sami.engine.Engine;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import javax.swing.Timer;
import sami.markup.Keyword;
import sami.markup.Markup;
import sami.markup.Priority;
import sami.markup.RelevantProxy;
import sami.proxy.ProxyInt;
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

    private static final Logger LOGGER = Logger.getLogger(MessagePanel.class.getName());
    UiClientInt uiClient;
    UiServerInt uiServer;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList messageList;
    private final PriorityQueue<ToUiMessage> messageQueue = new PriorityQueue<ToUiMessage>(1, new MessageComparator());
    int lastSize = 0;
    Hashtable<String, ToUiMessage> keywordToNoProxyMessage = new Hashtable<String, ToUiMessage>();
    Hashtable<String, Hashtable<ProxyInt, ToUiMessage>> keywordToProxyToMessage = new Hashtable<String, Hashtable<ProxyInt, ToUiMessage>>();

    public MessagePanel() {
        initComponents();
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
        Timer updateTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkUpdateList();
            }
        });
        updateTimer.start();

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

    public void addMessage(InformationMessage infoMessage) {
        boolean rpMarkup = false, keywordMarkup = false;
        ArrayList<ProxyInt> relevantProxies = null;
        String keyword = "";
        ArrayList<ToUiMessage> messagesToRemove = new ArrayList<ToUiMessage>();

        // Look at markups
        for (Markup markup : infoMessage.getMarkups()) {
            if (markup instanceof RelevantProxy) {
                rpMarkup = true;
                relevantProxies = ((RelevantProxy) markup).getRelevantProxies();
            }
            if (markup instanceof Keyword) {
                keywordMarkup = true;
                keyword = ((Keyword) markup).textOption.text;
            }
        }

        if (keywordMarkup) {
            // If a message with this keyword already exists, replace it
            if (rpMarkup) {
                if (keywordToProxyToMessage.containsKey(keyword)) {
                    Hashtable<ProxyInt, ToUiMessage> proxyToMessage = keywordToProxyToMessage.get(keyword);
                    for (ProxyInt proxy : relevantProxies) {
                        if (proxyToMessage.containsKey(proxy)) {
                            // Remove this entry from the queue
                            messagesToRemove.add(proxyToMessage.get(proxy));
                        }
                        proxyToMessage.put(proxy, infoMessage);
                    }
                } else {
                    Hashtable<ProxyInt, ToUiMessage> proxyToMessage = new Hashtable<ProxyInt, ToUiMessage>();
                    for (ProxyInt proxy : relevantProxies) {
                        proxyToMessage.put(proxy, infoMessage);
                    }
                    keywordToProxyToMessage.put(keyword, proxyToMessage);
                }
            } else {
                if (keywordToNoProxyMessage.containsKey(keyword)) {
                    // Remove this entry from the queue
                    messagesToRemove.add(keywordToNoProxyMessage.get(keyword));
                } else {
                    keywordToNoProxyMessage.put(keyword, infoMessage);
                }
            }

        }

        synchronized (messageQueue) {
            messageQueue.removeAll(messagesToRemove);
            messageQueue.add(infoMessage);
        }
    }

    public void checkUpdateList() {
        int queueSize;
        synchronized (messageQueue) {
            queueSize = messageQueue.size();
        }
        if (lastSize != queueSize) {
            // We have a new message
            lastSize = queueSize;
            messageList.setListData(getMessageArray());
            repaint();
        }
    }

    public Object[] getMessageArray() {
        Object[] messageArray;
        synchronized (messageQueue) {
            messageArray = messageQueue.toArray();
        }
        ArrayList<Object> messageListUpdate = new ArrayList<Object>();
        boolean rpMarkup, keywordMarkup;
        ArrayList<ProxyInt> relevantProxies = null;
        String keyword = "";
        for (Object message : messageArray) {
            if (message instanceof InformationMessage) {
                InformationMessage infoMessage = (InformationMessage) message;
                rpMarkup = false;
                keywordMarkup = false;

                // Look at markups
                for (Markup markup : infoMessage.getMarkups()) {
                    if (markup instanceof RelevantProxy) {
                        rpMarkup = true;
                        relevantProxies = ((RelevantProxy) markup).getRelevantProxies();
                    }
                    if (markup instanceof Keyword) {
                        keywordMarkup = true;
                        keyword = ((Keyword) markup).textOption.text;
                    }
                }

                // Handle message
                if (keywordMarkup && rpMarkup) {
                    // May have an instance where a message with a keyword has a proxy in its RP,
                    //  but the message is not the latest keyword message for that proxy
                    if (keywordToProxyToMessage.containsKey(keyword)) {
                        Hashtable<ProxyInt, ToUiMessage> proxyToMessage = keywordToProxyToMessage.get(keyword);
                        for (ProxyInt proxy : relevantProxies) {
                            if (proxyToMessage.containsKey(proxy)) {
                                if (proxyToMessage.get(proxy) == message) {
                                    // Add the message
                                    messageListUpdate.add("<html>"
                                            + "<font color=rgb(188,6,6)>" + Priority.getPriority(infoMessage.getPriority()).toString() + "</font>&nbsp;&nbsp;&nbsp;"
                                            + "<html><font color=rgb(0,0,0)>" + proxy.getProxyName() + ": " + infoMessage.getMessage() + "</font>"
                                            + "</html>");
                                }
                            } else {
                                LOGGER.warning("keywordToProxyToMessage should have had an entry for keyword: " + keyword + " and proxy: " + proxy + ", but did not");
                            }
                        }
                    } else {
                        LOGGER.warning("keywordToProxyToMessage should have had an entry for keyword: " + keyword + ", but did not");
                    }
                } else {
                    // Just add the message
                    messageListUpdate.add("<html>"
                            + "<font color=rgb(188,6,6)>" + Priority.getPriority(infoMessage.getPriority()).toString() + "</font>&nbsp;&nbsp;&nbsp;"
                            + "<html><font color=rgb(0,0,0)>" + infoMessage.getMessage() + "</font>"
                            + "</html>");
                }
            }
        }
        Object[] ret = messageListUpdate.toArray();
        return ret;
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
