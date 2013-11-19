package crw.task;

import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.mission.planning.task.Task;
import com.perc.mitpas.adi.mission.planning.task.TaskUnitRequirement;

/**
 * Take an electroconductivity measurement
 * 
 * @author nbb
 */
public class EcMeasTask extends Task {

    public EcMeasTask() {
        super();
        init();
    }

    public EcMeasTask(String name) {
        super(name);
        init();
    }

    public void init() {
        TaskUnitRequirement ur = new TaskUnitRequirement();
        ur.addRequiredFeatureType(new FeatureType("ES2"));
        addWho(ur);
    }
}
