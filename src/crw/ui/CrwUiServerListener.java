package crw.ui;

import crw.event.input.operator.OperatorAcceptsAllocation;
import crw.event.input.operator.OperatorAcceptsPath;
import crw.event.input.operator.OperatorRejectsAllocation;
import crw.event.input.operator.OperatorRejectsPath;
import crw.event.input.operator.OperatorSelectsBoat;
import crw.event.input.operator.OperatorSelectsBoatList;
import crw.uilanguage.message.fromui.BoatProxyListSelectedMessage;
import crw.uilanguage.message.fromui.BoatProxySelectedMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.allocation.ResourceAllocation;
import sami.engine.Engine;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.InputEvent;
import sami.event.MissingParamsReceived;
import sami.event.NoOption;
import sami.event.YesOption;
import sami.path.Path;
import sami.proxy.ProxyInt;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.UiServerListenerInt;
import sami.uilanguage.fromui.AllocationSelectedMessage;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.fromui.ParamsSelectedMessage;
import sami.uilanguage.fromui.PathSelectedMessage;
import sami.uilanguage.fromui.YesNoSelectedMessage;

/**
 *
 * @author nbb
 */
public class CrwUiServerListener implements UiServerListenerInt, InformationServiceProviderInt {

    private static final Logger LOGGER = Logger.getLogger(CrwUiServerListener.class.getName());
    UiClientInt uiClient;
    UiServerInt uiServer;
    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public CrwUiServerListener() {
        InformationServer.addServiceProvider(this);
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());
    }

    public void FromUiMessage(FromUiMessage m) {
        LOGGER.info("@STAT CrwUiServerListener message received: " + m);
        InputEvent generatorEvent = null;

        if (m instanceof AllocationSelectedMessage) {
            ResourceAllocation ra = ((AllocationSelectedMessage) m).getAllocation();
            if (ra == null) {
                generatorEvent = new OperatorRejectsAllocation(m.getRelevantOutputEventId(), m.getMissionId());
            } else {
                generatorEvent = new OperatorAcceptsAllocation(m.getRelevantOutputEventId(), m.getMissionId(), ra);
            }
        } else if (m instanceof PathSelectedMessage) {
            Hashtable<ProxyInt, Path> proxyPaths = ((PathSelectedMessage) m).getProxyPaths();
            if (proxyPaths == null) {
                generatorEvent = new OperatorRejectsPath(m.getRelevantOutputEventId(), m.getMissionId());
            } else {
                generatorEvent = new OperatorAcceptsPath(m.getRelevantOutputEventId(), m.getMissionId(), proxyPaths);
            }
        } else if (m instanceof BoatProxySelectedMessage) {
            generatorEvent = new OperatorSelectsBoat(m.getRelevantOutputEventId(), m.getMissionId(), ((BoatProxySelectedMessage) m).getBoatProxy());
        } else if (m instanceof BoatProxyListSelectedMessage) {
            generatorEvent = new OperatorSelectsBoatList(m.getRelevantOutputEventId(), m.getMissionId(), ((BoatProxyListSelectedMessage) m).getBoatProxyList());
        } else if (m instanceof ParamsSelectedMessage) {
            ParamsSelectedMessage psm = (ParamsSelectedMessage) m;
            generatorEvent = new MissingParamsReceived(psm.getRelevantOutputEventId(), psm.getMissionId(), psm.getEventSpecToFieldValues());
//        } else if (m instanceof AreaCreatedMessage) {
//            generatorEvent = new OperatorCreatesArea(((AreaCreatedMessage) m).getArea(), ((AreaCreatedMessage) m).getUuid());
        } else if (m instanceof YesNoSelectedMessage) {
            if (((YesNoSelectedMessage) m).getYes()) {
                generatorEvent = new YesOption(m.getRelevantOutputEventId(), m.getMissionId());
            } else {
                generatorEvent = new NoOption(m.getRelevantOutputEventId(), m.getMissionId());
            }
        } else {
            LOGGER.log(Level.SEVERE, "Unhandled incoming UI event type: " + m.getClass() + ", " + m);
            return;
        }

        for (GeneratedEventListenerInt listener : listeners) {
            listener.eventGenerated(generatorEvent);
        }
    }

    @Override
    public UiClientInt getUiClient() {
        return uiClient;
    }

    @Override
    public void setUiClient(UiClientInt uiClient) {
        this.uiClient = uiClient;
    }

    @Override
    public UiServerInt getUiServer() {
        return uiServer;
    }

    @Override
    public void setUiServer(UiServerInt uiServer) {
        if (this.uiServer != null) {
            this.uiServer.removeServerListener(this);
        }
        this.uiServer = uiServer;
        if (uiServer != null) {
            uiServer.addServerListener(this);
        }
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CrwUiServerListener offered subscription: " + sub);
        if (sub.getSubscriptionClass().equals(OperatorAcceptsAllocation.class)
                || sub.getSubscriptionClass().equals(OperatorRejectsAllocation.class)
                || sub.getSubscriptionClass().equals(OperatorAcceptsPath.class)
                || sub.getSubscriptionClass().equals(OperatorRejectsPath.class)
                || sub.getSubscriptionClass().equals(OperatorSelectsBoat.class)
                || sub.getSubscriptionClass().equals(OperatorSelectsBoatList.class)
                || sub.getSubscriptionClass().equals(MissingParamsReceived.class)
                || sub.getSubscriptionClass().equals(YesOption.class)
                || sub.getSubscriptionClass().equals(NoOption.class)) {
            LOGGER.log(Level.FINE, "\tCrwUiServerListener taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tCrwUiServerListener adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tCrwUiServerListener incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CrwUiServerListener asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass().equals(OperatorAcceptsAllocation.class)
                || sub.getSubscriptionClass().equals(OperatorRejectsAllocation.class)
                || sub.getSubscriptionClass().equals(OperatorAcceptsPath.class)
                || sub.getSubscriptionClass().equals(OperatorRejectsPath.class)
                || sub.getSubscriptionClass().equals(OperatorSelectsBoat.class)
                || sub.getSubscriptionClass().equals(OperatorSelectsBoatList.class)
                || sub.getSubscriptionClass().equals(MissingParamsReceived.class)
                || sub.getSubscriptionClass().equals(YesOption.class)
                || sub.getSubscriptionClass().equals(NoOption.class))
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "\tCrwUiServerListener canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tCrwUiServerListener removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tCrwUiServerListener decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}
