package crw.event.output.ui;

import sami.event.OutputEvent;
import java.util.UUID;

public class NewMissionPhase extends OutputEvent {

    public String phaseName;
    public boolean highlighted;

    public NewMissionPhase() {
        id = UUID.randomUUID();
    }
}
