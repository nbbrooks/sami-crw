package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import java.util.UUID;
import sami.path.Location;

public class AssembleLocationRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Location assemblyLocation;
    public double spacing; // m

    static {
        fieldNames.add("assemblyLocation");
        fieldNames.add("spacing");

        fieldNameToDescription.put("assemblyLocation", "Location to assemble around?");
        fieldNameToDescription.put("spacing", "Spacing between boats (m)?");
    }

    public AssembleLocationRequest() {
        id = UUID.randomUUID();
    }

    public AssembleLocationRequest(UUID uuid, UUID missionUuid, Location assemblyLocation, double spacing) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.assemblyLocation = assemblyLocation;
        this.spacing = spacing;
    }

    public Location getLocation() {
        return assemblyLocation;
    }

    public void setLocation(Location assemblyLocation) {
        this.assemblyLocation = assemblyLocation;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }
    
    public String toString() {
        return "AssembleLocationRequest [" + assemblyLocation + ", " + spacing + "]";
    }
}
