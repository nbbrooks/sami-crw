package crw.ui.queue;

import java.util.UUID;

/**
 *
 * @author owens
 * @author nbb
 */
public interface QueuePanelInt {

    public void hideContentPanel();

    public void showContentPanel();

    public boolean removeMessageId(UUID messageId);

    public int removeMissionId(UUID missionId);
}
