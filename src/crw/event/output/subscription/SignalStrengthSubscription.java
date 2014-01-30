//package crw.event.output.subscription;
//
//import sami.event.OutputEvent;
//import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
//import java.util.UUID;
//
///**
// *
// * @author nbb
// */
//public class SignalStrengthSubscription extends OutputEvent {
//
//    private double lowSignalFraction;
//    private double criticalSignalFraction;
//    private int proxyId;
//
//    public SignalStrengthSubscription() {
//        id = UUID.randomUUID();
//    }
//
//    public SignalStrengthSubscription(double lowSignalFraction, double criticalSignalFraction, int proxyId) {
//        this.lowSignalFraction = lowSignalFraction;
//        this.criticalSignalFraction = criticalSignalFraction;
//        this.proxyId = proxyId;
//        id = UUID.randomUUID();
//    }
//
//    public double getLowSignalFraction() {
//        return lowSignalFraction;
//    }
//
//    public double getCriticalSignalFraction() {
//        return criticalSignalFraction;
//    }
//
//    public int getProxyId() {
//        return proxyId;
//    }
//}
