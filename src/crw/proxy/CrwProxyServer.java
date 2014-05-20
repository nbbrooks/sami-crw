package crw.proxy;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.common.datamodels.Feature;
import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.common.datamodels.ModelCapability;
import com.perc.mitpas.adi.common.datamodels.ModelCapabilityType;
import com.perc.mitpas.adi.common.datamodels.VehicleAsset;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;
import sami.proxy.ProxyServerListenerInt;

/**
 *
 * @author nbb
 */
public class CrwProxyServer implements ProxyServerInt {

    private final static Logger LOGGER = Logger.getLogger(CrwProxyServer.class.getName());
    private Random random = new Random();
    private static ArrayList<ProxyServerListenerInt> listeners = new ArrayList<ProxyServerListenerInt>();
    private int proxyCounter = 1;
    ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
    ArrayList<AbstractAsset> assets = new ArrayList<AbstractAsset>();
    HashMap<InetSocketAddress, ProxyInt> ipToProxyMap = new HashMap<InetSocketAddress, ProxyInt>();
    HashMap<AbstractAsset, ProxyInt> assetToProxyMap = new HashMap<AbstractAsset, ProxyInt>();
    HashMap<ProxyInt, AbstractAsset> proxyToAssetMap = new HashMap<ProxyInt, AbstractAsset>();

    public CrwProxyServer() {
    }

    @Override
    public boolean addListener(ProxyServerListenerInt l) {
        return listeners.add(l);
    }

    @Override
    public boolean removeListener(ProxyServerListenerInt l) {
        return listeners.remove(l);
    }

    @Override
    public ProxyInt createProxy(String name, Color color, InetSocketAddress addr) {
        try {
            // Create proxy
            if (color == null) {
                LOGGER.severe("Boat proxy's color was null, using white");
                color = Color.WHITE;
            }
            ProxyInt proxy = new BoatProxy(name, color, proxyCounter, addr);
            proxyCounter++;
            proxies.add(proxy);
            ipToProxyMap.put(addr, proxy);

            // Create asset
            //@todo Proxy and asset should be combined...
            VehicleAsset v = new VehicleAsset(name);
            ModelCapabilityType capType = new ModelCapabilityType("Airboat");
            ArrayList<Feature> mFeatures = new ArrayList<Feature>();
            mFeatures.add(new Feature(new FeatureType("Sample")));
            mFeatures.add(new Feature(new FeatureType("DO")));
            mFeatures.add(new Feature(new FeatureType("ES2")));
            mFeatures.add(new Feature(new FeatureType("Camera")));
            ModelCapability modelCap = new ModelCapability(capType);
            modelCap.setFeatures(mFeatures);
            v.addAssetCapability(modelCap);

            assets.add(v);
            proxyToAssetMap.put(proxy, v);
            assetToProxyMap.put(v, proxy);

            proxy.start();
            for (ProxyServerListenerInt l : listeners) {
                l.proxyAdded(proxy);
            }
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ProxyInt getProxy(AbstractAsset asset) {
        return assetToProxyMap.get(asset);
    }

    @Override
    public AbstractAsset getAsset(ProxyInt proxy) {
        return proxyToAssetMap.get(proxy);
    }

    @Override
    public ArrayList<ProxyInt> getProxyListClone() {
        return (ArrayList<ProxyInt>) proxies.clone();
    }

    @Override
    public ArrayList<AbstractAsset> getAssetListClone() {
        return (ArrayList<AbstractAsset>) assets.clone();
    }

    @Override
    public boolean remove(ProxyInt proxy) {
        AbstractAsset asset = proxyToAssetMap.get(proxy);
        if (asset == null) {
            return false;
        }
        proxies.remove(proxy);
        assets.remove(asset);
        proxyToAssetMap.remove(proxy);
        assetToProxyMap.remove(asset);
        // @todo Proxies are not removed from hash table, expecting that something else with a new URI will override
        // boatMap.remove(proxy.)
        return true;
    }

    @Override
    public boolean remove(AbstractAsset asset) {
        ProxyInt proxy = assetToProxyMap.get(asset);
        if (proxy == null) {
            return false;
        }
        proxies.remove(proxy);
        assets.remove(asset);
        proxyToAssetMap.remove(proxy);
        assetToProxyMap.remove(asset);
        return true;
    }

    @Override
    public boolean shutdown() {
        //@todo nothing calls this - should that be the case?
        for (ProxyInt p : proxies) {
            if (p instanceof BoatProxy) {
                ((BoatProxy) p)._server.stopCamera(null);
                ((BoatProxy) p)._server.shutdown();
            }
        }
        return true;
    }

    public ProxyInt getRandomProxy() {
        if (proxies.isEmpty()) {
            return null;
        }
        return proxies.get(random.nextInt(proxies.size()));
    }

    public void setCameraRates(double d) {
        LOGGER.info("Setting camera on all boats to have time between frames = " + d);
        for (ProxyInt p : proxies) {
            if (p instanceof BoatProxy) {
                ((BoatProxy) p)._server.stopCamera(null);
                ((BoatProxy) p)._server.startCamera(0, d, 640, 480, null);
            }
        }
    }
}
