package crw.task;

import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.mission.planning.task.Task;
import com.perc.mitpas.adi.mission.planning.task.TaskUnitRequirement;

/**
 * Take a picture
 * 
 * @author nbb
 */
public class CollectSampleTask extends Task {

    public CollectSampleTask() {
        super();
        init();
    }

    public CollectSampleTask(String name) {
        super(name);
        init();
    }

    public void init() {
        TaskUnitRequirement ur = new TaskUnitRequirement();
        ur.addRequiredFeatureType(new FeatureType("Sample"));
        addWho(ur);
    }
}
