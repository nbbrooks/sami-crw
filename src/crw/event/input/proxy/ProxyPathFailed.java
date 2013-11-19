package crw.event.input.proxy;

import java.util.UUID;
import sami.event.InputEvent;

public class ProxyPathFailed extends InputEvent {

    public ProxyPathFailed() {
        id = UUID.randomUUID();
    }
}
