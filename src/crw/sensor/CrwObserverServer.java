package crw.sensor;

import crw.proxy.BoatProxy;
import sami.proxy.ProxyInt;
import sami.sensor.ObserverInt;
import sami.sensor.ObserverServerInt;
import sami.sensor.ObserverServerListenerInt;
import java.util.ArrayList;
import java.util.Hashtable;
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
    Hashtable<ProxyInt, Hashtable<Integer, ObserverInt>> proxyToChannelToObserver;

    public CrwObserverServer() {
        proxyToChannelToObserver = new Hashtable<ProxyInt, Hashtable<Integer, ObserverInt>>();
    }

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
                Hashtable<Integer, ObserverInt> channelToObserver;
                observers.add(observer);
                if (proxyToChannelToObserver.containsKey(source)) {
                    channelToObserver = proxyToChannelToObserver.get(source);
                } else {
                    channelToObserver = new Hashtable<Integer, ObserverInt>();
                    proxyToChannelToObserver.put(source, channelToObserver);
                }
                if (channelToObserver.containsKey(channel)) {
                    LOGGER.severe("Already have an observer for source: " + source + " and channel: " + channel + ", overwriting in Hashtable!");
                }
                channelToObserver.put(channel, observer);
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
    public ObserverInt getObserver(ProxyInt source, int channel) {
        if (proxyToChannelToObserver.containsKey(source)) {
            Hashtable<Integer, ObserverInt> channelToObserver = proxyToChannelToObserver.get(source);
            if (channelToObserver.containsKey(channel)) {
                return channelToObserver.get(channel);
            }
        }
        LOGGER.warning("Could not find observer for source: " + source + " and channel: " + channel + ", overwriting in Hashtable!");
        return null;
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
