package crw.uilanguage.message.fromui;

import java.util.UUID;
import sami.uilanguage.fromui.FromUiMessage;

/**
 *
 * @author nbb
 */
public class VariableSelectedMessage extends FromUiMessage {

    protected String variableName;

    public VariableSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, String variableName) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }

    public String toString() {
        return "VariableSelectedMessage [" + variableName + "]";
    }
}
