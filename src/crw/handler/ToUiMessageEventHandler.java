package crw.handler;

import crw.event.output.operator.OperatorAllocationOptions;
import crw.event.output.operator.OperatorPathOptions;
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
import sami.event.OutputEvent;
import sami.handler.EventHandlerInt;
import sami.markup.Priority;
import sami.mission.Token;
import sami.proxy.ProxyInt;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.InformationMessage;
import sami.uilanguage.toui.ToUiMessage;

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

        if (oe instanceof OperatorPathOptions) {
            // Retreive PathOptionsMessage
            if (((OperatorPathOptions) oe).getOptions() == null) {
                LOGGER.log(Level.SEVERE, "Getting plan options message failed!");
            } else {
                message = new PathOptionsMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, ((OperatorPathOptions) oe).getOptions());
            }
        } else if (oe instanceof OperatorAllocationOptions) {
            // Retreive AllocationOptionsMessage
            if (((OperatorAllocationOptions) oe).getOptions() == null) {
                LOGGER.log(Level.SEVERE, "Getting plan options message failed!");
            } else {
                message = new AllocationOptionsMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, ((OperatorAllocationOptions) oe).getOptions());
            }
        } else if (oe instanceof OperatorSelectBoat) {
            // Retreive AllocationOptionsMessage
            ArrayList<ProxyInt> proxyOptionsList = new ArrayList<ProxyInt>();
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    proxyOptionsList.add(token.getProxy());
                }
            }
            message = new ProxyOptionsMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, proxyOptionsList, false);
        } else if (oe instanceof OperatorSelectBoatList) {
            // Retreive AllocationOptionsMessage
            ArrayList<ProxyInt> proxyOptionsList = new ArrayList<ProxyInt>();
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    proxyOptionsList.add(token.getProxy());
                }
            }
            message = new ProxyOptionsMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, proxyOptionsList, true);
//        } else if (oe instanceof OperatorCreateArea) {
//            // Retreive AllocationOptionsMessage
//            Engine.getInstance().getUiClient().UIMessage(new CreateAreaMessage(oe.getUuid(), 1));
        } else if (oe instanceof MissingParamsRequest) {
            // Retreive AllocationOptionsMessage
            MissingParamsRequest mpr = (MissingParamsRequest) oe;
            message = new GetParamsMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, mpr.getFieldDescriptions(), mpr.getFieldToEventSpec());
        } else if (oe instanceof DisplayMessage) {
            // Retreive AllocationOptionsMessage
            DisplayMessage dm = (DisplayMessage) oe;
            message = new InformationMessage(oe.getId(), oe.getMissionId(), DEFAULT_PRIORITY, dm.getMessage());
        } else {
            LOGGER.log(Level.SEVERE, "Unhandled message type: " + oe, this);
        }

        if (message == null) {
            return;
        }

        message.setMarkups(oe.getMarkups());

        Engine.getInstance().getUiClient().UIMessage(message);
    }
}