package crw.ui.tests;

import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.SensorDataQuadtreeWidget;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.UUID;
import sami.engine.Engine;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class SensorTest extends UiFrame implements UiClientListenerInt {

    public WorldWindPanel wwPanel;
    UiClientInt uiClient;
    UiServerInt uiServer;
    public SensorDataQuadtreeWidget data;

    public SensorTest() {
        super("SensorTest");
        getContentPane().setLayout(new BorderLayout());

        // Add map
        wwPanel = new WorldWindPanel();
        wwPanel.createMap(800, 600, null);
        getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
        data = new SensorDataQuadtreeWidget(wwPanel);
        wwPanel.addWidget(data);
        ArrayList<RobotWidget.ControlMode> controlModes = new ArrayList<RobotWidget.ControlMode>();
        controlModes.add(RobotWidget.ControlMode.TELEOP);
        controlModes.add(RobotWidget.ControlMode.POINT);
        controlModes.add(RobotWidget.ControlMode.PATH);
        controlModes.add(RobotWidget.ControlMode.NONE);
        RobotWidget robot = new RobotWidget(wwPanel, controlModes);
        wwPanel.addWidget(robot);
        RobotTrackWidget robotTrack = new RobotTrackWidget(wwPanel);
        wwPanel.addWidget(robotTrack);
        AnnotationWidget annotation = new AnnotationWidget(wwPanel);
        wwPanel.addWidget(annotation);

        setUiClient(Engine.getInstance().getUiClient());
        setUiServer(Engine.getInstance().getUiServer());

        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        final SensorTest st = new SensorTest();

//        (new Thread() {
//            public void run() {
//                Random r = new Random();
//                int count = 500;
//                while(count > 0) {
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(SensorTest.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                   
//                    UTMCoordinate coord = new UTMCoordinate(2804466 + r.nextDouble() * 1000 - 500, 553327 + r.nextDouble() * 1000 - 500, "39R");
//                    Location l = new Location(coord, 0);
//                    Observation o = new Observation("NONE", r.nextDouble() * 100, "P1", l, System.currentTimeMillis());
////                    UTMCoordinate coord = new UTMCoordinate(2804466 + count * 10, 553327 + count * 10, "39R");
////                    Location l = new Location(coord, 0);
////                    Observation o = new Observation("NONE", (count + 1) * 10, "P1", l, System.currentTimeMillis());
//                    st.data.newObservation(o);
//                    
//                    count--;
//                }
//            }
//        }).start();
    }

    @Override
    public void toUiMessageReceived(ToUiMessage m) {
        // Check for markups in any message
        wwPanel.handleMarkups(m.getMarkups(), null);
    }

    @Override
    public void toUiMessageHandled(UUID toUiMessageId) {
    }

    @Override
    public UiClientInt getUiClient() {
        return uiClient;
    }

    @Override
    public void setUiClient(UiClientInt uiClient) {
        if (this.uiClient != null) {
            this.uiClient.removeClientListener(this);
        }
        this.uiClient = uiClient;
        if (uiClient != null) {
            uiClient.addClientListener(this);
        }
    }

    @Override
    public UiServerInt getUiServer() {
        return uiServer;
    }

    @Override
    public void setUiServer(UiServerInt uiServer) {
        this.uiServer = uiServer;
    }
}
