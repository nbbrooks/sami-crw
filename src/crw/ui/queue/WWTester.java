package crw.ui.queue;

import crw.ui.widget.WorldWindPanel;
import crw.uilanguage.message.toui.PathOptionsMessage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sami.path.Path;
import sami.proxy.ProxyInt;
import sami.uilanguage.toui.SelectionMessage;

/**
 *
 * @author nbb
 */
public class WWTester {

    public static int BUTTON_WIDTH = 100;
    public static int BUTTON_HEIGHT = 50;

    public static void main(String[] args) {
        SelectionMessage selectionMessage = new PathOptionsMessage(null, null, 1, new ArrayList<Hashtable<ProxyInt, Path>>());

        JPanel component = new JPanel();
        BoxLayout paramsLayout = new BoxLayout(component, BoxLayout.Y_AXIS);
        component.setLayout(paramsLayout);
        int maxColWidth = BUTTON_WIDTH;
        int cumulComponentHeight = 0;
        for (int i = 0; i < 3; i++) {
            JComponent objectVisualization = new WorldWindPanel(500, 300);
            objectVisualization.setMaximumSize(new Dimension(500, 300));
            int BORDER = 5;
            JPanel fieldPanel = new JPanel();
            BoxLayout layout = new BoxLayout(fieldPanel, BoxLayout.Y_AXIS);
            fieldPanel.setLayout(layout);
            fieldPanel.setBorder(BorderFactory.createMatteBorder(BORDER, BORDER, BORDER, BORDER, (Color.BLACK)));
            fieldPanel.add(objectVisualization);
            maxColWidth = Math.max(maxColWidth, (int) objectVisualization.getMaximumSize().getWidth() + BUTTON_WIDTH);
            cumulComponentHeight += Math.max((int) objectVisualization.getMaximumSize().getHeight(), BUTTON_HEIGHT);
            component.add(fieldPanel);
        }
        component.setSize(new Dimension(maxColWidth, cumulComponentHeight));
        component.setMinimumSize(new Dimension(maxColWidth, cumulComponentHeight));
        component.setMaximumSize(new Dimension(maxColWidth, cumulComponentHeight));
        component.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));

        ScrollPane jScrollPane1 = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        jScrollPane1.add(component);
        jScrollPane1.setPreferredSize(new Dimension(500, 500));

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("top"));
        topPanel.setPreferredSize(new Dimension(500, 500));
        JPanel btmPanel = new JPanel();
        btmPanel.add(new JLabel("bottom"));
        btmPanel.setPreferredSize(new Dimension(500, 500));

        JFrame frame = new JFrame();
        BoxLayout paneLayout = new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS);
        frame.getContentPane().setLayout(paneLayout);
        frame.getContentPane().add(topPanel);
        frame.getContentPane().add(jScrollPane1);
        frame.getContentPane().add(btmPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
