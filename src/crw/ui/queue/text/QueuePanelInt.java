package crw.ui.queue.text;

import java.util.UUID;

/**
 *
 * @author owens
 * @author nbb
 */
public interface QueuePanelInt {

    public QueueItemText getCurrentContent();

    public boolean removeMessageId(UUID messageId);

    public int removeMissionId(UUID missionId);

    public void setIsActive(boolean active);
}
