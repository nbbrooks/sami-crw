package crw.ui;

import sami.gui.GuiElementSpec;
import java.awt.BorderLayout;
import java.util.ArrayList;
import sami.uilanguage.UiFrame;
import crw.ui.widget.SensorDataWidget;
import crw.ui.widget.RobotTrackWidget;
import crw.ui.widget.RobotWidget;
import crw.ui.widget.RobotWidget.ControlMode;
import crw.ui.widget.WorldWindPanel;

/**
 *
 * @author nbb
 */
public class MapFrame extends UiFrame {

    public WorldWindPanel wwPanel;

    public MapFrame() {
        super("MapFrame");
        getContentPane().setLayout(new BorderLayout());

        // Add map
        wwPanel = new WorldWindPanel();
        getContentPane().add(wwPanel, BorderLayout.CENTER);
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

        pack();
        setVisible(true);
    }

    @Override
    public void setGUISpec(ArrayList<GuiElementSpec> guiElements) {
        System.out.println("Not supported yet.");
    }

    public static void main(String[] args) {
        MapFrame mf = new MapFrame();
    }
}
