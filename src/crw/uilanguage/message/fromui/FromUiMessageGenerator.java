package crw.uilanguage.message.fromui;

import crw.proxy.BoatProxy;
import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.PathOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.JComponent;
import sami.allocation.ResourceAllocation;
import sami.path.Path;
import sami.proxy.ProxyInt;
import sami.uilanguage.fromui.AllocationSelectedMessage;
import sami.uilanguage.fromui.CreationDoneMessage;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.fromui.ParamsSelectedMessage;
import sami.uilanguage.fromui.PathSelectedMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.SelectionMessage;

/**
 *
 * @author nbb
 */
public class FromUiMessageGenerator {

    private final static Logger LOGGER = Logger.getLogger(FromUiMessageGenerator.class.getName());

    private static class FromUiMessageGeneratorHolder {

        public static final FromUiMessageGenerator INSTANCE = new FromUiMessageGenerator();
    }

    private FromUiMessageGenerator() {
    }

    public static FromUiMessageGenerator getInstance() {
        return FromUiMessageGenerator.FromUiMessageGeneratorHolder.INSTANCE;
    }

    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<Field, JComponent> componentTable) {
        CreationDoneMessage doneMessage = null;
        if (creationMessage instanceof GetParamsMessage) {
            Hashtable<Field, Object> fieldToValue = new Hashtable<Field, Object>();
            for (Field field : componentTable.keySet()) {
                if (field != null) {
                    Object value = CrwUiComponentGenerator.getInstance().getComponentValue(componentTable.get(field), field);
                    if (value == null) {
                        LOGGER.severe("Got null value for field: " + field);
                    } else {
                        fieldToValue.put(field, value);
                    }
                }
            }
            doneMessage = new ParamsSelectedMessage(creationMessage.getRelevantOutputEventId(), creationMessage.getMissionId(), fieldToValue, ((GetParamsMessage) creationMessage).getFieldToEventSpec());
        }
        return doneMessage;
    }

    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, Object option) {
        FromUiMessage fromUiMessage = null;
        if (selectionMessage instanceof AllocationOptionsMessage) {
            if (option == null) {
                fromUiMessage = new AllocationSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
            } else if (option instanceof ResourceAllocation) {
                fromUiMessage = new AllocationSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (ResourceAllocation) option);
            }
        } else if (selectionMessage instanceof PathOptionsMessage) {
            if (option == null) {
                fromUiMessage = new PathSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
            } else if (option instanceof Hashtable) {
                fromUiMessage = new PathSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (Hashtable<ProxyInt, Path>) option);
            }
        } else if (selectionMessage instanceof ProxyOptionsMessage) {
            if (selectionMessage.getAllowMultiple() && (option instanceof ArrayList || option == null)) {
                // Multiple boat selection
                //@todo how to check element type of list? needs to be BoatProxy
                if (option == null) {
                    fromUiMessage = new BoatProxyListSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
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
                    fromUiMessage = new BoatProxyListSelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), selectedProxies);
                }
            } else if (!selectionMessage.getAllowMultiple() && (option instanceof BoatProxy || option == null)) {
                // Single boat selection
                if (option == null) {
                    fromUiMessage = new BoatProxySelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), null);
                } else {
                    fromUiMessage = new BoatProxySelectedMessage(selectionMessage.getRelevantOutputEventId(), selectionMessage.getMissionId(), (BoatProxy) option);
                }
            }
        }
        if (fromUiMessage == null) {
            LOGGER.warning("Got SelectionMessage " + selectionMessage + " with option " + option + ", don't know what to do with it!");
        }
        return fromUiMessage;
    }
}
