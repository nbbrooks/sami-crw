package crw.event.input.service;

import sami.event.InputEvent;
import sami.allocation.ResourceAllocation;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AllocationResponse extends InputEvent {

    public List<ResourceAllocation> resourceAllocations = null;

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