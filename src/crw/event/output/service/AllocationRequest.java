package crw.event.output.service;

import sami.event.OutputEvent;
import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AllocationRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public ArrayList<ITask> tasks = null;
    public ArrayList<AbstractAsset> assets = null;
    /**
     * Available time to complete request, in ms
     *
     * <=0 means no time limit, get optimal
     *
     * Preference here would be for some function over preferences for
     * time/quality tradeoss, not a time limit
     */
    public long availableTime = 0L;
    public int noOptions = 1;

    static {
        fieldNames.add("availableTime");
        fieldNames.add("noOptions");

        fieldNameToDescription.put("availableTime", "How long until the result is needed? (seconds)");
        fieldNameToDescription.put("noOptions", "How many options to present?");
    }

    public AllocationRequest() {
        id = UUID.randomUUID();
    }

//    @Override
//    public HashMap<String, String> getParamNames() {
//        HashMap<String, String> ret = new HashMap<String, String>();
//
//        ret.put("Tasks", "java.util.ArrayList");
//        ret.put("Assets", "java.util.ArrayList");
//        ret.put("Constraints", "java.util.ArrayList");
//
//        return ret;
//    }
//
//    @Override
//    public void instantiate(HashMap<String, Object> params) {
//        tasks = (ArrayList<ITask>) params.get("Tasks");
//        assets = (ArrayList<AbstractAsset>) params.get("Assets");
//        constraints = (ArrayList<Constraint>) params.get("Constraints");
//    }
//
//    @Override
//    public void instantiateValue(String s, Object p) {
//        params.put(s, p);
//    }
//
    public ArrayList<AbstractAsset> getAssets() {
        if (assets == null) {
            assets = new ArrayList<AbstractAsset>();
        }
        return assets;
    }

    public void setAssets(ArrayList<AbstractAsset> assets) {
        this.assets = assets;
    }

    public ArrayList<ITask> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<ITask>();
        }

        return tasks;
    }

    public void setTasks(ArrayList<ITask> tasks) {
        this.tasks = tasks;
    }

    public long getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(long availableTime) {
        this.availableTime = availableTime;
    }

    public int getNoOptions() {
        return noOptions;
    }

    public void setNoOptions(int noOptions) {
        this.noOptions = noOptions;
    }
//    public ArrayList<Constraint> getConstraints() {
//        return constraints;
//    }
//
//    public void setConstraints(ArrayList<Constraint> constraints) {
//        this.constraints = constraints;
//    }
    
    public String toString() {
        return "AllocationRequest [" + tasks + ", " + assets + ", " + availableTime + ", " + noOptions + "]";
    }
}
