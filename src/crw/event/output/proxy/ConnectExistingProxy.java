package crw.event.output.proxy;

import java.awt.Color;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public class ConnectExistingProxy extends OutputEvent {

    public String name;
    public Color color;
    public String server;
    public String imageStorageDirectory;

    public ConnectExistingProxy() {
        id = UUID.randomUUID();
    }
}
