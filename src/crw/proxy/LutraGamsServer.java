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
import crw.general.FastSimpleBoatSimulator;
import edu.cmu.ri.crw.AsyncVehicleServer;

public class LutraGamsServer implements Runnable {

    BaseController controller;

    /** Local reference to vehicle server. */
    protected AsyncVehicleServer _server;

    /** Local reference to MADARA knowledge base. */
    protected KnowledgeBase _knowledge;
    
    protected String _ipAddress;

    public LutraGamsServer(AsyncVehicleServer server, String ipAddress) {
        _server = server;
        _ipAddress = ipAddress;
        
        QoSTransportSettings settings = new QoSTransportSettings();
        settings.setHosts(new String[]{"239.255.0.1:4150"});
        settings.setType(TransportType.MULTICAST_TRANSPORT);

        _knowledge = new KnowledgeBase(ipAddress, settings);
        System.out.println("Passing knowledge base to base controller...");
        controller = new BaseController(_knowledge);
        
        // What should this be hardcoded to?
//        controller.initVars(boatId, 24);

        System.out.println("Creating lutra platform");
        LutraPlatform lutraPlatform = new LutraPlatform(_server, _ipAddress);

        System.out.println("Creating algorithm");
        LutraAlgorithm lutraAlgorithm = new LutraAlgorithm(_server, _ipAddress);

        System.out.println("Initializing platform");
        controller.initPlatform(lutraPlatform);
        controller.initAlgorithm(lutraAlgorithm);

        lutraPlatform.init();
        lutraAlgorithm.init();
        
//        System.out.println("LutraGamsServer knowledge");
//        _knowledge.print();
    }

    @Override
    public void run() {

        System.out.println("Running controller every 1s for 1000s...");
        controller.run(1.0, 1000.0);
    }
}
