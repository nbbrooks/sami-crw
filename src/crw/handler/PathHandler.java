package crw.handler;

import crw.Conversion;
import crw.event.input.service.PathUtmResponse;
import crw.event.output.service.PathUtmRequest;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.engine.Engine;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.OutputEvent;
import sami.handler.EventHandlerInt;
import sami.markup.Markup;
import sami.markup.NumberOfOptions;
import sami.mission.Token;
import sami.path.DestinationUtmObjective;
import sami.path.Location;
import sami.path.Path;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;
import sami.service.pathplanning.PlanningServiceListenerInt;
import sami.service.pathplanning.PlanningServiceRequest;
import sami.service.pathplanning.PlanningServiceResponse;

/**
 * Was working here. Need to edit offer so that it accepts the event
 * subscription (if it happened, not clear that it saves - try running) and
 * calls the event, instead of just acting.
 *
 * Register as an information provider, send planning response back as
 * information.
 *
 * @author pscerri
 */
public class PathHandler implements EventHandlerInt, InformationServiceProviderInt {

    private final static Logger LOGGER = Logger.getLogger(PathHandler.class.getName());
    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public PathHandler() {
        InformationServer.addServiceProvider(this);
    }

    @Override
    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
        LOGGER.log(Level.FINE, "PathHandler invoked with " + oe);
        if (oe.getId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null UUID");
        }

        if (oe instanceof PathUtmRequest) {
            PathUtmRequest request = (PathUtmRequest) oe;
            Hashtable<ProxyInt, Location> proxyEndLocation = request.getEndLocation();
            int numOptions = NumberOfOptions.DEFAULT_NUM_OPTIONS;
            for (Markup markup : oe.getMarkups()) {
                if (markup instanceof NumberOfOptions) {
                    numOptions = ((NumberOfOptions) markup).numberOption.number;
                    break;
                }
            }

            final ArrayList<Hashtable<ProxyInt, PathUtm>> proxyPathsChoices = new ArrayList<Hashtable<ProxyInt, PathUtm>>();
            for (int i = 0; i < numOptions; i++) {
                proxyPathsChoices.add(new Hashtable<ProxyInt, PathUtm>());
            }
            final ArrayList<ProxyInt> relevantProxies = new ArrayList<ProxyInt>();
            for (final Token token : tokens) {
                if (token.getProxy() != null && token.getProxy() instanceof BoatProxy) {
                    final BoatProxy boatProxy = (BoatProxy) token.getProxy();
                    if (proxyEndLocation.containsKey(boatProxy)) {
                        LOGGER.log(Level.FINE, "\tSubmitting PlanningServiceRequest for boat proxy " + boatProxy);
                        DestinationUtmObjective objective = new DestinationUtmObjective(Conversion.positionToLocation(boatProxy.getPosition()), proxyEndLocation.get(boatProxy));
                        PlanningServiceRequest req = new PlanningServiceRequest(null, objective, null, numOptions);

                        Engine.getInstance().getServiceServer().submitPlanningRequest(req, new PlanningServiceListenerInt() {
                            @Override
                            public void responseRecieved(PlanningServiceResponse response) {
                                LOGGER.log(Level.FINE, "\tResponseListener recieved " + response);
                                if (response.getPath() instanceof PathUtm) {
                                    proxyPathsChoices.get(0).put(boatProxy, (PathUtm) response.getPath());
                                } else {
                                    LOGGER.severe("Expected PathUtm, got: " + response.getPath());
                                }
                                int counter = 1;
                                for (Path altPath : response.getAlternatives()) {
                                    if (altPath instanceof PathUtm) {
                                        proxyPathsChoices.get(counter).put(boatProxy, (PathUtm) altPath);
                                    } else {
                                        LOGGER.severe("Expected PathUtm, got: " + altPath);
                                    }
                                    counter++;
                                }
                                relevantProxies.add(boatProxy);
                                PathUtmResponse responseEvent = new PathUtmResponse(oe.getId(), oe.getMissionId(), proxyPathsChoices, relevantProxies);
                                for (GeneratedEventListenerInt listener : listeners) {
                                    LOGGER.log(Level.FINE, "\tSending response to listener: " + listener);
                                    listener.eventGenerated(responseEvent);
                                }
                            }
                        });
                    } else {
                        LOGGER.severe("No entry for proxy: " + boatProxy + " in PathUtmRequest's proxyEndLocation: " + proxyEndLocation + "!");
                    }
                }
            }
        }
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "PathHandler offered subscription: " + sub);
        if (sub.getSubscriptionClass() == PathUtmResponse.class) {
            LOGGER.log(Level.FINE, "\tPathHandler taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tPathHandler adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tPathHandler incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "PathHandler asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == PathUtmResponse.class)
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "\tPathHandler canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tPathHandler removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tPathHandler decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}
