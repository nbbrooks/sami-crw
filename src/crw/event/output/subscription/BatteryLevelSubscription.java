package crw.event.output.subscription;

import sami.event.OutputEvent;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class BatteryLevelSubscription extends OutputEvent {

    private double lowFuelFraction;
    private double criticalFuelFraction;
    private int proxyId;

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
}
