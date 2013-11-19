package crw.event.output.ui;

import sami.event.OutputEvent;
import java.util.UUID;

public class FocusAttention extends OutputEvent {

    public FocusAttention() {
        id = UUID.randomUUID();
    }
}
