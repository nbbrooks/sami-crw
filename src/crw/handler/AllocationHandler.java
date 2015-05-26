package crw.handler;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.resourceallocation.AllocationSolverServer;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationRequest;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationResponse;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResponseListener;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.event.input.service.AllocationResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.allocation.ResourceAllocation;
import sami.engine.Engine;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.OutputEvent;
import sami.handler.EventHandlerInt;
import sami.markup.Markup;
import sami.markup.NumberOfOptions;
import sami.mission.Token;
import sami.mission.Token.TokenType;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;

/**
 *
 * @author nbb
 */
public class AllocationHandler implements EventHandlerInt, InformationServiceProviderInt {

    private final static Logger LOGGER = Logger.getLogger(AllocationHandler.class.getName());
    private final boolean appendAllocations = true;
    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

    public AllocationHandler() {
        InformationServer.addServiceProvider(this);
    }

    @Override
    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
        LOGGER.log(Level.FINE, "ResourceAllocationInvocationHandler invoked with " + oe);
        if (oe.getId() == null) {
            LOGGER.log(Level.WARNING, "\tOutputEvent " + oe + " has null UUID");
        }

        final ResourceAllocationRequest request = new ResourceAllocationRequest();
        int numOptions = NumberOfOptions.DEFAULT_NUM_OPTIONS;
        for (Markup markup : oe.getMarkups()) {
            if (markup instanceof NumberOfOptions) {
                numOptions = ((NumberOfOptions) markup).numberOption.number;
                break;
            }
        }
        request.setNoOptions(numOptions);

        ArrayList<ITask> tasks = new ArrayList<ITask>();
        LOGGER.log(Level.FINE, "\tTokens");
        for (Token token : tokens) {
            LOGGER.log(Level.FINE, "\t\t" + token);
            if (token.getType() == TokenType.Task) {
                if (token.getProxy() != null) {
                    LOGGER.log(Level.SEVERE, "Task token passed for allocation already has a proxy assigned");
                }
                if (token.getTask() != null) {
                    tasks.add(token.getTask());
                } else {
                    LOGGER.log(Level.SEVERE, "Task token passed for allocation has no task");
                }
            }
        }

//        request.setAvailableTime(0L);
        request.setAssets(Engine.getInstance().getProxyServer().getAssetListClone());
//        request.setConstraints(null);
        request.setTasks(tasks);
//        request.setNoOptions(1);

        LOGGER.log(Level.FINE, "Submitting RA request");
        LOGGER.log(Level.FINE, "\tAssets");
        for (AbstractAsset asset : request.getAssets()) {
            LOGGER.log(Level.FINE, "\t\t" + asset);
        }
        LOGGER.log(Level.FINE, "\tTasks");
        for (ITask task : request.getTasks()) {
            LOGGER.log(Level.FINE, "\t\t" + task);
        }

        (new Thread() {
            public void run() {
                AllocationSolverServer.submitRequest(request, new ResponseListener() {
                    @Override
                    public void response(List<ResourceAllocationResponse> responses) {
                        LOGGER.log(Level.FINE, "ResponseListener recieved " + responses);
                        if (responses.size() < 1) {
                            LOGGER.log(Level.SEVERE, "ResourceAllocationResponse is empty");
                        } else {
                            // Current AllocationSolverServer cannot allocate multiple tasks to assets, so instead of finding 
                            //  an allocation for all tasks, allocate only the new, passed in tasks among the passed in proxies and then either
                            //  1- Append tasks in new allocations to proxy after its tasks from previous allocations (appendAllocations = true), OR
                            //  2- Replace the existing allocation with the new allocations, unallocating tasks assigned in previous allocations (appendAllocations = false)
                            ArrayList<ResourceAllocation> resourceAllocations = new ArrayList<ResourceAllocation>();
                            for (ResourceAllocationResponse response : responses) {
                                LOGGER.log(Level.FINE, "Adding resource allocation to list");
                                ResourceAllocation currentAllocation = Engine.getInstance().getAllocation().clone();
                                ResourceAllocation newAllocation = new ResourceAllocation(response.getAllocation(), null);
                                if (appendAllocations) {
                                    // Append new allocations after current allocations
                                    for (AbstractAsset asset : newAllocation.getAssetToTasks().keySet()) {
                                        if (currentAllocation.getAssetToTasks().containsKey(asset)) {
                                            currentAllocation.getAssetToTasks().get(asset).addAll(newAllocation.getAssetToTasks().get(asset));
                                        } else {
                                            currentAllocation.getAssetToTasks().put(asset, (ArrayList<ITask>) newAllocation.getAssetToTasks().get(asset).clone());
                                        }
                                    }
                                    currentAllocation.getUnallocatedTasks().addAll(newAllocation.getUnallocatedTasks());
                                    currentAllocation.getTaskToAsset().putAll(newAllocation.getTaskToAsset());
                                    resourceAllocations.add(currentAllocation);
                                } else {
                                    // Replace the current allocations with the new allocations, unallocating tasks assigned in previous allocations
                                    for (ITask task : currentAllocation.getTaskToAsset().keySet()) {
                                        if (!newAllocation.getTaskToAsset().containsKey(task)) {
                                            newAllocation.getUnallocatedTasks().add(task);
                                        }
                                    }
                                    resourceAllocations.add(newAllocation);
                                }
                            }
                            AllocationResponse responseEvent = new AllocationResponse(oe.getId(), oe.getMissionId(), resourceAllocations);
                            ArrayList<GeneratedEventListenerInt> listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
                            for (GeneratedEventListenerInt listener : listenersCopy) {
                                LOGGER.log(Level.FINE, "Sending responseEvent: " + responseEvent + " to listener: " + listener);
                                listener.eventGenerated(responseEvent);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "AllocationHandler offered subscription: " + sub);
        if (sub.getSubscriptionClass() == AllocationResponse.class) {
            LOGGER.log(Level.FINE, "\tAllocationHandler taking subscription: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tAllocationHandler adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tAllocationHandler incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "AllocationHandler asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == AllocationResponse.class)
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "\tAllocationHandler canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\t\tAllocationHandler removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tAllocationHandler decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}
