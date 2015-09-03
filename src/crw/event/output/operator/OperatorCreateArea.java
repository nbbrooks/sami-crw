package crw.event.output.operator;

import crw.event.input.operator.OperatorCreatedArea;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OperatorCreateOutputEvent;

/**
 *
 * @author nbb
 */
public class OperatorCreateArea extends OperatorCreateOutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    // This is an OperatorCreateOutputEvent, fields are instead in the getInputEventClass() class

    public OperatorCreateArea() {
        id = UUID.randomUUID();
    }

    @Override
    public Class getInputEventClass() {
        return OperatorCreatedArea.class;
    }

    @Override
    public String toString() {
        return "OperatorCreateArea";
    }
}
