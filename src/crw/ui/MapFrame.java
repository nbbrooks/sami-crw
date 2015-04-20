package crw.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import sami.uilanguage.UiFrame;
import crw.ui.widget.SensorDataWidget;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.RobotWidget.ControlMode;
import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import java.util.UUID;
import sami.engine.Engine;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class MapFrame extends UiFrame implements UiClientListenerInt {

    public WorldWindPanel wwPanel;
    UiClientInt uiClient;
    UiServerInt uiServer;

    public MapFrame() {
        super("MapFrame");
        getContentPane().setLayout(new BorderLayout());

        // Add map
        wwPanel = new WorldWindPanel();
        wwPanel.createMap();
        getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
        SensorDataWidget data = new SensorDataWidget(wwPanel);
        wwPanel.addWidget(data);
        ArrayList<ControlMode> controlModes = new ArrayList<ControlMode>();
        controlModes.add(ControlMode.TELEOP);
        controlModes.add(ControlMode.POINT);
        controlModes.add(ControlMode.PATH);
        controlModes.add(ControlMode.NONE);
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
        MapFrame mf = new MapFrame();
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
