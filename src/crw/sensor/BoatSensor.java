package crw.sensor;

import crw.proxy.BoatProxy;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.sensor.Observation;
import sami.sensor.ObservationListenerInt;
import sami.sensor.ObserverInt;
import edu.cmu.ri.crw.SensorListener;
import edu.cmu.ri.crw.VehicleServer;
import edu.cmu.ri.crw.data.SensorData;
import gov.nasa.worldwind.geom.Position;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class BoatSensor implements ObserverInt, SensorListener {

    private static final Logger LOGGER = Logger.getLogger(BoatSensor.class.getName());
    private final boolean GENERATE_FAKE_DATA = true;
    BoatProxy proxy;
    int channel;
    ArrayList<ObservationListenerInt> listeners = new ArrayList<ObservationListenerInt>();
    Hashtable<ObservationListenerInt, Integer> listenerCounter = new Hashtable<ObservationListenerInt, Integer>();

    // Stuff for simulated data creation
    static double base = 100.0;
    static double distFactor = 0.01;
    static double valueFactor = 10.0;
    static double sigmaIncreaseRate = 0.00;
    static double valueDecreaseRate = 1.00;
    static double addRate = 0.01;
    static ArrayList<Double> xs = new ArrayList<Double>();
    static ArrayList<Double> ys = new ArrayList<Double>();
    static ArrayList<Double> vs = new ArrayList<Double>();
    static ArrayList<Double> sigmas = new ArrayList<Double>();
    static boolean simpleData = false;
    static boolean hysteresis = true;
    private Position currLoc = null;

    public BoatSensor(final BoatProxy proxy, int channel) {
        this.proxy = proxy;
        this.channel = channel;
        proxy.getVehicleServer().addSensorListener(channel, this, null);

        // Cheating dummy data, another version of this is in SimpleBoatSimulator, 
        // effectively overridden by overridding addSensorListener in FastSimpleBoatSimulator
        // because no access to that code from here.
        // @todo Only should be on for simulation
        // ABHINAV COMMENT OUT THIS THREAD BEFORE RUNNING ON THE REAL BOATS!!
        if (GENERATE_FAKE_DATA) {
            (new Thread() {
                Random rand = new Random();

                public void run() {

                    LOGGER.info("Generating fake sensor data for " + proxy);

                    while (true) {

                        double[] prev = null;

                        currLoc = proxy.getPosition();
                        if (currLoc != null) {
                            SensorData sd = new SensorData();

                            // @todo Observation
                            if (rand.nextBoolean()) {
                                sd.type = VehicleServer.SensorType.TE;
                            } else {
                                sd.type = VehicleServer.SensorType.UNKNOWN;
                            }

                            sd.data = new double[4];

                            if (simpleData) {
                                for (int i = 0; i < sd.data.length; i++) {
                                    if (prev == null || !hysteresis) {
                                        sd.data[i] = Math.abs(currLoc.longitude.degrees); //  + rand.nextDouble();
                                    } else {
                                        sd.data[i] = (Math.abs(currLoc.longitude.degrees) + prev[i]) / 2.0;
                                    }
                                }
                            } else {
                                double v = computeGTValue(currLoc.latitude.degrees, currLoc.longitude.degrees);
                                // System.out.println("Created data = " + v);
                                for (int i = 0; i < sd.data.length; i++) {
                                    sd.data[i] = v;
                                }

                                synchronized (xs) {
                                    // Possibly add another
                                    if ((rand.nextDouble() < addRate && xs.size() < 20) || (xs.size() == 0)) {
                                        LOGGER.info("Added a fake sensor data point for " + proxy);
                                        double lon = currLoc.longitude.degrees + (distFactor * (rand.nextDouble() - 0.5));
                                        double lat = currLoc.latitude.degrees + (distFactor * (rand.nextDouble() - 0.5));
                                        double value = rand.nextDouble() * valueFactor;
                                        if (rand.nextBoolean()) {
                                            value = -value;
                                        }

                                        xs.add(lon);
                                        ys.add(lat);
                                        vs.add(value);
                                        sigmas.add(0.01);
                                    }

                                    // Decay 
                                    for (int i = 0; i < xs.size(); i++) {
                                        sigmas.set(i, sigmas.get(i) + sigmaIncreaseRate);
                                        vs.set(i, vs.get(i) * valueDecreaseRate);
                                        if (Math.abs(vs.get(i)) <= 0.001) {
                                            LOGGER.info("Removing a fake sensor data point from " + proxy);
                                            xs.remove(i);
                                            ys.remove(i);
                                            vs.remove(i);
                                            sigmas.remove(i);
                                            i--;
                                        }
                                    }
                                }
                            }

                            receivedSensor(sd);
                            prev = sd.data;
                        }

                        try {
                            sleep(500L);
                        } catch (InterruptedException e) {
                        }

                    }
                }
            }).start();
        }
    }

    public double computeGTValue(double lat, double lon) {
        double v = base;
        synchronized (xs) {
            for (int i = 0; i < xs.size(); i++) {

                double dx = xs.get(i) - lon;
                double dy = ys.get(i) - lat;
                double distSq = dx * dx + dy * dy;

                double dv = vs.get(i) * (1.0 / Math.sqrt(2.0 * Math.PI * sigmas.get(i) * sigmas.get(i))) * Math.pow(Math.E, -(distSq / (2.0 * sigmas.get(i) * sigmas.get(i))));
                // if (i == 0) System.out.println("Delta at dist " + Math.sqrt(distSq) + " for " + sigmas.get(i) + " is " + dv);
                v += dv;
            }
        }
        return v;
    }

    @Override
    public void addListener(ObservationListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.fine("First addition of listener " + l);
            listenerCounter.put(l, 1);
            listeners.add(l);
        } else {
            LOGGER.fine("Count is now " + (listenerCounter.get(l) + 1) + " for " + l);
            listenerCounter.put(l, listenerCounter.get(l) + 1);
        }
    }

    @Override
    public void removeListener(ObservationListenerInt l) {
        if (!listenerCounter.containsKey(l)) {
            LOGGER.log(Level.WARNING, "Tried to remove ObservationListener [" + l + "] that is not in the list!");
        } else if (listenerCounter.get(l) == 1) {
            LOGGER.fine("Last count of listener, removing " + l);
            listenerCounter.remove(l);
            listeners.remove(l);
        } else {
            LOGGER.fine(listenerCounter.get(l) - 1 + " counts remaining for " + l);
            listenerCounter.put(l, listenerCounter.get(l) - 1);
        }
    }

    @Override
    public void handleEvent(OutputEvent oe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void receivedSensor(SensorData sd) {
        Position curP = proxy.getPosition();
        Long timeReceived = System.currentTimeMillis();
        Location curLocation = new Location(curP.latitude.degrees, curP.longitude.degrees, 0);
        Observation obs = new Observation(sd.type.toString(), sd.data[0], proxy.getName(), curLocation, timeReceived);
        for (ObservationListenerInt listener : listeners) {
            listener.newObservation(obs);
        }
    }
}
