/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package crw.event.output.operator;

import crw.Coordinator;
import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import sami.event.OutputEvent;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class OperatorSelectAssignmentMethod extends OutputEvent {
    
    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Coordinator.Method options;

    public OperatorSelectAssignmentMethod() {
        id = UUID.randomUUID();
    }

    public Coordinator.Method getOptions() {
        return options;
    }

    public void setOptions(Coordinator.Method options) {
        this.options = options;
    }

    //@todo ugly
    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return true;
    }
    
}
