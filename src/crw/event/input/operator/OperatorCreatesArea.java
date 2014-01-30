//package crw.event.input.operator;
//
//import sami.event.InputEvent;
//import sami.area.Area2D;
//import java.util.UUID;
//
///**
// *
// * @author nbb
// */
//public class OperatorCreatesArea extends InputEvent {
//
//    Area2D area;
//
//    public OperatorCreatesArea() {
//        id = UUID.randomUUID();
//    }
//
//    public OperatorCreatesArea(UUID relevantOutputEventUuid, UUID missionUuid, Area2D area) {
//        this.relevantOutputEventId = relevantOutputEventUuid;
//        this.missionId = missionUuid;
//        this.area = area;
//        id = UUID.randomUUID();
//    }
//
//    public Area2D getArea() {
//        return area;
//    }
//}
