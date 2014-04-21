package crw.event.output.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import sami.event.OutputEvent;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class BatteryLevelSubscription extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    private double lowFuelFraction;
    private double criticalFuelFraction;
    private int proxyId;

    static {
        fieldNames.add("lowFuelFraction");
        fieldNames.add("criticalFuelFraction");

        fieldNameToDescription.put("lowFuelFraction", "Low fuel percentange (%)?");
        fieldNameToDescription.put("criticalFuelFraction", "Critical fuel percentange (%)?");
    }

    public BatteryLevelSubscription() {
        id = UUID.randomUUID();
    }

    public BatteryLevelSubscription(double lowFuelFraction, double criticalFuelFraction, int proxyId) {
        this.lowFuelFraction = lowFuelFraction;
        this.criticalFuelFraction = criticalFuelFraction;
        this.proxyId = proxyId;
        id = UUID.randomUUID();
    }

    public double getLowFuelFraction() {
        return lowFuelFraction;
    }

    public double getCriticalFuelFraction() {
        return criticalFuelFraction;
    }

    public int getProxyId() {
        return proxyId;
    }

    public String toString() {
        return "BatteryLevelSubscription [" + lowFuelFraction + ", " + criticalFuelFraction + ", " + proxyId + "]";
    }
}
