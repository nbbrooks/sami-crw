package crw.event.output.proxy;

import java.awt.Color;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class CreateSimulatedProxy extends OutputEvent {

    public String name;
    public Color color;
    public Location startLocation;
    public int numberToCreate;

    public CreateSimulatedProxy() {
        id = UUID.randomUUID();
    }
}
