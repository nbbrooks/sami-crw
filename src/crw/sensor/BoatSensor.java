package crw.sensor;

import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.SensorData;
import crw.proxy.BoatProxy;
import sami.event.OutputEvent;
import sami.path.Location;
import sami.sensor.Observation;
import sami.sensor.ObservationListenerInt;
import sami.sensor.ObserverInt;
import gov.nasa.worldwind.geom.Position;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.CoreHelper;

/**
 *
 * @author nbb
 */
public class BoatSensor implements ObserverInt, SensorListener {

		private static final Logger LOGGER = Logger.getLogger(BoatSensor.class.getName());

		// Copied over sensor value handling from Shantanu's tablet-debug code but it causes parsing errors so far as I know
		public static final boolean NEW_SENSOR_FORMAT = false;

		// For data visualization testing only!
		// Do NOT commit this set to true
		private final boolean GENERATE_FAKE_DATA = false;

		BoatProxy proxy;
		int channel;
		final String sensorName;
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
		static boolean randomData = true;
		static boolean simpleData = false;
		static boolean hysteresis = true;
		private Position currLoc = null;

		static final int FAKE_ES2_CHANNEL = 3, FAKE_ES2_MIN = 100, FAKE_ES2_MAX = 600;
		static final int FAKE_PH_CHANNEL = 2, FAKE_PH_MIN = 5, FAKE_PH_MAX = 8;
		static final int FAKE_DO_CHANNEL = 1, FAKE_DO_MIN = 4, FAKE_DO_MAX = 12;
		static final int FAKE_BATTERY_CHANNEL = 4, FAKE_BATTERY_MIN = 4, FAKE_BATTERY_MAX = 12;

		private final AtomicBoolean loggingReady = new AtomicBoolean(false);
		public static final String LOG_DIRECTORY = "run/logs/" + CoreHelper.LOGGING_TIMESTAMP + "/";
		private static FileWriter writer;

		public static void setUpLogging() {
				LOGGER.info("Log directory is " + LOG_DIRECTORY);
				try {
						// Create directory
						new File(LOG_DIRECTORY).mkdir();
						// Add log file
						writer = new FileWriter(new File(LOG_DIRECTORY + "sensor.log"));
				} catch (IOException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
				} catch (SecurityException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
				}
		}

		public BoatSensor(final BoatProxy proxy, int channel) {
				if (!loggingReady.compareAndSet(true, true)) {
						setUpLogging();
				}
				this.proxy = proxy;
				this.channel = channel;
				sensorName = (proxy != null ? proxy.getProxyName() : "NULL") + "-" + channel;
				proxy.getVehicleServer().addSensorListener(channel, this, null);

        // Cheating dummy data, another version of this is in SimpleBoatSimulator, 
				// effectively overridden by overridding addSensorListener in FastSimpleBoatSimulator
				// because no access to that code from here.
				// @todo Only should be on for simulation
				// ABHINAV COMMENT OUT THIS THREAD BEFORE RUNNING ON THE REAL BOATS!!
				if (GENERATE_FAKE_DATA) {

						LOGGER.warning("GENERATE_FAKE_DATA is set to TRUE, will generate fake data!");

						(new Thread() {

								public void run() {

										LOGGER.info("Generating fake sensor data for " + proxy + " with " + sensorName);

										while (true) {

												double[] prev = null;

												currLoc = proxy.getPosition();
												if (currLoc != null) {
														SensorData sd = new SensorData();
														int sensorChannel = (int) (CoreHelper.RANDOM.nextDouble() * 4) + 1;
														sd.channel = sensorChannel;
														switch (sensorChannel) {
																case FAKE_DO_CHANNEL:
																		sd.type = VehicleServer.SensorType.ATLAS_DO;
																		break;
																case FAKE_PH_CHANNEL:
																		sd.type = VehicleServer.SensorType.ATLAS_PH;
																		break;
																case FAKE_ES2_CHANNEL:
																		sd.type = VehicleServer.SensorType.ES2;
																		break;
																case FAKE_BATTERY_CHANNEL:
																default:
																		sd.type = VehicleServer.SensorType.BATTERY;
																		break;
														}

														sd.data = new double[4];
														if (randomData) {
																// Random data scaled to realistic sensor bounds
																double value;
																switch (sensorChannel) {
																		case FAKE_DO_CHANNEL:
																				value = CoreHelper.RANDOM.nextDouble() * (FAKE_DO_MAX - FAKE_DO_MIN) + FAKE_DO_MIN;
																				break;
																		case FAKE_PH_CHANNEL:
																				value = CoreHelper.RANDOM.nextDouble() * (FAKE_PH_MAX - FAKE_PH_MIN) + FAKE_PH_MIN;
																				break;
																		case FAKE_ES2_CHANNEL:
																				value = CoreHelper.RANDOM.nextDouble() * (FAKE_ES2_MAX - FAKE_ES2_MIN) + FAKE_ES2_MIN;
																				break;
																		case FAKE_BATTERY_CHANNEL:
																		default:
																				value = CoreHelper.RANDOM.nextDouble() * (FAKE_BATTERY_MAX - FAKE_BATTERY_MIN) + FAKE_BATTERY_MIN;
																				break;
																}
																for (int i = 0; i < sd.data.length; i++) {
																		sd.data[i] = value;
																}
														} else if (simpleData) {
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
																		if ((CoreHelper.RANDOM.nextDouble() < addRate && xs.size() < 20) || (xs.size() == 0)) {
																				LOGGER.info("Added a fake sensor data point for " + proxy);
																				double lon = currLoc.longitude.degrees + (distFactor * (CoreHelper.RANDOM.nextDouble() - 0.5));
																				double lat = currLoc.latitude.degrees + (distFactor * (CoreHelper.RANDOM.nextDouble() - 0.5));
																				double value = CoreHelper.RANDOM.nextDouble() * valueFactor;
																				if (CoreHelper.RANDOM.nextBoolean()) {
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
				if (NEW_SENSOR_FORMAT) {
						String stringValue = Arrays.toString(sd.data);
						stringValue = stringValue.substring(1, stringValue.length() - 1);
						String sensorType = "";
						double value = -1;
						switch (sd.channel) {
								case 4:
										sensorType = VehicleServer.SensorType.BATTERY.toString();
										String[] batteries = stringValue.split(",");
										value = Double.parseDouble(batteries[0]);
										break;
								case 1:
										sensorType = VehicleServer.SensorType.ATLAS_DO.toString();
										value = Double.parseDouble(stringValue);
										break;
								case 2:
										sensorType = VehicleServer.SensorType.ATLAS_PH.toString();
										Double.parseDouble(stringValue);
										break;
								case 3:
										sensorType = VehicleServer.SensorType.ES2.toString();
										Double.parseDouble(stringValue);
										break;
								default:
										break;
						}

						Position curP = proxy.getPosition();
						Long timeReceived = System.currentTimeMillis();
						Location curLocation = new Location(curP.latitude.degrees, curP.longitude.degrees, 0);
						Observation obs = new Observation(sensorType, value, proxy.getProxyName(), curLocation, timeReceived);
						// Don't spam primary log
						//        LOGGER.info("Received SensorData: " + obs);
						try {
								writer.write(obs.variable + "\t" + obs.value + "\t" + obs.source + "\t" + obs.location + "\t" + String.format("%1$tH:%1$tM:%1$tS:%1$tL", new Date(obs.time)) + "\n");
								writer.flush();
						} catch (IOException ex) {
								LOGGER.log(Level.SEVERE, null, ex);
						}
						for (ObservationListenerInt listener : listeners) {
								listener.newObservation(obs);
						}
				} else {
						Position curP = proxy.getPosition();
						Long timeReceived = System.currentTimeMillis();
						Location curLocation = new Location(curP.latitude.degrees, curP.longitude.degrees, 0);
						Observation obs = new Observation(sd.type.toString(), sd.data[0], proxy.getProxyName(), curLocation, timeReceived);
						// Don't spam primary log
						//        LOGGER.info("Received SensorData: " + obs);
						try {
								writer.write(obs.variable + "\t" + obs.value + "\t" + obs.source + "\t" + obs.location + "\t" + String.format("%1$tH:%1$tM:%1$tS:%1$tL", new Date(obs.time)) + "\n");
								writer.flush();
						} catch (IOException ex) {
								LOGGER.log(Level.SEVERE, null, ex);
						}
						for (ObservationListenerInt listener : listeners) {
								listener.newObservation(obs);
						}
				}
		}

		@Override
		public String toString() {
				return sensorName;
		}
}
