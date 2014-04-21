package crw.event.output.proxy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class ConnectExistingProxy extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public String name;
    public Color color;
    public String server;
    public String imageStorageDirectory;

    static {
        fieldNames.add("name");
        fieldNames.add("color");
        fieldNames.add("server");
        fieldNames.add("imageStorageDirectory");

        fieldNameToDescription.put("name", "Name of the proxy?");
        fieldNameToDescription.put("color", "Visualization color for the proxy?");
        fieldNameToDescription.put("server", "IP address (with port number) of proxy?");
        fieldNameToDescription.put("imageStorageDirectory", "Where to store images taken by this proxy?");
    }

    public ConnectExistingProxy() {
        id = UUID.randomUUID();
    }

    public String toString() {
        return "ConnectExistingProxy [" + name + ", " + color + ", " + server + ", " + imageStorageDirectory + "]";
    }
}
