//package crw.handler;
//
//import crw.event.input.service.SignalCritical;
//import crw.event.input.service.SignalLow;
//import crw.event.input.service.SignalNominal;
//import crw.event.output.subscription.SignalStrengthSubscription;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import sami.event.GeneratedEventListenerInt;
//import sami.event.GeneratedInputEventSubscription;
//import sami.event.OutputEvent;
//import sami.handler.EventHandlerInt;
//import sami.mission.Token;
//import sami.service.information.InformationServer;
//import sami.service.information.InformationServiceProviderInt;
//
///**
// *
// * @author nbb
// */
//public class SignalHandler implements EventHandlerInt, InformationServiceProviderInt {
//
//    private final static Logger LOGGER = Logger.getLogger(SignalHandler.class.getName());
//    HashMap<Integer, double[]> idToSignalSettings = new HashMap<Integer, double[]>();
//    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
//    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();
//
//    public SignalHandler() {
//        InformationServer.addServiceProvider(this);
//    }
//
//    @Override
//    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
//        LOGGER.log(Level.FINE, "SignalHandler invoked with " + oe);
//        if (oe.getId() == null) {
//            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null UUID");
//        }
//
//        SignalStrengthSubscription invocation = (SignalStrengthSubscription) oe;
//        for (Token token : tokens) {
//            if (token.getProxy() != null) {
//                idToSignalSettings.put(token.getProxy().getProxyId(), new double[]{invocation.getLowSignalFraction(), invocation.getCriticalSignalFraction()});
//                SignalStrengthSubscription sub = new SignalStrengthSubscription(invocation.getLowSignalFraction(), invocation.getCriticalSignalFraction(), token.getProxy().getProxyId());
//
//                //@todo add interface with crw simulation/system
////                    Vbs2Link.getInstance().addSubscription(sub);
//            }
//        }
//    }
//
//    @Override
//    public boolean offer(GeneratedInputEventSubscription sub) {
//        LOGGER.log(Level.FINE, "SignalHandler offered subscription: " + sub);
//        if (sub.getSubscriptionClass() == SignalNominal.class
//                || sub.getSubscriptionClass() == SignalLow.class
//                || sub.getSubscriptionClass() == SignalCritical.class) {
//            LOGGER.log(Level.FINE, "\tSignalHandler taking subscription: " + sub);
//            if (!listeners.contains(sub.getListener())) {
//                LOGGER.log(Level.FINE, "\t\tSignalHandler adding listener: " + sub.getListener());
//                listeners.add(sub.getListener());
//                listenerGCCount.put(sub.getListener(), 1);
//            } else {
//                LOGGER.log(Level.FINE, "\t\tSignalHandler incrementing listener: " + sub.getListener());
//                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
//            }
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public boolean cancel(GeneratedInputEventSubscription sub) {
//        LOGGER.log(Level.FINE, "SignalHandler asked to cancel subscription: " + sub);
//        if (listeners.contains(sub.getListener())) {
//            if ((sub.getSubscriptionClass() == SignalNominal.class
//                    || sub.getSubscriptionClass() == SignalLow.class
//                    || sub.getSubscriptionClass() == SignalCritical.class)
//                    && listeners.contains(sub.getListener())) {
//                LOGGER.log(Level.FINE, "\tSignalHandler canceling subscription: " + sub);
//                if (listenerGCCount.get(sub.getListener()) == 1) {
//                    // Remove listener
//                    LOGGER.log(Level.FINE, "\t\tSignalHandler removing listener: " + sub.getListener());
//                    listeners.remove(sub.getListener());
//                    listenerGCCount.remove(sub.getListener());
//                } else {
//                    // Decrement garbage colleciton count
//                    LOGGER.log(Level.FINE, "\t\tSignalHandler decrementing listener: " + sub.getListener());
//                    listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
//                }
//                return true;
//            }
//        }
//        return false;
//    }
//}