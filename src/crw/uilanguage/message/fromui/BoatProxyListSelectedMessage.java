package crw.uilanguage.message.fromui;

import crw.proxy.BoatProxy;
import java.util.ArrayList;
import java.util.UUID;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author nbb
 */
public class BoatProxyListSelectedMessage extends FromUiMessage {

    protected ArrayList<BoatProxy> boatProxyList;

    public BoatProxyListSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, ArrayList<BoatProxy> boatProxyList) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.boatProxyList = boatProxyList;
    }

    public ArrayList<BoatProxy> getBoatProxyList() {
        return boatProxyList;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }
    
    public String toString() {
        return "BoatProxyListSelectedMessage [" + boatProxyList + "]";
    }
}