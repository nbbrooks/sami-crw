package crw.event.output.proxy;

import sami.event.OutputEvent;
import sami.area.Area2D;
import java.util.UUID;

public class ProxyExploreArea extends OutputEvent {

    public Area2D area;

    public ProxyExploreArea() {
        id = UUID.randomUUID();
    }

    public ProxyExploreArea(UUID uuid, UUID missionUuid, Area2D area) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.area = area;
    }

    public Area2D getArea() {
        return area;
    }

    public void setArea(Area2D area) {
        this.area = area;
    }
}