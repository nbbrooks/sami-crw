package crw.ui;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import crw.proxy.BoatProxy;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.allocation.ResourceAllocation;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.engine.TaskAllocationListenerInt;
import sami.proxy.ProxyInt;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 * Displays the current team allocation
 *
 * @author nbb
 */
public class AllocationFrame extends UiFrame implements TaskAllocationListenerInt, UiClientListenerInt {

    private static final Logger LOGGER = Logger.getLogger(AllocationFrame.class.getName());
    private final int BORDER_WIDTH = 3;
    private ResourceAllocation resourceAllocation;
    UiClientInt uiClient;
    UiServerInt uiServer;

    /*
     Panel 1
     Grid bag layout
     N+1 rows, N=# proxies
     M+1 columns, M=max # tasks any one proxy holds
    
     Panel 2
     J rows, J=# PMs with unallocated tasks
     2 columns, 1 for PM names, 1 for list of unallocated tasks in that PM
     */
    public AllocationFrame() {
        super("AllocationFrame");
        getContentPane().setLayout(new BorderLayout());
        allocationApplied(Engine.getInstance().getTaskAllocation());

        Engine.getInstance().addListener(this);
        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());

        pack();
        setVisible(true);
    }

    @Override
    public void allocationApplied(ResourceAllocation resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
        redrawAllocation();
    }

    private void redrawAllocation() {
        JPanel newAllocatedPanel = new JPanel();
        JPanel newUnallocatedPanel = new JPanel();
        if (resourceAllocation != null) {
            int maxTasks = 0;
            for (AbstractAsset asset : resourceAllocation.getAssetToTasks().keySet()) {
                if (resourceAllocation.getAssetToTasks().get(asset).size() > maxTasks) {
                    maxTasks = resourceAllocation.getAssetToTasks().get(asset).size();
                }
            }
            newAllocatedPanel.setLayout(new GridLayout(resourceAllocation.getAssetToTasks().size(), maxTasks + 1));
            for (AbstractAsset asset : resourceAllocation.getAssetToTasks().keySet()) {
                // Asset
                JLabel assetL = new JLabel(asset.getName());
                ProxyInt proxy = Engine.getInstance().getProxyServer().getProxy(asset);
                if (proxy instanceof BoatProxy) {
                    assetL.setBorder(BorderFactory.createLineBorder(((BoatProxy) proxy).getColor(), BORDER_WIDTH));
                }
                newAllocatedPanel.add(assetL);

                // Asset's tasks
                int blankSpaces = maxTasks;
                for (ITask iTask : resourceAllocation.getAssetToTasks().get(asset)) {
                    PlanManager pm = Engine.getInstance().getPlanManager((Task) iTask);
                    Color[] pmColors = Engine.getInstance().getPlanManagerColor(pm);
                    JLabel taskL = new JLabel(iTask.getName());
                    taskL.setBorder(BorderFactory.createLineBorder(pmColors[0], BORDER_WIDTH));
                    newAllocatedPanel.add(taskL);
                    blankSpaces--;
                }
                // Fill in remaining spaces in this row
                while (blankSpaces > 0) {
                    newAllocatedPanel.add(new JLabel(""));
                    blankSpaces--;
                }
            }

            // newUnallocatedPanel
            HashMap<PlanManager, ArrayList<Task>> pmToUnallocated = new HashMap<PlanManager, ArrayList<Task>>();
            for (ITask iTask : resourceAllocation.getUnallocatedTasks()) {
                PlanManager pm = Engine.getInstance().getPlanManager((Task) iTask);
                ArrayList<Task> tasks;
                if (!pmToUnallocated.containsKey(pm)) {
                    tasks = new ArrayList<Task>();
                    pmToUnallocated.put(pm, tasks);
                } else {
                    tasks = pmToUnallocated.get(pm);
                }
                tasks.add((Task) iTask);
            }
            if (pmToUnallocated.size() > 0) {
                newUnallocatedPanel.setLayout(new GridLayout(pmToUnallocated.size(), 2));
                for (PlanManager pm : pmToUnallocated.keySet()) {
                    // PM
                    Color[] pmColors = Engine.getInstance().getPlanManagerColor(pm);
                    JLabel planL = new JLabel(pm.getPlanName());
                    planL.setBackground(pmColors[0]);
                    planL.setForeground(pmColors[1]);
                    planL.setOpaque(true);
                    newUnallocatedPanel.add(planL);

                    // PM's unallocated tasks
                    JLabel taskL = new JLabel(pmToUnallocated.get(pm).toString());
                    newUnallocatedPanel.add(taskL);
                }
            }
        }
        getContentPane().removeAll();
        getContentPane().add(newAllocatedPanel, BorderLayout.NORTH);
        getContentPane().add(newUnallocatedPanel, BorderLayout.SOUTH);
        validate();
    }

    @Override
    public void taskCompleted(ITask task) {
        AbstractAsset asset = resourceAllocation.getTaskToAsset().get(task);
        Map<AbstractAsset, ArrayList<ITask>> assetToTasks = resourceAllocation.getAssetToTasks();
        if (!assetToTasks.containsKey(asset)) {
            LOGGER.severe("Allocation's assetToTasks did not contain asset [" + asset + "]");
            return;
        }
        ArrayList<ITask> tasks = assetToTasks.get(asset);
        tasks.remove(task);
        redrawAllocation();
    }

    @Override
    public void toUiMessageReceived(ToUiMessage m) {
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
    }

    @Override
    public UiClientInt getUiClient() {
        return uiClient;
    }

    @Override
    public void setUiClient(UiClientInt uiClient) {
        if (this.uiClient != null) {
            this.uiClient.removeClientListener(this);
        }
        this.uiClient = uiClient;
        if (uiClient != null) {
            uiClient.addClientListener(this);
        }
    }

    @Override
    public UiServerInt getUiServer() {
        return uiServer;
    }

    @Override
    public void setUiServer(UiServerInt uiServer) {
        this.uiServer = uiServer;
    }
}
