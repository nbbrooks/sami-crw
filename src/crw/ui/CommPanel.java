package crw.ui;

import crw.proxy.BoatProxy;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.markup.Markup;
import sami.markup.ProxyStatus;
import sami.markup.ProxyStatus.Status;
import sami.markup.RelevantProxy;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerListenerInt;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.InformationMessage;

/**
 *
 * @author nbb
 */
public class CommPanel extends JPanel implements UiClientListenerInt, ProxyServerListenerInt {

    public Color nominalColor = Color.GREEN;
    public Color warningColor = Color.YELLOW;
    public Color severeColor = Color.RED;
    UiClientInt uiClient;
    UiServerInt uiServer;

    Hashtable<ProxyInt, JLabel> proxyToPanel = new Hashtable<ProxyInt, JLabel>();

    public CommPanel() {
        super();
        setLayout(new FlowLayout());

        Engine.getInstance().getProxyServer().addListener(this);
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    @Override
    public void proxyAdded(ProxyInt proxy) {
        if (proxy instanceof BoatProxy && !proxyToPanel.containsKey(proxy)) {
            final BoatProxy boatProxy = (BoatProxy) proxy;
            // Create marker
            JLabel proxyLabel = new JLabel(boatProxy.getProxyName());
            proxyLabel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, nominalColor));
            proxyToPanel.put(boatProxy, proxyLabel);
            add(proxyLabel);
        }
        revalidate();
    }

    @Override
    public void proxyRemoved(ProxyInt p) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void toUiMessageReceived(sami.uilanguage.toui.ToUiMessage toUiMsg) {
        boolean proxyMarkup = false;
        boolean statusMarkup = false;
        if (toUiMsg instanceof InformationMessage) {
            InformationMessage informationMessage = (InformationMessage) toUiMsg;
            for (Markup markup : informationMessage.getMarkups()) {
                if (markup instanceof RelevantProxy) {
                    proxyMarkup = true;
                } else if (markup instanceof ProxyStatus) {
                    statusMarkup = true;
                }
            }
            if (proxyMarkup && statusMarkup) {
                handleMessage(informationMessage);
            }

        }
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
    }

    public void handleMessage(InformationMessage informationMessage) {
        ArrayList<ProxyInt> relevantProxies = null;
        Status status = null;
        for (Markup markup : informationMessage.getMarkups()) {
            if (markup instanceof RelevantProxy) {
                RelevantProxy rp = (RelevantProxy) markup;
                relevantProxies = rp.getRelevantProxies();
            } else if (markup instanceof ProxyStatus) {
                status = ((ProxyStatus) markup).proxyStatus;
            }
        }

        for (ProxyInt proxy : relevantProxies) {
            if (proxyToPanel.containsKey(proxy)) {
                JLabel proxyLabel = proxyToPanel.get(proxy);
                switch (status) {
                    case NOMINAL:
                        proxyLabel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, nominalColor));
                        break;
                    case WARNING:
                        proxyLabel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, warningColor));
                        break;
                    case SEVERE:
                        proxyLabel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, severeColor));
                        break;
                }
                proxyLabel.repaint();
            }
        }
        repaint();
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
