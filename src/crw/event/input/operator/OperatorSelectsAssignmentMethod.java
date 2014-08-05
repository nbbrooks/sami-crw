/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package crw.event.input.operator;

import crw.Coordinator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class OperatorSelectsAssignmentMethod extends InputEvent {
    
     // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    
    public Coordinator.Method method;
    
       static {
        variableNames.add("method");

        variableNameToDescription.put("method", "Returned method.");
    }
    public OperatorSelectsAssignmentMethod() {
        id = UUID.randomUUID();
    }

    public OperatorSelectsAssignmentMethod(UUID relevantOutputEventUuid, UUID missionUuid, Coordinator.Method method) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        this.method = method;
    }

    public Coordinator.Method getMethod() {
        return method;
    }

}
