package crw.event.output.ui;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import java.util.UUID;

public class DisplayMessage extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public String message;

    static {
        fieldNames.add("message");

        fieldNameToDescription.put("message", "Message to display?");
    }

    public DisplayMessage() {
        id = UUID.randomUUID();
    }

    public String getMessage() {
        return message;
    }
    
    public String toString() {
        return "DisplayMessage [" + message + "]";
    }
}
