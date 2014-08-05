/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crw.handler;

import crw.event.output.proxy.ProxyEmergencyAbort;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.InterruptEventIE;
import sami.event.InterruptEventOE;
import sami.event.OutputEvent;
import sami.event.ProxyInterruptEventIE;
import sami.event.ProxyInterruptEventOE;
import sami.handler.EventHandlerInt;
import sami.mission.Token;
import sami.proxy.ProxyInt;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class InterruptEventHandler implements EventHandlerInt, InformationServiceProviderInt {

    private final static Logger LOGGER = Logger.getLogger(InterruptEventHandler.class.getName());

    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public InterruptEventHandler() {
        LOGGER.log(Level.FINE, "Adding InterruptHandler as service provider");

        InformationServer.addServiceProvider(this);
    }

    public void invoke(OutputEvent oe, ArrayList<Token> tokens) {

        LOGGER.log(Level.FINE, "InterruptHandler invoked with " + oe);
        if (oe.getId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null event id");
        }
        if (oe.getMissionId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null mission id");
        }

        if (oe instanceof InterruptEventOE) {

//            JOptionPane.showMessageDialog(null, "INTERRUPT HANDLER!!");

        } else if (oe instanceof ProxyInterruptEventOE) {

            ArrayList<ProxyInt> tokenProxies = new ArrayList<ProxyInt>();

            int numProxies = 0;

            for (Token token : tokens) {
                if (token.getProxy() != null && token.getProxy() instanceof BoatProxy) {
                    tokenProxies.add((ProxyInt) token.getProxy());
                    numProxies++;
                }
            }
//
//            for (ProxyInt b : tokenProxies) {
//                b.handleEvent(new ProxyEmergencyAbort(oe.getId(), oe.getMissionId()));
//
//            }

            //SINGLE RESPONSE EVENT
            ProxyInterruptEventIE response = new ProxyInterruptEventIE(oe.getId(), oe.getMissionId(), tokenProxies);

            for (GeneratedEventListenerInt listener : listeners) {
                LOGGER.log(Level.INFO, "\tSending response: " + response + " to listener: " + listener);
                listener.eventGenerated(response);
            }

        }

    }

    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "InterruptEventHandler offered subscription: " + sub);
        if (sub.getSubscriptionClass() == InterruptEventIE.class
                || sub.getSubscriptionClass() == ProxyInterruptEventIE.class) {
            LOGGER.log(Level.FINE, "\tInterruptEventHandler taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tInterruptEventHandler adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tInterruptEventHandler incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "InterruptEventHandler asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == InterruptEventIE.class
                || sub.getSubscriptionClass() == ProxyInterruptEventIE.class)
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "\tInterruptEventHandler canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tInterruptEventHandler removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tInterruptEventHandler decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }

}
