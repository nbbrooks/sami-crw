package crw.task;

import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.mission.planning.task.Task;
import com.perc.mitpas.adi.mission.planning.task.TaskUnitRequirement;

/**
 * Take a picture
 * 
 * @author nbb
 */
public class PictureTask extends Task {

    public PictureTask() {
        super();
        init();
    }

    public PictureTask(String name) {
        super(name);
        init();
    }

    public void init() {
        TaskUnitRequirement ur = new TaskUnitRequirement();
        ur.addRequiredFeatureType(new FeatureType("Camera"));
        addWho(ur);
    }
}
