package crw.ui.queue;

import crw.ui.component.QueueFrame;
import sami.allocation.ResourceAllocation;
import sami.proxy.ProxyInt;
import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.resourceallocation.AllocationSolverServer;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationRequest;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResourceAllocationResponse;
import com.perc.mitpas.adi.mission.planning.resourceallocation.ResponseListener;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.asset.BoatAsset;
import crw.event.output.proxy.ProxyExploreArea;
import crw.proxy.BoatProxy;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import crw.task.DoMeasTask;
import crw.uilanguage.message.toui.AllocationOptionsMessage;
import crw.uilanguage.message.toui.ProxyOptionsMessage;
import dreaam.developer.ReflectedEventD;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.MissingParamsRequest;
import sami.event.ReflectedEventSpecification;
import sami.markup.Attention;
import sami.markup.Markup;
import sami.markup.Priority;
import sami.markupOption.BlinkOption;
import sami.uilanguage.toui.GetParamsMessage;

/**
 *
 * @author nbb
 */
public class QueueTest implements ResponseListener {

    public static Random randomOpt = new Random(0);
    public static Random randomPri = new Random(0);
    public QueueFrame oif;
    public ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();

    public static void main(String[] args) {
        QueueTest test = new QueueTest();
    }

    public QueueTest() {
        int num = 3;
        proxies = createProxies(3);
        oif = new QueueFrame(new QueueDatabase());
        ArrayList<AbstractAsset> assetList = new ArrayList<AbstractAsset>();
        for (int i = 0; i < num; i++) {
            assetList.add(new BoatAsset(i));
        }
        ArrayList<ITask> taskList = new ArrayList<ITask>();
        for (int i = 0; i < num; i++) {
            taskList.add(new DoMeasTask("DO_" + i));
        }

//        getRA(assetList, taskList, 1);
//        getPP(3);
//        getBP(proxies);
        createArea(Priority.getPriority(Priority.Ranking.LOW));
        createArea(Priority.getPriority(Priority.Ranking.HIGH));
    }

    public void createArea(int priority) {
        Class eventClass = ProxyExploreArea.class;
        ReflectedEventSpecification eventSpec = new ReflectedEventSpecification(ProxyExploreArea.class.getName());
        try {
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));

            for (String fieldName : fieldNames) {
                eventSpec.addFieldDefinition(fieldName, null);
            }

        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        }

        Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions = new Hashtable<ReflectedEventSpecification, Hashtable<Field, String>>();
        Hashtable<ReflectedEventSpecification, ArrayList<Field>> eventSpecToFields = new Hashtable<ReflectedEventSpecification, ArrayList<Field>>();

        Hashtable<Field, String> fieldDescriptions = new Hashtable<Field, String>();
        ArrayList<Field> fields = new ArrayList<Field>();
        eventSpecToFieldDescriptions.put(eventSpec, fieldDescriptions);
        eventSpecToFields.put(eventSpec, fields);
        HashMap<String, Object> instanceParams = eventSpec.getFieldDefinitions();
        for (String fieldName : instanceParams.keySet()) {
            System.out.println("\tfield name: " + fieldName);
            if (instanceParams.get(fieldName) == null) {
                try {
                    System.out.println("\t\tlooking for " + fieldName + " in class " + eventSpec.getClassName());
                    Field missingField = Class.forName(eventSpec.getClassName()).getField(fieldName);
                    fieldDescriptions.put(missingField, "");
                    fields.add(missingField);
                } catch (ClassNotFoundException cnfe) {
                    cnfe.printStackTrace();
                } catch (NoSuchFieldException nsfe) {
                    nsfe.printStackTrace();
                }
            }
        }
        MissingParamsRequest oe = new MissingParamsRequest(null, eventSpecToFieldDescriptions);
        GetParamsMessage msg = new GetParamsMessage(oe.getId(), oe.getMissionId(), priority, oe.getFieldDescriptions());
        oif.toUiMessageReceived(msg);
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

    private Color randomColor() {
        Random rand = new Random();

        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();

        return new Color(r, g, b);
    }
}
