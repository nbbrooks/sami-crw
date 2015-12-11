package crw.uilanguage.message.fromui;

import crw.event.output.proxy.BoatProxyId;
import java.util.UUID;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author nbb
 */
public class BoatIdSelectedMessage extends FromUiMessage {

    protected BoatProxyId boatId;

    public BoatIdSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, BoatProxyId boatId) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.boatId = boatId;
    }

    public BoatProxyId getBoatId() {
        return boatId;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }

    public String toString() {
        return "BoatIdSelectedMessage [" + boatId + "]";
    }
}
