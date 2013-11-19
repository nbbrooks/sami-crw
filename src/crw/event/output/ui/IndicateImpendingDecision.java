package crw.event.output.ui;

import sami.event.OutputEvent;
import java.util.UUID;

public class IndicateImpendingDecision extends OutputEvent {

    public IndicateImpendingDecision() {
        id = UUID.randomUUID();
    }
}
