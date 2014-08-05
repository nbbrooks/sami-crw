/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package crw.event.input.service;

import static crw.event.input.service.PathUtmResponse.fieldNameToDescription;
import static crw.event.input.service.PathUtmResponse.fieldNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.InputEvent;
import sami.path.Location;
import sami.path.Path;
import sami.path.PathUtm;
import sami.proxy.ProxyInt;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class TasksAssignmentResponse extends InputEvent {
    
     // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public Hashtable<ProxyInt, Path> computedPaths = null;

    static {

        variableNames.add("computedPaths");

        variableNameToDescription.put("computedPaths", "Returned assignment options.");
    }

    public TasksAssignmentResponse() {
    }

    public TasksAssignmentResponse(UUID relevantOutputEventUuid, UUID missionUuid, Hashtable<ProxyInt, Path> computedPaths, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.computedPaths = computedPaths;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
    
    public Hashtable<ProxyInt, Path> getPaths() {
        return computedPaths;
    }

    public void setPaths(Hashtable<ProxyInt, Path> computedPaths) {
        this.computedPaths = computedPaths;
    }
    
}
