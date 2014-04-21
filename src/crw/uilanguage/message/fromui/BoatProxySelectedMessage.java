package crw.uilanguage.message.fromui;

import crw.proxy.BoatProxy;
import java.util.UUID;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author nbb
 */
public class BoatProxySelectedMessage extends FromUiMessage {

    protected BoatProxy boatProxy;

    public BoatProxySelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, BoatProxy boatProxy) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.boatProxy = boatProxy;
    }

    public BoatProxy getBoatProxy() {
        return boatProxy;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }
    
    public String toString() {
        return "BoatProxySelectedMessage [" + boatProxy + "]";
    }
}