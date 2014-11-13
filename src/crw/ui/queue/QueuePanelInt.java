package crw.ui.queue;

import java.util.UUID;

/**
 *
 * @author owens
 * @author nbb
 */
public interface QueuePanelInt {

    public QueueItem getCurrentContent();

    public boolean removeMessageId(UUID messageId);

    public int removeMissionId(UUID missionId);

    public void setIsActive(boolean active);
}
