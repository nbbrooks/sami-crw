package crw.uilanguage.message.fromui;

import crw.proxy.BoatProxy;
import crw.proxy.clickthrough.ClickthroughProxy;
import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.PathOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.allocation.ResourceAllocation;
import sami.engine.Engine;
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
import sami.uilanguage.fromui.VariablesDefinedMessage;
import sami.uilanguage.fromui.YesNoSelectedMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.GetVariablesMessage;
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

    @Override
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>> eventSpecToComponentTable) {
        if(eventSpecToComponentTable == null) {
            LOGGER.severe("Tried to create FromUIMessage from NULL eventSpecToComponentTable");
            return null;
        }
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
    

    @Override
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<Field, MarkupComponent> fieldToComponentTable, int erasureThrowaway) {
        if(fieldToComponentTable == null) {
            LOGGER.severe("Tried to create FromUIMessage from NULL fieldToComponentTable");
            return null;
        }
        CreationDoneMessage doneMessage = null;
        LOGGER.fine("fieldToComponentTable: " + fieldToComponentTable);
        Hashtable<Field, Object> fieldToValue = new Hashtable<Field, Object>();
        for (Field field : fieldToComponentTable.keySet()) {
            if (field != null) {
                Object value = CrwUiComponentGenerator.getInstance().getComponentValue(fieldToComponentTable.get(field), field.getType());
                if (value == null) {
                    LOGGER.severe("Got null value for field: " + field);
                } else {
                    fieldToValue.put(field, value);
                }
            }
            doneMessage = new CreationDoneMessage(creationMessage.getMessageId(), creationMessage.getRelevantOutputEventId(), creationMessage.getMissionId(), fieldToValue, 0);
        }
        return doneMessage;
    }

    @Override
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<String, MarkupComponent> variableNameToComponentTable, int erasureThrowaway1, int erasureThrowaway2) {
        if(variableNameToComponentTable == null) {
            LOGGER.severe("Tried to create FromUIMessage from NULL variableNameToComponentTable");
            return null;
        }
        CreationDoneMessage doneMessage = null;
        if (creationMessage instanceof GetVariablesMessage) {
            LOGGER.fine("variableNameToComponentTable: " + variableNameToComponentTable);
            Hashtable<String, Object> variableToValue = new Hashtable<String, Object>();
            for (String variableName : variableNameToComponentTable.keySet()) {
                Object curValue = Engine.getInstance().getVariableValue(variableName, Engine.getInstance().getPlanManager(creationMessage.getMissionId()));
                Class variableClass = curValue.getClass();
                Object value = CrwUiComponentGenerator.getInstance().getComponentValue(variableNameToComponentTable.get(variableName), variableClass);
                if (value == null) {
                    LOGGER.severe("Got null value for variable name: " + variableName);
                } else {
                    variableToValue.put(variableName, value);
                }
            }
            doneMessage = new VariablesDefinedMessage(creationMessage.getMessageId(), creationMessage.getRelevantOutputEventId(), creationMessage.getMissionId(), variableToValue);
        }
        return doneMessage;
    }

    @Override
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
        } else if (selectionMessage instanceof ProxyOptionsMessage) {
            if (selectionMessage.getAllowMultiple() && (option instanceof ArrayList || option == null)) {
                // Multiple boat selection
                //@todo how to check element type of list? needs to be BoatProxy

                if (option == null) {
                    fromUiMessage = new BoatProxyListSelectedMessage(selectionMessage.getMessageId(), selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
                } else {
                    ArrayList<?> list = (ArrayList<?>) option;
                    ArrayList<ProxyInt> selectedProxies = new ArrayList<ProxyInt>();
                    for (Object object : list) {
                        if (object instanceof BoatProxy || object instanceof ClickthroughProxy) {
                            selectedProxies.add((ProxyInt)object);
                        } else {
                            LOGGER.warning("List contained something other than BoatProxy and ClickthroughProxy!");
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

    @Override
    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, int optionIndex) {
        if (selectionMessage.getOptionsList().size() > optionIndex) {
            return getFromUiMessage(selectionMessage, selectionMessage.getOptionsList().get(optionIndex));
        } else {
            LOGGER.severe("Got SelectionMessage " + selectionMessage + " with option index " + optionIndex + ", but only have " + selectionMessage.getOptionsList().size() + " options!");
            return null;
        }
    }
}
