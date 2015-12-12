package crw.proxy.clickthrough;

import com.perc.mitpas.adi.mission.planning.task.Task;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import crw.Conversion;
import crw.event.input.proxy.ProxyPathCompleted;
import crw.event.output.proxy.ProxyEmergencyAbort;
import crw.event.output.proxy.ProxyExecutePath;
import crw.event.output.proxy.ProxyGotoPoint;
import crw.event.output.proxy.ProxyResendWaypoints;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import robotutils.Pose3D;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import crw.event.output.proxy.BlockMovement;
import crw.event.input.proxy.BlockMovementDone;
import crw.event.output.proxy.ProxyExecutePathAndBlock;
import crw.event.output.proxy.ProxyGotoPointAndBlock;
import sami.event.BlankInputEvent;
import sami.event.TaskComplete;
import sami.event.TaskDelayed;
import sami.event.TaskReleased;
import sami.event.TaskStarted;
import sami.markup.Markup;
import sami.markup.Priority;
import sami.markup.Priority.Ranking;
import sami.path.Location;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyListenerInt;

/**
 * Simple proxy similar to BoatProxy which creates the InputEvents corresponding
 * to the OutputEvents it accepts, but doesn't actually move or generate the
 * IEs. IE generation is done via a call to generateTopInputEvent(), which will
 * then pop and publish the top sequentialInputEvent. This allows for simulating
 * SAMI execution where the operator gets to decide when the proxies finish
 * tasks.
 *
 * @author nbb
 */
public class ClickthroughProxy extends Thread implements ProxyInt {

    private static final Logger LOGGER = Logger.getLogger(ClickthroughProxy.class.getName());
    // InputEvent generation rates
    protected final int EVENT_GENERATION_TIMER = 500; // ms

    // Identifiers
    protected final String name;
    protected final Color color;
    protected final int proxyNum;
    // SAMI variables
    // OutputEvents that must occur sequentially (require movement)
    protected OutputEvent curSequentialEvent = null;
    protected final ArrayList<OutputEvent> sequentialOutputEvents = new ArrayList<OutputEvent>();
    // The resulting InputEvents from sequentialOutputEvents
    protected final ArrayList<InputEvent> sequentialInputEvents = new ArrayList<InputEvent>();
    // The first OE in sequentialOutputEvents not associated with a task
    protected OutputEvent firstNoTaskOe = null;
    // The ordered list of tasks the proxy has been assigned
    protected final ArrayList<Task> taskList = new ArrayList<Task>();
    protected Task currentTask = null;
    protected HashMap<Task, OutputEvent> taskToLastOe = new HashMap<Task, OutputEvent>();
    protected HashMap<OutputEvent, Task> oeToTask = new HashMap<OutputEvent, Task>();
    protected ArrayList<ProxyListenerInt> listeners = new ArrayList<ProxyListenerInt>();
    protected Hashtable<ProxyListenerInt, Integer> listenerCounter = new Hashtable<ProxyListenerInt, Integer>();
    //@todo need to consolidate all these different pose representations
    protected UtmPose _pose;
    protected Position position = null;
    protected UTMCoord utmCoord = null;
    protected Location location = null;
    // Waypoints
    protected final Queue<UtmPose> _curWaypoints = new LinkedList<UtmPose>();
    protected final Queue<UtmPose> _futureWaypoints = new LinkedList<UtmPose>();
    protected Iterable<Position> _curWaypointsPos = null;
    protected Iterable<Position> _futureWaypointsPos = null;
    // InputEvent generation
    protected final AtomicBoolean sendEvent = new AtomicBoolean(true);
    protected long lastTime = -1;

    // End stuff for simulated data creation
    public ClickthroughProxy(final String name, Color color, final int proxyNum) {

        String message = "Clickthrough proxy created with name: " + name + ", color: " + color;
        LOGGER.info(message);

        Timer stateTimer = new Timer(EVENT_GENERATION_TIMER, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                sendEvent.set(true);
            }
        });
        stateTimer.start();

        // this.masterURI = masterURI;
        this.name = name;
        this.color = color;
        this.proxyNum = proxyNum;
    }

    @Override
    public int getProxyId() {
        return proxyNum;
    }

    @Override
    public String getProxyName() {
        return name;
    }

    @Override
    public void addListener(ProxyListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.log(Level.FINE, "First addition of listener " + l);
            listenerCounter.put(l, 1);
            listeners.add(l);
        } else {
            LOGGER.log(Level.FINE, "Count is now " + (listenerCounter.get(l) + 1) + " for " + l);
            listenerCounter.put(l, listenerCounter.get(l) + 1);
        }
    }

    @Override
    public void removeListener(ProxyListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.log(Level.WARNING, "Tried to remove ProxyListener that is not in the list!" + l);
        } else if (listenerCounter.get(l) == 1) {
            LOGGER.log(Level.FINE, "Last count of listener, removing " + l);
            listenerCounter.remove(l);
            listeners.remove(l);
        } else {
            LOGGER.log(Level.FINE, listenerCounter.get(l) - 1 + " counts remaining for " + l);
            listenerCounter.put(l, listenerCounter.get(l) - 1);
        }
    }

    @Override
    public void run() {
    }

    @Override
    public void handleEvent(OutputEvent oe, Task task) {
        handleEvent(oe, task, false);
    }

    /**
     *
     * @param oe The output event to handle
     * @param task The task (if any) associated with the OE
     * @param useBlankInputEvent For movement related OEs, whether to pair the
     * OE with a ProxyCompletedPath IE (last waypoint) or BlankInputEvent (not
     * last waypoint)
     */
    public void handleEvent(OutputEvent oe, Task task, boolean useBlankInputEvent) {
        if (task != null && task != currentTask) {
            // OEs with a defined task should only be processed after that task becomes the current task and a corresponding TaskStarted is generated
            LOGGER.severe("Proxy [" + this + "] asked to handle event [" + oe + "] for a task [" + task + "] that is not the current task [" + currentTask + "] - ignoring");
            return;
        } else {
            // OE has no associated task or its task is the current task
            LOGGER.fine("Proxy [" + this + "] asked to handle event [" + oe + "] for task [" + task + "], current task is [" + currentTask + "]");
        }

        // Check if there is a Priority markup on this OE
        Ranking priorityRanking = null;
        for (Markup markup : oe.getMarkups()) {
            if (markup instanceof Priority) {
                priorityRanking = ((Priority) markup).ranking;
            }
        }

        // OE has no associated task or its task is the current task
        int index;
        if (task != null && taskToLastOe.containsKey(task)) {
            if (priorityRanking == Ranking.HIGH || priorityRanking == Ranking.CRITICAL) {
                // High priority command - insert OE so it is the first event for this task
                LOGGER.fine("\tPrepending task event before first OE for the task");
                //  This is for the currently executing task, so index is simply 0
                index = 0;
            } else {
                // By default append OE after last event for the task
                LOGGER.fine("\tAppending task event after last OE for the task");
                index = sequentialOutputEvents.indexOf(taskToLastOe.get(task)) + 1;
                if (index == 0) {
                    // What if taskToLastOe result not in seq OE?
                    // This is the current task, so put it at the front of the list
                    LOGGER.severe("taskToLastOe result was not in sequentialOutputEvents, appending oe being processed to sequentialOutputEvents");
                    index = 0;
                }
                taskToLastOe.put(task, oe);
            }
        } else if (task != null) {
            // This is the first OE for the currently executing task; start it now
            LOGGER.fine("\tFirst OE for this task");
            index = 0;
            taskToLastOe.put(task, oe);
        } else {
            // No task associated with the token that triggered this OE
            if (priorityRanking == Ranking.HIGH || priorityRanking == Ranking.CRITICAL) {
                // High priority command - insert OE so it is the first event without an associated task
                LOGGER.fine("\tPrepending no-task event to beginning of no-task section of OE list");
                if (firstNoTaskOe != null) {
                    index = sequentialOutputEvents.indexOf(firstNoTaskOe);
                    if (index == -1) {
                        // What if not in seq OE?
                        LOGGER.severe("firstNoTaskOe result was not in sequentialOutputEvents, appending oe being processed to sequentialOutputEvents");
                        index = sequentialOutputEvents.size();
                    }
                } else {
                    index = sequentialOutputEvents.size();
                }
                // Update firstNoTaskOe
                firstNoTaskOe = oe;
            } else {
                // By default put OE at the end of the list
                LOGGER.fine("\tAppending no-task event to end of OE list");
                index = sequentialOutputEvents.size();
                if (firstNoTaskOe == null) {
                    firstNoTaskOe = oe;
                }
            }
        }
        oeToTask.put(oe, task);

        LOGGER.fine("\t\tAdding to index [" + index + "] in list of " + sequentialOutputEvents.size());

        // If we have a current task but the front OE does not correspond to it, don't do it
        //  If this happens, the front OE should be a no-task
        //  Unless it is a ProxyResendWaypoints or ProxyEmergencyAbort event
        boolean sendWps = false;
        if (oe instanceof ProxyExecutePath) {
            System.out.println("ProxyExecutePath at index " + index);
            sequentialOutputEvents.add(index, oe);
            if (!useBlankInputEvent) {
                sequentialInputEvents.add(index, new ProxyPathCompleted(oe.getId(), oe.getMissionId(), this));
            } else {
                sequentialInputEvents.add(index, new BlankInputEvent(oe.getId(), oe.getMissionId()));
            }
        } else if (oe instanceof ProxyExecutePathAndBlock) {
            // Insert ProxyExecutePath followed by BlockMovement
            sequentialOutputEvents.add(index, new BlockMovement(oe.getMissionId()));
            sequentialOutputEvents.add(index, new ProxyExecutePath(oe.getId(), oe.getMissionId(), ((ProxyExecutePathAndBlock) oe).getProxyPaths()));
            sequentialInputEvents.add(index, new BlockMovementDone(oe.getId(), oe.getMissionId(), this));
            if (!useBlankInputEvent) {
                sequentialInputEvents.add(index, new ProxyPathCompleted(oe.getId(), oe.getMissionId(), this));
            } else {
                sequentialInputEvents.add(index, new BlankInputEvent(oe.getId(), oe.getMissionId()));
            }
        } else if (oe instanceof ProxyGotoPoint) {
            sequentialOutputEvents.add(index, oe);
            if (!useBlankInputEvent) {
                sequentialInputEvents.add(index, new ProxyPathCompleted(oe.getId(), oe.getMissionId(), this));
            } else {
                sequentialInputEvents.add(index, new BlankInputEvent(oe.getId(), oe.getMissionId()));
            }
        } else if (oe instanceof ProxyGotoPointAndBlock) {
            System.out.println("ProxyGotoPointAndBlock at index " + index);
            // Insert ProxyGotoPoint followed by BlockMovement
            sequentialOutputEvents.add(index, new BlockMovement(oe.getMissionId()));
            sequentialOutputEvents.add(index, new ProxyGotoPoint(oe.getId(), oe.getMissionId(), ((ProxyGotoPointAndBlock) oe).getProxyPoints()));
            sequentialInputEvents.add(index, new BlockMovementDone(oe.getId(), oe.getMissionId(), this));
            if (!useBlankInputEvent) {
                sequentialInputEvents.add(index, new ProxyPathCompleted(oe.getId(), oe.getMissionId(), this));
            } else {
                sequentialInputEvents.add(index, new BlankInputEvent(oe.getId(), oe.getMissionId()));
            }
        } else if (oe instanceof ProxyEmergencyAbort) {
            // Clear out all events and stop
            LOGGER.severe("Handling ProxyEmergencyAbort! sequentialOutputEvents were: " + sequentialOutputEvents + ", sequentialInputEvents were: " + sequentialInputEvents);
            sequentialOutputEvents.clear();
            sequentialInputEvents.clear();
            taskToLastOe.clear();
            firstNoTaskOe = null;
            sendWps = true;
        } else if (oe instanceof ProxyResendWaypoints) {
            sendWps = true;
        } else if (oe instanceof TaskComplete) {
            if (task == currentTask && currentTask == taskList.get(0)) {
                // Update task list and currentTask
                taskList.remove(0);
                if (!taskList.isEmpty()) {
                    // If there is a next task, start it
                    currentTask = taskList.get(0);
                    PlanManager pm = Engine.getInstance().getPlanManager(currentTask);
                    pm.eventGenerated(new TaskStarted(pm.missionId, currentTask));
                } else {
                    // Otherwise set currentTask to null so non-task OEs will run
                    currentTask = null;
                }
                sendWps = true;
            } else if (task != currentTask) {
                LOGGER.severe("Got TaskComplete event for task [" + task + "], but current task is [" + currentTask + "]");
            } else if (currentTask == taskList.get(0)) {
                LOGGER.severe("Got TaskComplete event for task [" + task + "], but currentTask [" + currentTask + "] does not match first task [" + taskList.get(0) + "]");
            }
        } else if (oe instanceof BlockMovement) {
            sequentialOutputEvents.add(index, oe);
            sequentialInputEvents.add(index, new BlockMovementDone(oe.getId(), oe.getMissionId(), this));
        } else if (oe instanceof BlockMovement) {
            // Do nothing
        } else {
            LOGGER.severe("Can't handle OutputEvent of class " + oe.getClass().getSimpleName());
        }

        // Update proxy's waypoints
        updateWaypoints(true, true);

        /// not sure about this check  - what about index 0 with non matching task
        if (sequentialOutputEvents.size() == 1
                || index == 0
                || sendWps) {
            // Send waypoints if we modified the first set of waypoints or want to resend them due to spotty comms
            LOGGER.fine("Sending waypoints to finish handling event");
            sendCurrentWaypoints();
        }

        LOGGER.fine("After handle event: sequentialOutputEvents [" + sequentialOutputEvents.toString() + "], curSequentialEvent [" + curSequentialEvent + "], taskList [" + taskList.toString() + "], currentTask [" + currentTask + "]");
    }

    @Override
    public OutputEvent getCurrentEvent() {
        return curSequentialEvent;
    }

    @Override
    public ArrayList<OutputEvent> getEvents() {
        return sequentialOutputEvents;
    }

    @Override
    public void abortEvent(UUID eventId) {
        LOGGER.info("Abort Event called with event id [" + eventId + "] on proxy [" + toString() + "]");
        int numRemoved = 0;
        ArrayList<OutputEvent> outputEventsToRemove = new ArrayList<OutputEvent>();
        ArrayList<InputEvent> inputEventsToRemove = new ArrayList<InputEvent>();
        boolean removeFirst = false, removedEvent = false;
        for (int i = 0; i < sequentialOutputEvents.size(); i++) {
            if (sequentialOutputEvents.get(i).getId() == null) {
                LOGGER.warning("Output event [" + sequentialOutputEvents.get(i) + "] on proxy [" + toString() + "] has no event id!");
            } else if (sequentialOutputEvents.get(i).getId().equals(eventId)) {
                outputEventsToRemove.add(sequentialOutputEvents.get(i));
                inputEventsToRemove.add(sequentialInputEvents.get(i));
                if (i == 0) {
                    // We need to remove the currently executing task
                    removeFirst = true;
                }
                numRemoved++;
            }
        }
        for (OutputEvent outputEvent : outputEventsToRemove) {
            sequentialOutputEvents.remove(outputEvent);
            updateOeTaskMapping();
        }
        for (InputEvent inputEvent : inputEventsToRemove) {
            sequentialInputEvents.remove(inputEvent);
        }

        updateWaypoints(removeFirst, numRemoved > 0);
        if (removeFirst) {
            // If we modified the first set of waypoints, start them
            LOGGER.info("\t Removed " + numRemoved + " events while aborting eventId [" + eventId + "] on proxy [" + toString() + "], including current event");
            sendCurrentWaypoints();
        } else {
            LOGGER.info("\t Removed " + numRemoved + " events while aborting eventId [" + eventId + "] on proxy [" + toString() + "], but not the current event");
        }
    }

    @Override
    public void addChildTask(Task parentTask, Task childTask) {
        // Add child task immediately after parent task
        if (!taskList.contains(childTask)) {
            if (taskList.contains(parentTask)) {
                int index = taskList.indexOf(parentTask);
                taskList.add(index + 1, childTask);
            } else {
                LOGGER.warning("Tried to add child task " + childTask + " to proxy " + toString() + ". but parent task " + parentTask + " is not in task list");
            }
        } else {
            LOGGER.warning("Tried to add child task " + childTask + " to proxy " + toString() + " twice");
        }
    }

    @Override
    public Task getCurrentTask() {
        return currentTask;
    }

    @Override
    public ArrayList<Task> getTasks() {
        return (ArrayList<Task>) taskList.clone();
    }

    /**
     * Takes in a new ordered list of tasks for the proxy to execute Generates a
     * TaskDelayed event if the current task is not the front task in the new
     * list Generates TaskReleased events for tasks which are not in the new
     * list Generates TaskStarted event if new list's head task is non-null and
     * not the current task
     *
     * @param tasks
     * @return
     */
    @Override
    public ArrayList<InputEvent> setTasks(ArrayList<Task> tasks) {
        LOGGER.fine("Set tasks for proxy [" + this + "] from [" + taskList.toString() + "] to [" + tasks.toString() + "]");
        // Create lists for TaskDelayed and TaskReleased events
        ArrayList<InputEvent> taskEvents = new ArrayList<InputEvent>();
        // TaskDelayed
        if (currentTask != null
                && !tasks.isEmpty()
                && currentTask != tasks.get(0)
                && tasks.contains(currentTask)) {
            // The current task is being pushed back in the task order
            LOGGER.fine("Proxy [" + this + "] interrupted first task");
            PlanManager pm = Engine.getInstance().getPlanManager(currentTask);
            taskEvents.add(new TaskDelayed(pm.missionId, currentTask));
        }
        // TaskReleased
        for (Task task : taskList) {
            if (!tasks.contains(task)) {
                LOGGER.fine("Proxy [" + this + "] adding TaskReleased event for task [" + task + "]");
                PlanManager pm = Engine.getInstance().getPlanManager(task);
                taskEvents.add(new TaskReleased(pm.missionId, this, task));
            }
        }
        // Check if we have a new current task we should send begin executing
        boolean changedFirstTask = false;
        if ((tasks.isEmpty() && currentTask != null)
                || (!tasks.isEmpty() && currentTask != tasks.get(0))) {
            LOGGER.fine("Proxy [" + this + "] changed first task");
            changedFirstTask = true;
            if (tasks.isEmpty()) {
                currentTask = null;
            } else {
                currentTask = tasks.get(0);
            }
        }
        // Update waypoint list
        taskList.clear();
        taskList.addAll(tasks);

        // update current task!
        if (changedFirstTask) {
            // Should stop the proxy unless it has no tasks, but has no-task events
            updateWaypoints(true, true);
            sendCurrentWaypoints();
            if (currentTask != null) {
                PlanManager pm = Engine.getInstance().getPlanManager(currentTask);
                taskEvents.add(new TaskStarted(pm.missionId, currentTask));
            }
        } else {
            updateWaypoints(false, true);
        }

        LOGGER.fine("After set tasks: sequentialOutputEvents [" + sequentialOutputEvents.toString() + "], curSequentialEvent [" + curSequentialEvent + "], taskList [" + taskList.toString() + "], currentTask [" + currentTask + "]");
        LOGGER.fine("After set tasks: returning taskEvent [" + taskEvents + "]");
        return taskEvents;
    }

    @Override
    public void taskCompleted(Task task) {
        if (currentTask != task) {
            LOGGER.severe("Received task complete for [" + task + "], but the current task is [" + currentTask + "]");
            return;
        }
        // Consume completed task
        taskList.remove(0);
        Engine.getInstance().taskCompleted(task);

        // Update current task
        if (!taskList.isEmpty()) {
            currentTask = taskList.get(0);
            // Send out a Task Started event
            PlanManager pm = Engine.getInstance().getPlanManager(currentTask);
            pm.eventGenerated(new TaskStarted(pm.missionId, currentTask));

            // Update proxy's waypoints
            updateWaypoints(true, true);
            sendCurrentWaypoints();
        } else {
            currentTask = null;
        }

        //@todo What if we have delayed this and already have events for this task????
    }

    @Override
    public void abortMission(UUID missionId) {
        LOGGER.info("Abort Mission called with mission id [" + missionId + "] on proxy [" + toString() + "]");
        removeMission(missionId);
    }

    @Override
    public void completeMission(UUID missionId) {
        LOGGER.info("Complete Mission called with mission id [" + missionId + "] on proxy [" + toString() + "]");
        removeMission(missionId);
    }

    /**
     * Remove all events associated with a mission and starts new events if
     * necessary
     *
     * @param missionId
     */
    private void removeMission(UUID missionId) {

        int numRemoved = 0;
        ArrayList<OutputEvent> outputEventsToRemove = new ArrayList<OutputEvent>();
        ArrayList<InputEvent> inputEventsToRemove = new ArrayList<InputEvent>();
        ArrayList<Task> tasksToRemove = new ArrayList<Task>();
        boolean removeFirst = false, removedEvent = false;
        // Remove any OE assocaited with this mission and its corresponding IE and task
        for (int i = 0; i < sequentialOutputEvents.size(); i++) {
            if (sequentialOutputEvents.get(i).getMissionId() == null) {
                LOGGER.warning("Output event [" + sequentialOutputEvents.get(i) + "] on proxy [" + toString() + "] has no mission id!");
            } else if (sequentialOutputEvents.get(i).getMissionId().equals(missionId)) {
                outputEventsToRemove.add(sequentialOutputEvents.get(i));
                inputEventsToRemove.add(sequentialInputEvents.get(i));
                if (i == 0) {
                    // We need to remove the currently executing task
                    removeFirst = true;
                }
                Task oeTask = oeToTask.get(sequentialOutputEvents.get(i));
                if (oeTask != null && !tasksToRemove.contains((oeTask))) {
                    tasksToRemove.add(oeTask);
                }
                numRemoved++;
            }
        }
        for (OutputEvent outputEvent : outputEventsToRemove) {
            sequentialOutputEvents.remove(outputEvent);
            updateOeTaskMapping();
        }
        for (InputEvent inputEvent : inputEventsToRemove) {
            sequentialInputEvents.remove(inputEvent);
        }
        ArrayList<Task> newTaskList = (ArrayList<Task>) taskList.clone();
        ArrayList<InputEvent> taskEvents = setTasks(newTaskList);

        // @todo Consolidate updateWaypoints call - some cases will have a rendundant call in setTasks
        updateWaypoints(removeFirst, removedEvent);
        if (removeFirst) {
            // If we modified the first set of waypoints, start them
            LOGGER.info("Removed " + numRemoved + " events while aborting missionId [" + missionId + "] on proxy [" + toString() + "], including current event");
            sendCurrentWaypoints();
        } else {
            LOGGER.info("Removed " + numRemoved + " events while aborting missionId [" + missionId + "] on proxy [" + toString() + "], but not the current event");
        }

        // Finally, generate any TaskStarted events resulting from the current task being associated with the removed mission
        ArrayList<PlanManager> pmList = new ArrayList<PlanManager>();
        for (InputEvent ie : taskEvents) {
            // Don't bother generating TaskCompleted or TaskDelayed as that PM should be the one that we called remove on
            if (ie instanceof TaskStarted) {
                PlanManager pm = Engine.getInstance().getPlanManager(ie.getMissionId());
                pm.eventGenerated(ie);
            }
        }
    }

    public OutputEvent getCurSequentialEvent() {
        return curSequentialEvent;
    }

    public UtmPose getUtmPose() {
        return _pose;
    }

    public Position getPosition() {
        return position;
    }

    public Location getLocation() {
        return location;
    }

    public Queue<UtmPose> getCurrentWaypoints() {
        return _curWaypoints;
    }

    public Iterable<Position> getCurrentWaypointsAsPositions() {
        return _curWaypointsPos;
    }

    public Queue<UtmPose> getFutureWaypoints() {
        return _futureWaypoints;
    }

    public Iterable<Position> getFutureWaypointsAsPositions() {
        return _futureWaypointsPos;
    }

    /**
     * Updates the variables and lists used to execute and visualize current and
     * future waypoints from the output event list. Note this does not actually
     * send waypoints to the server
     *
     * @param updateCurrent Whether to update the current event and waypoint
     * list
     * @param updateFuture Whether to update the waypoint list to be executed
     * after the current event's
     */
    public void updateWaypoints(boolean updateCurrent, boolean updateFuture) {
        // While current event is a BlockMovement and next event belongs to the same plan, consume the BlockMovement
        while (sequentialOutputEvents.size() > 1
                && sequentialOutputEvents.get(0) instanceof BlockMovement
                && sequentialOutputEvents.get(0).getMissionId() == sequentialOutputEvents.get(1).getMissionId()) {

            System.out.println("Will remove BlockMovement as next event is in same plan, list is currently: " + sequentialOutputEvents.toString());

            // Consume BlockMovement
            OutputEvent oe = sequentialOutputEvents.remove(0);
//            updateOeTaskMapping();
            // Send out the InputEvent assocaited with this path
            InputEvent ie = sequentialInputEvents.remove(0);

            LOGGER.log(Level.FINE, "ClickthroughProxy " + getName() + " completed sequential OE " + oe + ", sending out IE " + ie);
            System.out.println("ClickthroughProxy " + getName() + " completed sequential OE " + oe + ", sending out IE " + ie);
            for (ProxyListenerInt cickthroughProxyListener : listeners) {
                cickthroughProxyListener.eventOccurred(ie);
            }
        }

        // Current waypoint must be first in the list AND match the currentTask if there is one
        if (updateCurrent) {
            LOGGER.fine("Proxy [" + this + "]; updating current waypoint: oeToTask [" + oeToTask + "]");
            if (!sequentialOutputEvents.isEmpty()) {
                LOGGER.fine("Proxy [" + this + "]; current task [" + currentTask + "] and first OE [" + sequentialOutputEvents.get(0) + "]");
            }
            _curWaypointsPos = null;
            _curWaypoints.clear();
            curSequentialEvent = null;

            if (sequentialOutputEvents.isEmpty()) {
                // Have no events to process
                LOGGER.fine("Proxy [" + this + "] has no current WP, sequentialOutputEvents is empty");
            } else if (currentTask != null && oeToTask.get(sequentialOutputEvents.get(0)) != currentTask) {
                // First event doesn't belong to current event
                LOGGER.fine("Proxy [" + this + "] has no current WP, first event [" + sequentialOutputEvents.get(0) + "] is not for current task [" + currentTask + "]");
            } else {
                LOGGER.fine("Proxy [" + this + "] updating current WP with current task [" + currentTask + "] and first event [" + sequentialOutputEvents.get(0) + "]");
                // We have no current task or first event belongs to current task
                if (sequentialOutputEvents.get(0) instanceof ProxyExecutePath) {
                    ProxyExecutePath executePath = (ProxyExecutePath) sequentialOutputEvents.get(0);
                    if (executePath.getProxyPaths().containsKey(this)
                            && executePath.getProxyPaths().get(this) instanceof PathUtm) {
                        curSequentialEvent = sequentialOutputEvents.get(0);
                        PathUtm pathUtm = (PathUtm) executePath.getProxyPaths().get(this);
                        ArrayList<Position> positions = new ArrayList<Position>();
                        for (Location waypoint : pathUtm.getPoints()) {
                            positions.add(Conversion.locationToPosition(waypoint));
                        }
                        _curWaypointsPos = positions;
                        for (Position position : positions) {
                            UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                            UtmPose pose = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                            _curWaypoints.add(pose);
                        }
                        if (_curWaypoints.isEmpty()) {
                            // Add current position so waypoint complete fires
                            LOGGER.info("Constructed curWaypoints was empty, adding current proxy position");
                            UtmPose pose = new UtmPose(new Pose3D(utmCoord.getEasting(), utmCoord.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utmCoord.getZone(), utmCoord.getHemisphere().contains("North")));
                            _curWaypoints.add(pose);
                        }
                    } else {
                        if (!executePath.getProxyPaths().containsKey(this)) {
                            LOGGER.severe("Proxy Paths has no entry for this proxy: " + this + ": " + executePath.getProxyPaths());
                        } else {
                            LOGGER.severe("Can't handle Path of class " + executePath.getProxyPaths().get(this).getClass().getSimpleName());
                        }
                    }
                } else if (sequentialOutputEvents.get(0) instanceof ProxyGotoPoint) {
                    ProxyGotoPoint gotoPoint = (ProxyGotoPoint) sequentialOutputEvents.get(0);
                    if (gotoPoint.getProxyPoints().containsKey(this)) {
                        curSequentialEvent = sequentialOutputEvents.get(0);
                        Location location = gotoPoint.getProxyPoints().get(this);
                        ArrayList<Position> positions = new ArrayList<Position>();
                        positions.add(Conversion.locationToPosition(location));

                        _curWaypointsPos = positions;
                        for (Position position : positions) {
                            UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                            UtmPose pose = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                            _curWaypoints.add(pose);
                        }
                    } else {
                        LOGGER.severe("Proxy points has no entry for this proxy: " + this + ": " + gotoPoint.getProxyPoints());
                    }
                } else if (sequentialOutputEvents.get(0) instanceof BlockMovement) {
                    // Do nothing (waypoints are already cleared, which will stop the proxy's movement)
//                    LOGGER.info("Current event is  BlockMovement, list is currently: " + sequentialOutputEvents.toString());
                } else {
                    LOGGER.severe("Can't handle OutputEvent of class " + sequentialOutputEvents.get(0).getClass().getSimpleName());
                }
            }
        }

        // Future
        if (updateFuture) {
            _futureWaypoints.clear();
            _futureWaypointsPos = null;

            boolean includeFirst = !sequentialOutputEvents.isEmpty()
                    && currentTask != null
                    && oeToTask.get(sequentialOutputEvents.get(0)) != currentTask;
            if (sequentialInputEvents.isEmpty()
                    || (sequentialOutputEvents.size() == 1 && !includeFirst)) {
                LOGGER.fine("No future WPs, includeFirst [" + includeFirst + "] and sequentialInputEvents size [" + sequentialInputEvents.size() + "]");
            } else {
                int startIndex = includeFirst ? 0 : 1;
                LOGGER.fine("Updating future WPs, starting at index [" + startIndex + "] with sequentialOutputEvents of size [" + sequentialOutputEvents.size() + "]");
                ArrayList<Position> positions = new ArrayList<Position>();
                for (int i = startIndex; i < sequentialOutputEvents.size(); i++) {
                    if (sequentialOutputEvents.get(i) instanceof ProxyExecutePath) {
                        ProxyExecutePath executePath = (ProxyExecutePath) sequentialOutputEvents.get(i);
                        if (executePath.getProxyPaths().containsKey(this)
                                && executePath.getProxyPaths().get(this) instanceof PathUtm) {
                            PathUtm pathUtm = (PathUtm) executePath.getProxyPaths().get(this);
                            for (Location waypoint : pathUtm.getPoints()) {
                                positions.add(Conversion.locationToPosition(waypoint));
                            }
                        } else {
                            if (!executePath.getProxyPaths().containsKey(this)) {
                                LOGGER.severe("Proxy paths [" + executePath.getProxyPaths() + "] has no entry for proxy [" + toString() + "]!");
                            } else {
                                LOGGER.severe("Can't handle Path of class [" + executePath.getProxyPaths().get(this).getClass().getSimpleName() + "]!");
                            }
                        }
                    } else if (sequentialOutputEvents.get(i) instanceof ProxyGotoPoint) {
                        ProxyGotoPoint gotoPoint = (ProxyGotoPoint) sequentialOutputEvents.get(i);
                        if (gotoPoint.getProxyPoints().containsKey(this)) {
                            Location location = gotoPoint.getProxyPoints().get(this);
                            positions.add(Conversion.locationToPosition(location));
                        } else {
                            LOGGER.severe("Proxy points [" + gotoPoint.getProxyPoints() + "] has no entry for proxy [" + toString() + "]!");
                        }
                    } else if (sequentialOutputEvents.get(0) instanceof BlockMovement) {
                        // Do nothing
                    } else {
                        LOGGER.severe("ClickthroughProxy can't handle OutputEvent of class [" + sequentialOutputEvents.get(i).getClass().getSimpleName() + "]");
                    }
                }
                _futureWaypointsPos = positions;
                for (Position position : positions) {
                    UTMCoord utm = UTMCoord.fromLatLon(position.latitude, position.longitude);
                    UtmPose pose = new UtmPose(new Pose3D(utm.getEasting(), utm.getNorthing(), 0.0, 0.0, 0.0, 0.0), new Utm(utm.getZone(), utm.getHemisphere().contains("North")));
                    _futureWaypoints.add(pose);
                }
            }
        }

        // Notify listeners
        for (ProxyListenerInt cickthroughProxyListener : listeners) {
            cickthroughProxyListener.waypointsUpdated();
        }
    }

    /**
     * Takes in an output event: if the current sequential output event has the
     * same UUID, it is replaced with the passed in event: if not, it is
     * inserted at the beginning of the sequential list and the proxy begins
     * executing the event
     *
     * @param updatedEvent The event to replace or insert in front of the
     * current sequential event
     * @return Whether the event was handled
     */
    public boolean updateCurrentSeqEvent(OutputEvent updatedEvent) {
        LOGGER.fine("updateCurrentSeqEvent with updatedEvent: " + updatedEvent + "sequentialOutputEvents: " + sequentialOutputEvents);
        boolean handled = true;
        if (curSequentialEvent != null && curSequentialEvent.getId().equals(updatedEvent.getId())) {
            LOGGER.fine("Replace");
            // The updated event is the currently executing event, replace it
            if (updatedEvent instanceof ProxyExecutePath) {
                // Replace the existing event with the updated event and begin executing it
                sequentialOutputEvents.set(0, updatedEvent);
                updateWaypoints(true, false);
                sendCurrentWaypoints();
            } else {
                LOGGER.severe("ClickthroughProxy can't handle OutputEvent of class [" + updatedEvent.getClass().getSimpleName() + "]!");
                handled = false;
            }
        } else {
            LOGGER.fine("Insert at 0 while size is " + sequentialOutputEvents.size());
            // The updated event is not the currently executing event, insert it at the beginning of the list
            if (updatedEvent instanceof ProxyExecutePath) {
                // Insert the event and begin executing it
                sequentialOutputEvents.add(0, updatedEvent);
                sequentialInputEvents.add(0, new ProxyPathCompleted(updatedEvent.getId(), updatedEvent.getMissionId(), this));
                updateWaypoints(true, true);
                sendCurrentWaypoints();
            } else {
                LOGGER.severe("ClickthroughProxy can't handle OutputEvent of class [" + updatedEvent.getClass().getSimpleName() + "]!");
                handled = false;
            }
        }
        return handled;
    }

    /**
     * Stops and removes the current sequential output event being executed and
     * then begins the next (if applicable)
     */
    public void cancelCurrentSeqEvent() {
        if (sequentialOutputEvents.isEmpty()) {
            return;
        }
        OutputEvent removedEvent = sequentialOutputEvents.remove(0);
        updateOeTaskMapping();
        sequentialInputEvents.remove(0);

        updateWaypoints(true, true);
        sendCurrentWaypoints();
    }

    /**
     * Repopulate taskToLastOe and firstNoTaskOe
     */
    private void updateOeTaskMapping() {
        taskToLastOe.clear();
        firstNoTaskOe = null;
        Task task = null;
//        for (OutputEvent oe : sequentialOutputEvents) {
        for (int i = 0; i < sequentialOutputEvents.size(); i++) {
            OutputEvent oe = sequentialOutputEvents.get(0);
            if (oeToTask.get(oe) == null) {
                firstNoTaskOe = oe;
                // No task OEs are put at the end of sequentialOutputEvents, so we are done now
                break;
            } else if (task == null) {
                // Just started
                task = oeToTask.get(oe);
            } else if (oeToTask.get(oe) != task) {
                // At the divider between one task's OEs and the next's
                taskToLastOe.put(task, sequentialOutputEvents.get(i - 1));
                task = oeToTask.get(oe);
            } else if (i == sequentialOutputEvents.size() - 1) {
                // Final OE in sequentialOutputEvents is for final task
                taskToLastOe.put(task, oe);
            }
        }
    }

    public void sendCurrentWaypoints() {
        if (_curWaypoints.isEmpty()) {
            LOGGER.fine("stopWaypoints");
        } else {
            LOGGER.fine("sendCurrentWaypoints");
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public ArrayList<InputEvent> getSequentialInputEvents() {
        return sequentialInputEvents;
    }

    public void generateTopInputEvent() {
        if (sequentialOutputEvents.isEmpty()) {
            LOGGER.severe("Called generateTopInputEvent, but sequentialOutputEvents is empty!");
        } else {
            // Remove the OutputEvent that held this path
            OutputEvent oe = sequentialOutputEvents.remove(0);
            updateOeTaskMapping();
            // Send out the InputEvent assocaited with this path
            InputEvent ie = sequentialInputEvents.remove(0);

            updateWaypoints(true, true);

            LOGGER.log(Level.FINE, "ClickthroughProxy " + getName() + " completed sequential OE " + oe + ", sending out IE " + ie);
            for (ProxyListenerInt cickthroughProxyListener : listeners) {
                cickthroughProxyListener.eventOccurred(ie);
            }
            if (!sequentialOutputEvents.isEmpty()) {
                LOGGER.log(Level.FINE, "Sequential OE list is not empty, do the next one!");
                sendCurrentWaypoints();
            }
        }
    }
}
