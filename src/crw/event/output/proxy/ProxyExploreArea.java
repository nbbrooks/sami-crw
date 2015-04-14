package crw.event.output.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import sami.area.Area2D;
import java.util.UUID;

public class ProxyExploreArea extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Area2D area;
    // How many meters the proxy should move north after each horizontal section of the lawnmower pattern
    public double spacing;

    static {
        fieldNames.add("area");
        fieldNames.add("spacing");

        fieldNameToDescription.put("area", "Area to explore?");
        fieldNameToDescription.put("spacing", "Maximum distance between measurements? (m)");
    }

    public ProxyExploreArea() {
        id = UUID.randomUUID();
    }

    public ProxyExploreArea(UUID uuid, UUID missionUuid, Area2D area) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.area = area;
    }

    public Area2D getArea() {
        return area;
    }

    public void setArea(Area2D area) {
        this.area = area;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }

    public String toString() {
        return "ProxyExploreArea [" + area + ", " + spacing + "]";
    }
}
