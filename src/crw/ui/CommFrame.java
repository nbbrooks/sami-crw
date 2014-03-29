package crw.ui;

import sami.uilanguage.UiFrame;

/**
 *
 * @author nbb
 */
public class CommFrame extends UiFrame {

    public CommPanel commPanel;

    public CommFrame() {
        super("CommFrame");

        commPanel = new CommPanel();
        getContentPane().add(commPanel);

        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        CommFrame cf = new CommFrame();
    }

}
