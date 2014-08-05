/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crw;

import crw.proxy.BoatProxy;
import crw.ui.BoatMarker;
import crw.ui.widget.RobotWidget;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class Coordinator {


    public enum Method {

        EASY, COST;

        @Override
        public String toString() {
            switch (this) {
                case EASY:
                    return "Easy Coordination";
                case COST:
                    return "Sequential System Item Auctions";
                default:
                    throw new IllegalArgumentException();

            }
        }
    }

    protected Hashtable<BoatMarker, BoatProxy> markerToProxy;

    private HashMap<BoatMarker, ArrayList<Position>> decisions = new HashMap<BoatMarker, ArrayList<Position>>();
    private Method method = Method.EASY;

    public Coordinator(Hashtable<BoatMarker, BoatProxy> markerToProxy) {
        this.markerToProxy = markerToProxy;
//        if(this.decisions.isEmpty()){
        initDecisions();
//        }
    }
    
    public Coordinator() {
        this.markerToProxy = new Hashtable<BoatMarker, BoatProxy>();
        initDecisions();
    }

    public void initDecisions() {
        for (BoatMarker b : markerToProxy.keySet()) {
            this.decisions.put(b, new ArrayList<Position>());
        }
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Hashtable<BoatMarker, BoatProxy> getMarkerToProxy() {
        return markerToProxy;
    }

    public void setMarkerToProxy(Hashtable<BoatMarker, BoatProxy> markerToProxy) {
        this.markerToProxy = markerToProxy;
    }

    public HashMap<BoatMarker, ArrayList<Position>> getDecisions(){
        return decisions;
    }
    
    public void clearDecisions() {

        this.decisions = new HashMap<BoatMarker, ArrayList<Position>>();
        initDecisions();
    }

    public HashMap<BoatMarker, ArrayList<Position>> taskAssignment(ArrayList<Position> positions) {

        HashMap<BoatMarker, ArrayList<Position>> result = new HashMap<BoatMarker, ArrayList<Position>>();

        switch (this.method) {
            case EASY:
                result = doEasyCoord(positions);
                break;
            case COST:
                result = doCostBasedCoord(positions);
                break;
            default:
                result = doEasyCoord(positions);
        }

        return result;

    }

    private HashMap<BoatMarker, ArrayList<Position>> doEasyCoord(ArrayList<Position> positions) {

        //ArrayList<ArrayList<Position>> 
//        HashMap<BoatMarker, ArrayList<Position>> decisions = new HashMap<BoatMarker, ArrayList<Position>>();
        ArrayList<Position> dummyPos = new ArrayList<Position>(positions);
        ArrayList<BoatMarker> boats = new ArrayList<BoatMarker>(markerToProxy.keySet());

//        for (BoatMarker b : boats) {
//            decisions.put(b, new ArrayList<Position>());
//        }
        Double min = Double.MAX_VALUE;

        do {
            HashMap<BoatMarker, Position> nearest = new HashMap<BoatMarker, Position>();

            for (int i = 0; i < boats.size(); i++) {
                BoatMarker b = boats.get(i);

                final Position boatPos = (decisions.get(b).isEmpty()) ? b.getPosition() : decisions.get(b).get(decisions.get(b).size() - 1);

                Collections.sort(dummyPos, new Comparator<Position>() {
                    @Override
                    public int compare(Position t, Position t1) {
                        if (computeDistance(boatPos, t) < computeDistance(boatPos, t1)) {
                            return -1;
                        } else if (computeDistance(boatPos, t) > computeDistance(boatPos, t1)) {
                            return 1;
                        }

                        return 0;

                    }
                });

                Position insert = dummyPos.isEmpty() ? null : dummyPos.get(0);

                if (insert == null) {
                    break;
                }

                nearest.put(b, insert);
//                System.out.println("near boat: " + b + " pos: " + nearest.get(b));
            }

            HashMap<BoatMarker, Double> best = new HashMap<BoatMarker, Double>();

            for (Map.Entry<BoatMarker, Position> entry : nearest.entrySet()) {

                Position comparing = (decisions.get(entry.getKey()).isEmpty()) ? entry.getKey().getPosition() : decisions.get(entry.getKey()).get(decisions.get(entry.getKey()).size() - 1);

                best.put(entry.getKey(), computeDistance(comparing, entry.getValue()));
//                System.out.println("distance: " + best.get(entry.getKey()) + " for " + entry.getKey() + " and pos: " + entry.getValue());
            }

            Map.Entry<BoatMarker, Double> bestPos = Collections.min(best.entrySet(), new Comparator<Map.Entry<BoatMarker, Double>>() {
                public int compare(Map.Entry<BoatMarker, Double> entry1, Map.Entry<BoatMarker, Double> entry2) {
                    return entry1.getValue().compareTo(entry2.getValue());
                }
            });

//            System.out.println("boat: " + bestPos.getKey() + " pos: " + bestPos.getValue());

            decisions.get(bestPos.getKey()).add(nearest.get(bestPos.getKey()));
            dummyPos.remove(nearest.get(bestPos.getKey()));

        } while (!dummyPos.isEmpty());

//        System.out.println("Positions: " + positions.toString());
//
//        for (BoatMarker b : boats) {
//            System.out.println("lista for " + b.toString() + ": " + decisions.get(b).toString());
//        }

//        cancelAssignedWaypoints();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RobotWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
        
//        startCheckVisitedPositions(this);
        
        return decisions;
//        executePath(boats, decisions);

    }

    public static Position getNearestPos(Position comparing, ArrayList<Position> dummyPos) {

        Position ret = null;
        double min = Double.MAX_VALUE;

        for (Position p : dummyPos) {
            double t = computeDistance(comparing, p);
            if (t < min) {
                min = t;
                ret = p;
            }
        }
        return ret;
    }

    private HashMap<Double, ArrayList<Position>> computeBestPath(BoatMarker b, ArrayList<Position> get) {

        HashMap<Double, ArrayList<Position>> result = new HashMap<Double, ArrayList<Position>>();

        if (get.size() == 1) {
            result.put(computeDistance(b.getPosition(), get.get(0)), get);
            return result;
        }

        ArrayList<Position> dummyGet = new ArrayList<Position>(get);
        ArrayList<Position> tmpPath = new ArrayList<Position>();
        Double cost = 0.0;

        do {
            Position init;
            if (tmpPath.isEmpty()) {
                init = b.getPosition();
            } else {
                init = tmpPath.get(tmpPath.size() - 1);
            }
            Position p = getNearestPos(init, dummyGet);

            cost += computeDistance(init, p);

            dummyGet.remove(p);
            tmpPath.add(p);

        } while (!dummyGet.isEmpty());

        result.put(cost, tmpPath);

        return result;
    }

    private HashMap<BoatMarker, ArrayList<Position>> doCostBasedCoord(ArrayList<Position> positions) {

//        System.out.println("Cost based");
        HashMap<BoatMarker, ArrayList<Position>> dummyDec = new HashMap<BoatMarker, ArrayList<Position>>();

        ArrayList<Position> dummyPos = new ArrayList<Position>(positions);
        ArrayList<BoatMarker> boats = new ArrayList<BoatMarker>(markerToProxy.keySet());

        do {

            HashMap<BoatMarker, ArrayList<Position>> possibilities = new HashMap<BoatMarker, ArrayList<Position>>();
            HashMap<BoatMarker, Double> possibilitiesCost = new HashMap<BoatMarker, Double>();

            Random r = new Random();
            Position test = dummyPos.get(r.nextInt(dummyPos.size()));

            for (BoatMarker b : boats) {
                ArrayList<Position> tmp;
                if (decisions.get(b).isEmpty()) {
                    tmp = new ArrayList<Position>();
                } else {
                    tmp = new ArrayList<Position>(decisions.get(b));
                }

                tmp.add(test);

                HashMap<Double, ArrayList<Position>> allPathsCost;

//                if (decisions.get(b).isEmpty()) {
                allPathsCost = computeBestPath(b, tmp);
//                } else if (working) {
//                    allPathsCost = computeNewInsertPath(b, tmp);
//                } else {
//                    allPathsCost = computeBestPath(b, tmp);
//                }

                ArrayList<Map.Entry<Double, ArrayList<Position>>> listPaths = new ArrayList<Map.Entry<Double, ArrayList<Position>>>(allPathsCost.entrySet());
                Map.Entry<Double, ArrayList<Position>> min = Collections.min(listPaths, new Comparator<Map.Entry<Double, ArrayList<Position>>>() {

                    @Override
                    public int compare(Map.Entry<Double, ArrayList<Position>> t, Map.Entry<Double, ArrayList<Position>> t1) {
                        return t.getKey().compareTo(t1.getKey());
                    }
                });

                possibilities.put(b, min.getValue());
                possibilitiesCost.put(b, min.getKey());

            }

            ArrayList<Map.Entry<BoatMarker, Double>> listCosts = new ArrayList<Map.Entry<BoatMarker, Double>>(possibilitiesCost.entrySet());
            Map.Entry<BoatMarker, Double> min = Collections.min(listCosts, new Comparator<Map.Entry<BoatMarker, Double>>() {

                @Override
                public int compare(Map.Entry<BoatMarker, Double> t, Map.Entry<BoatMarker, Double> t1) {
                    return t.getValue().compareTo(t1.getValue());
                }
            });

            decisions.put(min.getKey(), possibilities.get(min.getKey()));
            dummyPos.remove(test);

        } while (!dummyPos.isEmpty());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RobotWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
//                startCheckVisitedPositions(this);

        return decisions;

    }

    /**
     * Calculates geodetic distance between two points specified by
     * latitude/longitude using Vincenty inverse formula for ellipsoids
     *
     * @param lat1 first point latitude in decimal degrees
     * @param lon1 first point longitude in decimal degrees
     * @param lat2 second point latitude in decimal degrees
     * @param lon2 second point longitude in decimal degrees
     * @returns distance in meters between points with 5.10<sup>-4</sup>
     * precision
     * @see <a
     * href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally
     * posted here</a>
     */
    public static double distVincenty(double lat1, double lon1, double lat2, double lon2) {
        double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
        double lambda = L, lambdaP, iterLimit = 100;
        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
                    + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0) {
                return 0; // co-incident points
            }
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            if (Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
            }
            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1 - C) * f * sinAlpha
                    * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0) {
            return Double.NaN; // formula failed to converge
        }
        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B
                * sinSigma
                * (cos2SigmaM + B
                / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
                * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double dist = b * A * (sigma - deltaSigma);

        return dist;
    }
    
//    private static void startCheckVisitedPositions(final Coordinator c){
//                
//        (new Thread() {
//            @Override
//            public void run() {
//                boolean guard;
//                do {
//                    guard = false;
//                    for (BoatMarker b : c.getMarkerToProxy().keySet()) {
//                        ArrayList<Position> copy = new ArrayList<Position>(c.getDecisions().get(b));
//                        for (Position p : copy) {
//                            Double d = Coordinator.computeDistance(b.getProxy().getPosition(), p);
////                            System.out.println("distance: "+d);
//                            if (d == 0.0) {
//                                c.getDecisions().get(b).remove(p);
//                                System.out.println("distance equals: " + d);
//                                LatLon m = new LatLon(p.getLatitude(), p.getLongitude());
//
//                            } else if (Math.abs(d) < 3) {
//                                c.getDecisions().get(b).remove(p);
//                                System.out.println("distance error: " + d);
//
//                            }
//                        }
//                    }
//
//                    for (BoatMarker b : c.getMarkerToProxy().keySet()) {
//                        if (!c.getDecisions().get(b).isEmpty()) {
//                            guard = true;
//
//                        }
//                    }
//
//                } while (guard);
//
//            }
//        }).start();
//        
//    }

    public static double computeDistance(Position p1, Position p2) {
        //This uses the ‘haversine’ formula to calculate the great-circle distance between two points – that is,
        //the shortest distance over the earth’s surface – giving an ‘as-the-crow-flies’ distance between 
        //the points (ignoring any hills, of course!).

//        return distance(p1.getLatitude().degrees, p2.getLatitude().degrees, 
//                p1.getLongitude().degrees, p2.getLongitude().degrees, 
//                p1.getElevation(), p2.getElevation());
//        return haversine(p1.getLatitude().degrees, p1.getLongitude().degrees,
//                p2.getLatitude().degrees, p2.getLongitude().degrees);
        return distVincenty(p1.getLatitude().degrees, p1.getLongitude().degrees,
                p2.getLatitude().degrees, p2.getLongitude().degrees);
    }

}
