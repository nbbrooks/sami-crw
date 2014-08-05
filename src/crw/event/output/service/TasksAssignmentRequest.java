/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package crw.event.output.service;

import crw.Coordinator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.proxy.ProxyInt;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class TasksAssignmentRequest extends OutputEvent {
    
    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public LinkedList<Location> tasks;
    public Coordinator.Method method;
//    public Location proxyTasks;
    
    static {
        fieldNames.add("tasks");
        fieldNames.add("method");

        fieldNameToDescription.put("tasks", "Tasks to assign?");
        fieldNameToDescription.put("method", "Method of assignment?");
    }

    public TasksAssignmentRequest() {
        id = UUID.randomUUID();
    }
        
    public TasksAssignmentRequest(UUID uuid, UUID missionUuid, LinkedList<Location> tasks, Coordinator.Method method) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.tasks = tasks;
        this.method = method;
    }
    
    public LinkedList<Location> getTasks() {
        return tasks;
    }

    public void setTasks(LinkedList<Location> tasks) {
        this.tasks = tasks;
    }
    
    public Coordinator.Method getMethod(){
        return method;
    }
    
    public void  setMethod(Coordinator.Method method){
        this.method = method;
    }


    public String toString() {
        return "TasksSelectionRequest: tasks = " + tasks + ", method = "+method;
    }
    
}
