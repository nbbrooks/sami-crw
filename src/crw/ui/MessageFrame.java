package crw.ui;

import sami.gui.GuiElementSpec;
import java.awt.BorderLayout;
import java.util.ArrayList;
import sami.uilanguage.UiFrame;

/**
 *
 * @author nbb
 */
public class MessageFrame extends UiFrame {

    public MessageFrame() {
        super("MessageFrame");
        
        getContentPane().setLayout(new BorderLayout());
        
        MessagePanel messagePanel = new MessagePanel();
        getContentPane().add(messagePanel, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }
    
    @Override
    public void setGUISpec(ArrayList<GuiElementSpec> guiElements) {
        System.out.println("Not supported yet.");
    }
    
    public static void main(String[] args) {
        MessageFrame mf = new MessageFrame();
    }
}
