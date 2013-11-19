package crw.sensor;

import crw.proxy.BoatProxy;
import sami.proxy.ProxyInt;
import sami.sensor.ObserverInt;
import sami.sensor.ObserverServerInt;
import sami.sensor.ObserverServerListenerInt;
import edu.cmu.ri.crw.SensorListener;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class CrwObserverServer implements ObserverServerInt {
    
    private final static Logger LOGGER = Logger.getLogger(CrwObserverServer.class.getName());
    private static ArrayList<ObserverServerListenerInt> listeners = new ArrayList<ObserverServerListenerInt>();
    private int observerCounter = 1;
    ArrayList<ObserverInt> observers = new ArrayList<ObserverInt>();
    
    @Override
    public void addListener(ObserverServerListenerInt l) {
        listeners.add(l);
    }
    
    @Override
    public void removeListener(ObserverServerListenerInt l) {
        listeners.remove(l);
    }
    
    @Override
    public void createObserver(ProxyInt source, int channel) {
        if (source instanceof BoatProxy) {
            try {
                BoatProxy boatProxy = (BoatProxy) source;
                BoatSensor observer = new BoatSensor(boatProxy, channel);
                observerCounter++;
                observers.add(observer);
                boatProxy.addSensorListener(channel, observer);
                
                for (ObserverServerListenerInt l : listeners) {
                    l.observerAdded(observer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public ArrayList<ObserverInt> getObserverListClone() {
        return (ArrayList<ObserverInt>) observers.clone();
    }
    
    @Override
    public void remove(ObserverInt observer) {
        observers.remove(observer);
    }
    
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
