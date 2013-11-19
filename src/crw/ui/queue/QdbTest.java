package crw.ui.queue;

import sami.allocation.ResourceAllocation;
import sami.proxy.ProxyInt;
import crw.asset.StationAsset;
import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.resourceallocation.AllocationSolverServer;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationRequest;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationResponse;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResponseListener;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.proxy.BoatProxy;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import crw.task.DoMeasTask;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;

/**
 *
 * @author nbb
 */
public class QdbTest implements ResponseListener {

    public static Random randomOpt = new Random(0);
    public static Random randomPri = new Random(0);
    public QueueFrame oif;
    public ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();

    public static void main(String[] args) {
        QdbTest qdbTest = new QdbTest();
    }

    public QdbTest() {
        int num = 3;
        proxies = createProxies(3);
        oif = new QueueFrame(new QueueDatabase());
        ArrayList<AbstractAsset> assetList = new ArrayList<AbstractAsset>();
        for (int i = 0; i < num * 2; i++) {
            assetList.add(new StationAsset(i));
        }
        ArrayList<ITask> taskList = new ArrayList<ITask>();
        for (int i = 0; i < num; i++) {
            taskList.add(new DoMeasTask("aerial" + i));
        }

//        getRA(assetList, taskList, 1);
//        getPP(3);
        getBP(proxies);

//        createArea(Priority.getPriority(Priority.Ranking.LOW));
//        createArea(Priority.getPriority(Priority.Ranking.LOW));
//        createArea(Priority.getPriority(Priority.Ranking.HIGH));
//        createArea(Priority.getPriority(Priority.Ranking.MEDIUM));
//        createArea(Priority.getPriority(Priority.Ranking.HIGH));
    }

    public void createArea(int priority) {
//        AreaCreatedMessage acm = new AreaCreatedMessage(null, null);
//
//        try {
//            Object object = Class.forName(AreaCreatedMessage.class.getName()).newInstance();
//        } catch (ClassNotFoundException cnfe) {
//            cnfe.printStackTrace();
//        } catch (InstantiationException ie) {
//            ie.printStackTrace();
//        } catch (IllegalAccessException iae) {
//            iae.printStackTrace();
//        }
//        oif.ToUiMessage(new CreateAreaMessage(UUID.randomUUID(), priority));
    }

    public ArrayList<ProxyInt> createProxies(int numProxies) {
        ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
        for (int i = 0; i < numProxies; i++) {
            proxies.add(new BoatProxy("Boat" + i, randomColor(), i, new InetSocketAddress("localhost", 11411 + i)));
        }
        return proxies;
    }

//    public void getPP(int numOptions) {
//        List<PathUtm> wps = new ArrayList<PathUtm>();
//        for (int i = 0; i < numOptions; i++) {
//            wps.add(new PathUtm());
//        }
//        PathOptionsMessage msg = new PathOptionsMessage(null, null, randomPri.nextInt(3) + 1, wps);
//        oif.ToUiMessage(msg);
//    }
    public void getRA(ArrayList<AbstractAsset> assetList, ArrayList<ITask> taskList, int numOptions) {
        ResourceAllocationRequest request = new ResourceAllocationRequest();
        List<ResourceAllocationResponse> response;

        request.setAvailableTime(0L);
        request.setAssets(assetList);
        request.setConstraints(null);
        request.setTasks(taskList);
        request.setNoOptions(numOptions);

        AllocationSolverServer.submitRequest(request, this);
    }

    public void getBP(ArrayList<ProxyInt> boatProxyList) {
        ProxyOptionsMessage msg = new ProxyOptionsMessage(null, null, randomPri.nextInt(3) + 1, boatProxyList, true);
        oif.ToUiMessage(msg);
    }

    @Override
    public void response(List<ResourceAllocationResponse> list) {
        List<ResourceAllocation> allocs = new ArrayList<ResourceAllocation>();

        for (int i = 0; i < 3; i++) {
            allocs.add(new ResourceAllocation(list.get(0).getAllocation(), list.get(0).getTaskTimings()));
            AllocationOptionsMessage msg = new AllocationOptionsMessage(null, null, randomPri.nextInt(3) + 1, allocs);
            oif.ToUiMessage(msg);
        }
    }

    private Color randomColor() {
        Random rand = new Random();

        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();

        return new Color(r, g, b);
    }
}
