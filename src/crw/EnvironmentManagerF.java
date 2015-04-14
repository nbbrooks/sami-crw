package crw;

import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import sami.engine.Mediator;
import sami.environment.EnvironmentListenerInt;

/**
 *
 * @author nbb
 */
public class EnvironmentManagerF extends JFrame implements EnvironmentListenerInt {

    private static final Logger LOGGER = Logger.getLogger(EnvironmentManagerF.class.getName());
    WorldWindPanel wwPanel;

    public EnvironmentManagerF() {
        super("EnvironmentManagerF");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Add map
        wwPanel = new WorldWindPanel();
        wwPanel.createMap();
        getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
        AnnotationWidget annotation = new AnnotationWidget(wwPanel);
        wwPanel.addWidget(annotation);

        pack();

        Mediator.getInstance().addEnvironmentListener(this);

        // Try to load the last used EPF file
        LOGGER.info("Load EPF");
        boolean success = Mediator.getInstance().openLatestEnvironment();
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load previous environment, opening new environment");
            Mediator.getInstance().newEnvironment();
        }
    }

    @Override
    public void environmentUpdated() {
        applyEnvironmentValues();
    }

    private void applyEnvironmentValues() {
        if (Mediator.getInstance().getEnvironmentFile() == null) {
            setTitle("Not saved");
        } else {
            setTitle(Mediator.getInstance().getEnvironmentFile().getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new EnvironmentManagerF().setVisible(true);
            }
        });
    }
}
