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

    public AllocationRequest() {
        id = UUID.randomUUID();
    }

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

//    public ArrayList<Constraint> getConstraints() {
//        return constraints;
//    }
//
//    public void setConstraints(ArrayList<Constraint> constraints) {
//        this.constraints = constraints;
//    }
    public String toString() {
        return "AllocationRequest [" + tasks + ", " + assets + "]";
    }
}
