package crw.handler;

import crw.event.input.service.BatteryCritical;
import crw.event.input.service.BatteryLow;
import crw.event.input.service.BatteryNominal;
import crw.event.output.subscription.BatteryLevelSubscription;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.OutputEvent;
import sami.handler.EventHandlerInt;
import sami.mission.Token;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;

/**
 *
 * @author nbb
 */
public class BatteryHandler implements EventHandlerInt, InformationServiceProviderInt {

    private final static Logger LOGGER = Logger.getLogger(BatteryHandler.class.getName());
    HashMap<Integer, double[]> idToSignalSettings = new HashMap<Integer, double[]>();
    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public BatteryHandler() {
        InformationServer.addServiceProvider(this);
    }

    @Override
    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
        LOGGER.log(Level.FINE, "BatteryHandler invoked with " + oe);
        if (oe.getId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null UUID");
        }

        BatteryLevelSubscription invocation = (BatteryLevelSubscription) oe;
        for (Token token : tokens) {
            if (token.getProxy() != null) {
                idToSignalSettings.put(token.getProxy().getProxyId(), new double[]{invocation.getLowFuelFraction(), invocation.getCriticalFuelFraction()});
                BatteryLevelSubscription sub = new BatteryLevelSubscription(invocation.getLowFuelFraction(), invocation.getCriticalFuelFraction(), token.getProxy().getProxyId());

                //@todo add interface with crw simulation/system
//                    Vbs2Link.getInstance().addSubscription(sub);
            }
        }
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "BatteryHandler offered subscription: " + sub);
        if (sub.getSubscriptionClass() == BatteryNominal.class
                || sub.getSubscriptionClass() == BatteryLow.class
                || sub.getSubscriptionClass() == BatteryCritical.class) {
            LOGGER.log(Level.FINE, "\tBatteryHandler taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tBatteryHandler adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tBatteryHandler incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "BatteryHandler asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == BatteryNominal.class
                || sub.getSubscriptionClass() == BatteryLow.class
                || sub.getSubscriptionClass() == BatteryCritical.class)
                && (listeners.contains(sub.getListener()))) {
            LOGGER.log(Level.FINE, "\tBatteryHandler canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tBatteryHandler removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tBatteryHandler decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}