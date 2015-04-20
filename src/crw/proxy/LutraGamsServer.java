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

    public LutraGamsServer(AsyncVehicleServer server, String ipAddress, int id) {
        _server = server;
        _ipAddress = ipAddress;

        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);

        _knowledge = new KnowledgeBase(ipAddress, settings);
//        System.out.println("Passing knowledge base to base controller...");
        controller = new BaseController(_knowledge);

        // What should this be hardcoded to?
//        controller.initVars(boatId, 24);
//        System.out.println("Creating lutra platform");
        lutraPlatform = new LutraPlatform(_server, _ipAddress, id);

//        System.out.println("Creating algorithm");
        lutraAlgorithm = new LutraAlgorithm(_server, _ipAddress);

//        System.out.println("Initializing platform");
        controller.initPlatform(lutraPlatform);
        controller.initAlgorithm(lutraAlgorithm);

        lutraPlatform.init();
        lutraAlgorithm.init();
//        lutraPlatform.self.init();

//        System.out.println("LutraGamsServer knowledge");
//        _knowledge.print();
//        System.out.println("ipAddress: " + ipAddress);
//        System.out.println("self.id: " + lutraAlgorithm.self.id);
//        System.out.println("self.device: " + lutraAlgorithm.self.device);
//        System.out.println("self.device.command: " + lutraAlgorithm.self.device.command);
//        System.out.println("");
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
