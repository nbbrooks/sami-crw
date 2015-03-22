package crw.ui.queue.text;

import sami.allocation.ResourceAllocation;
import sami.proxy.ProxyInt;
import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.resourceallocation.AllocationSolverServer;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationRequest;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationResponse;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResponseListener;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.CrwHelper;
import crw.asset.BoatAsset;
import crw.event.output.proxy.ProxyExploreArea;
import crw.proxy.BoatProxy;
import crw.task.CollectSampleTask;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import crw.task.EcMeasTask;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.event.MissingParamsRequest;
import sami.event.ReflectedEventSpecification;
import sami.event.ReflectionHelper;
import sami.markup.Attention;
import sami.markupOption.BlinkOption;
import sami.uilanguage.toui.GetParamsMessage;

/**
 *
 * @author nbb
 */
public class QueueTest implements ResponseListener {

    private final static Logger LOGGER = Logger.getLogger(QueueTest.class.getName());
    public static Random randomOpt = new Random(0);
    public static Random randomPri = new Random(0);
    public QueueFrame oif;
    public ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();

    public static void main(String[] args) {
        QueueTest test = new QueueTest();
    }

    public QueueTest() {
        int numTasks = 1;
        int numProxies = 3;
//        proxies = createProxies(numProxies);
        oif = new QueueFrame();
        ArrayList<AbstractAsset> assetList = new ArrayList<AbstractAsset>();
        for (int i = 0; i < numProxies; i++) {
            assetList.add(new BoatAsset(i));
        }
        ArrayList<ITask> taskList = new ArrayList<ITask>();
        for (int i = 0; i < numTasks; i++) {
            taskList.add(new EcMeasTask("EX_" + i));
        }
        for (int i = 0; i < numTasks; i++) {
            taskList.add(new CollectSampleTask("SA_" + i));
        }

        getRA(assetList, taskList, 1);
        getRA(assetList, taskList, 1);
//        getPP(3);
//        getBP(proxies);
//        createArea(Priority.getPriority(Priority.Ranking.LOW));
//        createArea(Priority.getPriority(Priority.Ranking.HIGH));
    }

    public void createArea(int priority) {
        Class eventClass = ProxyExploreArea.class;
        ReflectedEventSpecification eventSpec = new ReflectedEventSpecification(ProxyExploreArea.class.getName());
        try {
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
            HashMap<String, Object> fieldValues = eventSpec.getFieldValues();
            HashMap<String, String> fieldReadVariables = eventSpec.getReadVariables();
            HashMap<String, String> fieldDescriptions = (HashMap<String, String>) (eventClass.getField("fieldNameToDescription").get(null));

            Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions = new Hashtable<ReflectedEventSpecification, Hashtable<Field, String>>();
            Hashtable<ReflectedEventSpecification, ArrayList<Field>> eventSpecToFields = new Hashtable<ReflectedEventSpecification, ArrayList<Field>>();
            ArrayList<Field> missingFields = new ArrayList<Field>();
            Hashtable<Field, String> missingFieldDescriptions = new Hashtable<Field, String>();

            eventSpecToFieldDescriptions.put(eventSpec, missingFieldDescriptions);
            eventSpecToFields.put(eventSpec, missingFields);

            for (String fieldName : fieldNames) {
                System.out.println("\tfield name: " + fieldName);
                if (!fieldValues.containsKey(fieldName) && !fieldReadVariables.containsKey(fieldName)) {
                    System.out.println("\t\tlooking for " + fieldName + " in class " + eventSpec.getClassName());
                    Field missingField = ReflectionHelper.getField(eventClass, fieldName);
                    missingFieldDescriptions.put(missingField, fieldDescriptions.get(fieldName));
                    missingFields.add(missingField);
                }
            }
            MissingParamsRequest oe = new MissingParamsRequest(null, eventSpecToFieldDescriptions);
            GetParamsMessage msg = new GetParamsMessage(oe.getId(), oe.getMissionId(), priority, oe.getEventToFieldDescriptions());
            oif.toUiMessageReceived(msg);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ProxyInt> createProxies(int numProxies) {
        ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
        for (int i = 0; i < numProxies; i++) {
            proxies.add(new BoatProxy("Boat" + i, CrwHelper.randomColor(), i, new InetSocketAddress("localhost", 11411 + i)));
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

        System.out.println("Submiting request");
        System.out.println("\t" + request.getAssets());
        System.out.println("\t" + request.getAvailableTime());
        System.out.println("\t" + request.getConstraints());
        System.out.println("\t" + request.getTasks());
        System.out.println("\t" + request.getNoOptions());

        AllocationSolverServer.submitRequest(request, this);
    }

    public void getBP(ArrayList<ProxyInt> boatProxyList) {
        ProxyOptionsMessage msg = new ProxyOptionsMessage(null, null, randomPri.nextInt(3) + 1, true, boatProxyList);
        Attention a = new Attention();
        a.attentionType = Attention.AttentionType.BLINK;
        a.blink = new BlinkOption();
        a.attentionTarget = Attention.AttentionTarget.PANEL;
        a.attentionEnd = Attention.AttentionEnd.ON_CLICK;
        msg.addMarkup(a);
        oif.toUiMessageReceived(msg);

        msg = new ProxyOptionsMessage(null, null, randomPri.nextInt(3) + 1, true, boatProxyList);
        a = new Attention();
        a.attentionType = Attention.AttentionType.HIGHLIGHT;
        a.attentionTarget = Attention.AttentionTarget.PANEL;
        a.attentionEnd = Attention.AttentionEnd.ON_CLICK;
        msg.addMarkup(a);
        oif.toUiMessageReceived(msg);
    }

    @Override
    public void response(List<ResourceAllocationResponse> list) {
        List<ResourceAllocation> allocs = new ArrayList<ResourceAllocation>();

        for (int i = 0; i < 3; i++) {
            allocs.add(new ResourceAllocation(list.get(0).getAllocation(), list.get(0).getTaskTimings()));
            AllocationOptionsMessage msg = new AllocationOptionsMessage(null, null, randomPri.nextInt(3) + 1, allocs);
            oif.toUiMessageReceived(msg);
        }
    }
}
