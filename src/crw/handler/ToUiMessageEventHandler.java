package crw.handler;

import crw.Coordinator;
import crw.event.output.operator.OperatorAllocationOptions;
import crw.event.output.operator.OperatorPathOptions;
import crw.event.output.operator.OperatorSelectAssignmentMethod;
import crw.event.output.operator.OperatorSelectBoat;
import crw.event.output.operator.OperatorSelectBoatList;
import crw.event.output.ui.DisplayMessage;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.PathOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.engine.Engine;
import sami.event.MissingParamsRequest;
import sami.event.OperatorApprove;
import sami.event.OutputEvent;
import sami.handler.EventHandlerInt;
import sami.markup.Markup;
import sami.markup.Priority;
import sami.markup.RelevantArea;
import sami.markup.RelevantProxy;
import sami.mission.Token;
import sami.proxy.ProxyInt;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.InformationMessage;
import sami.uilanguage.toui.MethodOptionMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;
import sami.uilanguage.toui.YesNoOptionsMessage;

/**
 *
 * @author nbb
 */
public class ToUiMessageEventHandler implements EventHandlerInt {

    private final static Logger LOGGER = Logger.getLogger(ToUiMessageEventHandler.class.getName());
    private static final int DEFAULT_PRIORITY = Priority.getPriority(Priority.Ranking.LOW);

    @Override
    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
        LOGGER.log(Level.FINE, "ToUiMessageEventHandler invoked with " + oe);
        ToUiMessage message = null;

        // list of things to viz
        // list of things to create
        // list of markups
        // how would teleop be handled? markup?
        if (oe.getId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent has null UUID: " + oe);
        }
        int priority = DEFAULT_PRIORITY;
        for (Markup markup : oe.getMarkups()) {
            if (markup instanceof Priority) {
                priority = Priority.priorityToInt.get(((Priority) markup).ranking);
            }
        }

        if (oe instanceof OperatorPathOptions) {
            // Retreive PathOptionsMessage
            if (((OperatorPathOptions) oe).getOptions() == null) {
                LOGGER.log(Level.SEVERE, "Getting plan options message failed!");
            } else {
                message = new PathOptionsMessage(oe.getId(), oe.getMissionId(), priority, ((OperatorPathOptions) oe).getOptions());
            }
        } else if (oe instanceof OperatorAllocationOptions) {
            // Retreive AllocationOptionsMessage
            if (((OperatorAllocationOptions) oe).getOptions() == null) {
                LOGGER.log(Level.SEVERE, "Getting plan options message failed!");
            } else {
                message = new AllocationOptionsMessage(oe.getId(), oe.getMissionId(), priority, ((OperatorAllocationOptions) oe).getOptions());
            }
        } else if (oe instanceof OperatorSelectBoat) {
            // Retreive AllocationOptionsMessage
            ArrayList<ProxyInt> proxyOptionsList = new ArrayList<ProxyInt>();
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    proxyOptionsList.add(token.getProxy());
                }
            }
            message = new ProxyOptionsMessage(oe.getId(), oe.getMissionId(), priority, false, proxyOptionsList);
        } else if (oe instanceof OperatorSelectBoatList) {
            // Retreive AllocationOptionsMessage
            ArrayList<ProxyInt> proxyOptionsList = new ArrayList<ProxyInt>();
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    proxyOptionsList.add(token.getProxy());
                }
            }
            message = new ProxyOptionsMessage(oe.getId(), oe.getMissionId(), priority, true, proxyOptionsList);
        } else if (oe instanceof OperatorSelectAssignmentMethod) {
            
            ArrayList<Coordinator.Method> methods = new ArrayList<Coordinator.Method>();
            methods.add(Coordinator.Method.EASY); methods.add(Coordinator.Method.COST);
            
            message = new MethodOptionMessage(oe.getId(), oe.getMissionId(), priority, false, methods);
                        
        } else if (oe instanceof MissingParamsRequest) {
            // Retreive AllocationOptionsMessage
            MissingParamsRequest mpr = (MissingParamsRequest) oe;
            message = new GetParamsMessage(oe.getId(), oe.getMissionId(), priority, mpr.getFieldDescriptions());
        } else if (oe instanceof DisplayMessage) {
            // Retreive AllocationOptionsMessage
            DisplayMessage dm = (DisplayMessage) oe;
            message = new InformationMessage(oe.getId(), oe.getMissionId(), priority, dm.getMessage());
        } else if (oe instanceof OperatorApprove) {
            // Retreive AllocationOptionsMessage
            OperatorApprove oa = (OperatorApprove) oe;
            message = new YesNoOptionsMessage(oe.getId(), oe.getMissionId(), priority);
        } else {
            LOGGER.log(Level.SEVERE, "Unhandled message type: " + oe, this);
        }

        if (message == null) {
            return;
        }

        // Handle markups
        for (Markup markup : oe.getMarkups()) {
            if (markup instanceof RelevantProxy) {
                // Needs to be copied
                RelevantProxy copy = ((RelevantProxy) markup).copy();
                ArrayList<ProxyInt> relevantProxies = new ArrayList<ProxyInt>();
                for (Token t : tokens) {
                    if (t.getProxy() != null && !relevantProxies.contains(t.getProxy())) {
                        relevantProxies.add(t.getProxy());
                    }
                }
                copy.setRelevantProxies(relevantProxies);
                message.addMarkup(copy);
            } else if (markup instanceof RelevantArea) {
                // Needs to be copied
                //@todo implement
                RelevantArea copy = ((RelevantArea) markup).copy();
                message.addMarkup(copy);
            } else {
                message.addMarkup(markup);
            }
        }
        Engine.getInstance().getUiClient().toUiMessageReceived(message);
    }
}
