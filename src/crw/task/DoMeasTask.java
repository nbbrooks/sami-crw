package crw.task;

import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.mission.planning.task.Task;
import com.perc.mitpas.adi.mission.planning.task.TaskUnitRequirement;

/**
 * Take a Dissolved Oxygen measurement
 * 
 * @author nbb
 */
public class DoMeasTask extends Task {

    public DoMeasTask() {
        super();
        init();
    }

    public DoMeasTask(String name) {
        super(name);
        init();
    }

    public void init() {
        TaskUnitRequirement ur = new TaskUnitRequirement();
        ur.addRequiredFeatureType(new FeatureType("DO"));
        addWho(ur);
    }
}
