/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crw.event.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.event.InputEvent;
import sami.proxy.ProxyInt;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class WaitingProxiesResponse extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public WaitingProxiesResponse() {
        id = UUID.randomUUID();
    }

    public WaitingProxiesResponse(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<ProxyInt> relevantProxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }

}
