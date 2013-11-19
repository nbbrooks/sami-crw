package crw.event.output.ui;

import sami.event.OutputEvent;
import java.util.UUID;

public class DisplayMessage extends OutputEvent {

    public String message;

    public DisplayMessage() {
        id = UUID.randomUUID();
    }

    public String getMessage() {
        return message;
    }
}
