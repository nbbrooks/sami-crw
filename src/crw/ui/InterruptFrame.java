/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crw.ui;

import java.awt.BorderLayout;
import java.util.UUID;
import javax.swing.JDialog;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiClientListenerInt;
import sami.uilanguage.UiFrame;
import sami.uilanguage.UiServerInt;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class InterruptFrame extends UiFrame {
    
    public InterruptPanel p;
    
    public InterruptFrame() {

        super("InterruptFrame");
        
        getContentPane().setLayout(new BorderLayout());
        
        p = new InterruptPanel();
        getContentPane().add(p);
        pack();
        setVisible(true);
    }


}
