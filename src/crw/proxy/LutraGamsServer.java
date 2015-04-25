/**
 * *****************************************************************
 * Usage of this software requires acceptance of the GAMS-CMU License, which can
 * be found at the following URL:
 *
 * https://code.google.com/p/gams-cmu/wiki/License
 * *******************************************************************
 */
package crw.proxy;

import com.madara.KnowledgeBase;
import com.gams.controllers.BaseController;
import com.madara.transport.QoSTransportSettings;
import com.madara.transport.TransportType;
import edu.cmu.ri.crw.AsyncVehicleServer;

public class LutraGamsServer implements Runnable {

    BaseController controller;

    /**
     * Local reference to vehicle server.
     */
    protected AsyncVehicleServer _server;

    /**
     * Local reference to MADARA knowledge base.
     */
    protected KnowledgeBase _knowledge;

    protected final String _ipAddress;
    protected final LutraPlatform lutraPlatform;
    protected final LutraAlgorithm lutraAlgorithm;

    public LutraGamsServer(AsyncVehicleServer server, String ipAddress, int id, int teamSize) {
        _server = server;
        _ipAddress = ipAddress;

        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);

        _knowledge = new KnowledgeBase(ipAddress, settings);
//        _knowledge.evaluateNoReturn("#set_precision(9)");
        
//        System.out.println("Passing knowledge base to base controller...");
        controller = new BaseController(_knowledge);

//        System.out.println("Creating lutra platform");
        lutraPlatform = new LutraPlatform(_server, _ipAddress, id);

//        System.out.println("Creating algorithm");
        lutraAlgorithm = new LutraAlgorithm(_server, _ipAddress);

//        System.out.println("Initializing platform");
        controller.initVars(id, teamSize);
        controller.initPlatform(lutraPlatform);
        controller.initAlgorithm(lutraAlgorithm);

        lutraPlatform.init(controller);
        lutraAlgorithm.init(controller);
    }

    public void setDeviceId(int id) {
//        lutraAlgorithm.self.id = new Integer(id);
    }

    @Override
    public void run() {

        System.out.println("Running controller every 1s for 1000s...");
        controller.run(1.0, 1000.0);
    }
}
