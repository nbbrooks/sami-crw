package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.InputEvent;
import sami.allocation.ResourceAllocation;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AllocationResponse extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public List<ResourceAllocation> resourceAllocations = null;

    static {
        variableNames.add("resourceAllocations");

        variableNameToDescription.put("resourceAllocations", "Returned task allocation options.");
    }

    public AllocationResponse() {
        id = UUID.randomUUID();
    }

    public AllocationResponse(UUID relevantOutputEventUuid, UUID missionUuid, List<ResourceAllocation> resourceAllocations) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.resourceAllocations = resourceAllocations;
        id = UUID.randomUUID();
    }

    public List<ResourceAllocation> getResourceAllocations() {
        return resourceAllocations;
    }

    public void setResourceAllocations(List<ResourceAllocation> resourceAllocations) {
        this.resourceAllocations = resourceAllocations;
    }
}