package crw;

import crw.ui.component.WorldWindPanel;
import crw.ui.widget.AnnotationWidget;
import java.awt.BorderLayout;
import java.io.File;
import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import sami.engine.Mediator;
import sami.environment.EnvironmentListenerInt;
import static sami.ui.MissionMonitor.LAST_EPF_FILE;

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
        Preferences p = Preferences.userRoot();
        try {
            String lastEpfPath = p.get(LAST_EPF_FILE, null);
            if (lastEpfPath != null) {
                Mediator.getInstance().openEnvironment(new File(lastEpfPath));
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used EPF");
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
