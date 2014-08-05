package crw.event.output.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import sami.event.OutputEvent;
import java.util.UUID;
import sami.path.Location;
import sami.proxy.ProxyInt;

public class NearAssemblyLocationRequest extends OutputEvent {

//    // List of fields for which a definition should be provided
//    public static final ArrayList<String> fieldNames = new ArrayList<String>();
//    // Description for each field
//    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
//    // Fields
//    public Location assemblyLocation;
//    public double spacing; // m
//
//    static {
//        fieldNames.add("assemblyLocation");
//        fieldNames.add("spacing");
//
//        fieldNameToDescription.put("assemblyLocation", "assemblyLocation ?");
//        fieldNameToDescription.put("spacing", "Spacing between boats?");
//    }
    
        // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public Hashtable<ProxyInt, Location> proxyPoints;

    static {
        fieldNames.add("proxyPoints");

        fieldNameToDescription.put("proxyPoints", "Points for the proxies to go to?");
    }
    
    
    public NearAssemblyLocationRequest() {
        id = UUID.randomUUID();
    }

    public NearAssemblyLocationRequest(UUID uuid, UUID missionUuid, Hashtable<ProxyInt, Location> proxyPoints) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.proxyPoints = proxyPoints;
    }

    public Hashtable<ProxyInt, Location> getProxyPoints() {
        return proxyPoints;
    }

    public void setProxyPoints(Hashtable<ProxyInt, Location> proxyPoints) {
        this.proxyPoints = proxyPoints;
    }
    
    public String toString() {
        return "ProxyGotoPoint [" + proxyPoints + "]";
    }
//
//    public NearAssemblyLocationRequest() {
//        id = UUID.randomUUID();
//    }
//
//    public NearAssemblyLocationRequest(UUID uuid, UUID missionUuid, Location assemblyLocation, double spacing) {
//        this.id = uuid;
//        this.missionId = missionUuid;
//        this.assemblyLocation = assemblyLocation;
//        this.spacing = spacing;
//    }
//
//    public Location getLocation() {
//        return assemblyLocation;
//    }
//
//    public void setLocation(Location assemblyLocation) {
//        this.assemblyLocation = assemblyLocation;
//    }
//
//    public double getSpacing() {
//        return spacing;
//    }
//
//    public void setSpacing(double spacing) {
//        this.spacing = spacing;
//    }
//    
//    public String toString() {
//        return "NearAssemblyLocationRequest [" + assemblyLocation + ", " + spacing + "]";
//    }
}
