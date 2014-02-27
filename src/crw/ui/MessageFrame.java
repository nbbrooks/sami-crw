package crw.ui;

import java.awt.BorderLayout;
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

    public static void main(String[] args) {
        MessageFrame mf = new MessageFrame();
    }
}
