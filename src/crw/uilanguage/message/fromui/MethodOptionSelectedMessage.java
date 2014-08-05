/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package crw.uilanguage.message.fromui;

import crw.Coordinator;
import java.util.UUID;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class MethodOptionSelectedMessage extends FromUiMessage{

    protected Coordinator.Method method;
    
    public MethodOptionSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Coordinator.Method method) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        
        this.method = method;
    }
    
    
    public Coordinator.Method getMethod() {
        return method;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }
    
}
