package crw.uilanguage.message.fromui;

import java.util.ArrayList;
import java.util.UUID;
import sami.proxy.ProxyInt;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author nbb
 */
public class BoatProxyListSelectedMessage extends FromUiMessage {

    protected ArrayList<ProxyInt> boatProxyList;

    public BoatProxyListSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, ArrayList<ProxyInt> boatProxyList) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.boatProxyList = boatProxyList;
    }

    public ArrayList<ProxyInt> getBoatProxyList() {
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