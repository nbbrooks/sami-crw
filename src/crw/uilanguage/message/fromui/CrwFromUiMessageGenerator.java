package crw.uilanguage.message.fromui;

import crw.Coordinator;
import crw.proxy.BoatProxy;
import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.PathOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.allocation.ResourceAllocation;
import sami.event.ReflectedEventSpecification;
import sami.path.Path;
import sami.proxy.ProxyInt;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.fromui.FromUiMessageGeneratorInt;
import sami.uilanguage.fromui.AllocationSelectedMessage;
import sami.uilanguage.fromui.CreationDoneMessage;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.fromui.ParamsSelectedMessage;
import sami.uilanguage.fromui.PathSelectedMessage;
import sami.uilanguage.fromui.YesNoSelectedMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.MethodOptionMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.YesNoOptionsMessage;

/**
 *
 * @author nbb
 */
public class CrwFromUiMessageGenerator implements FromUiMessageGeneratorInt {

    private final static Logger LOGGER = Logger.getLogger(CrwFromUiMessageGenerator.class.getName());

    private static class FromUiMessageGeneratorHolder {

        public static final CrwFromUiMessageGenerator INSTANCE = new CrwFromUiMessageGenerator();
    }

    private CrwFromUiMessageGenerator() {
    }

    public static CrwFromUiMessageGenerator getInstance() {
        return CrwFromUiMessageGenerator.FromUiMessageGeneratorHolder.INSTANCE;
    }

    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>> eventSpecToComponentTable) {
        CreationDoneMessage doneMessage = null;
        if (creationMessage instanceof GetParamsMessage) {
            LOGGER.fine("eventSpecToComponentTable: " + eventSpecToComponentTable);
            Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues = new Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>>();
            for (ReflectedEventSpecification eventSpec : eventSpecToComponentTable.keySet()) {
                Hashtable<Field, Object> fieldToValue = new Hashtable<Field, Object>();
                eventSpecToFieldValues.put(eventSpec, fieldToValue);
                Hashtable<Field, MarkupComponent> componentTable = eventSpecToComponentTable.get(eventSpec);
                for (Field field : componentTable.keySet()) {
                    if (field != null) {
                        Object value = CrwUiComponentGenerator.getInstance().getComponentValue(componentTable.get(field), field.getType());
                        if (value == null) {
                            LOGGER.severe("Got null value for field: " + field);
                        } else {
                            fieldToValue.put(field, value);
                        }
                    }
                }
            }
            doneMessage = new ParamsSelectedMessage(creationMessage.getMessageId(), creationMessage.getRelevantOutputEventId(), creationMessage.getMissionId(), eventSpecToFieldValues);
        }
        return doneMessage;
    }

    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, Object option) {
        FromUiMessage fromUiMessage = null;
        if (selectionMessage instanceof AllocationOptionsMessage) {
            if (option == null) {
                fromUiMessage = new AllocationSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
            } else if (option instanceof ResourceAllocation) {
                fromUiMessage = new AllocationSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (ResourceAllocation) option);
            }
        } else if (selectionMessage instanceof PathOptionsMessage) {
            if (option == null) {
                fromUiMessage = new PathSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
            } else if (option instanceof Hashtable) {
                fromUiMessage = new PathSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (Hashtable<ProxyInt, Path>) option);
            }
        } else if (selectionMessage instanceof MethodOptionMessage){
            fromUiMessage = new MethodOptionSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (Coordinator.Method) option);
        } else if (selectionMessage instanceof ProxyOptionsMessage) {
            if (selectionMessage.getAllowMultiple() && (option instanceof ArrayList || option == null)) {
                // Multiple boat selection
                //@todo how to check element type of list? needs to be BoatProxy

                if (option == null) {
                    fromUiMessage = new BoatProxyListSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
                } else {
                    ArrayList<?> list = (ArrayList<?>) option;
                    ArrayList<BoatProxy> selectedProxies = new ArrayList<BoatProxy>();
                    for (Object object : list) {
                        if (object instanceof BoatProxy) {
                            selectedProxies.add((BoatProxy) object);
                        } else {
                            LOGGER.warning("List contained something other than BoatProxy!");
                        }
                    }
                    fromUiMessage = new BoatProxyListSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), selectedProxies);
                }
            } else if (!selectionMessage.getAllowMultiple() && (option instanceof BoatProxy || option == null)) {
                // Single boat selection
                if (option == null) {
                    fromUiMessage = new BoatProxySelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
                } else {
                    fromUiMessage = new BoatProxySelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (BoatProxy) option);
                }
            }
        } else if (selectionMessage instanceof MethodOptionMessage) {
            fromUiMessage = new MethodOptionSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMessageId(), (Coordinator.Method) option);
            
        } else if (selectionMessage instanceof YesNoOptionsMessage) {
            if (selectionMessage.getOptionsList().get(0) == option) {
                fromUiMessage = new YesNoSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), true);
            } else {
                fromUiMessage = new YesNoSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), false);
            }
        } 
        if (fromUiMessage == null) {
            LOGGER.warning("Got SelectionMessage " + selectionMessage + " with option " + option + ", don't know what to do with it!");
        }
        return fromUiMessage;
    }

    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, int optionIndex) {
        if (selectionMessage.getOptionsList().size() > optionIndex) {
            return getFromUiMessage(selectionMessage, selectionMessage.getOptionsList().get(optionIndex));
        } else {
            LOGGER.severe("Got SelectionMessage " + selectionMessage + " with option index " + optionIndex + ", but only have " + selectionMessage.getOptionsList().size() + " options!");
            return null;
        }
    }
}
