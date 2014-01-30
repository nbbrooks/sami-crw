package crw.ui.component;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.markup.Attention;
import sami.markup.Priority;

/**
 *
 * @author nbb
 */
public class TextPanel extends UiComponent {

    static {
        computeUiComponent();
    }

    public TextPanel() {
    }

    public static void computeUiComponent() {
        // Creation
        supportedCreationClasses.add(String.class);
        supportedCreationClasses.add(Double.class);
        supportedCreationClasses.add(Float.class);
        supportedCreationClasses.add(Integer.class);
        supportedCreationClasses.add(Long.class);
        supportedCreationClasses.add(double.class);
        supportedCreationClasses.add(float.class);
        supportedCreationClasses.add(int.class);
        supportedCreationClasses.add(long.class);

        // Visualization
        supportedSelectionClasses.add(String.class);
        supportedSelectionClasses.add(Double.class);
        supportedSelectionClasses.add(Float.class);
        supportedSelectionClasses.add(Integer.class);
        supportedSelectionClasses.add(Long.class);
        supportedSelectionClasses.add(double.class);
        supportedSelectionClasses.add(float.class);
        supportedSelectionClasses.add(int.class);
        supportedSelectionClasses.add(long.class);

        // Markups
        supportedMarkups.add(Attention.AttentionEnd.ON_CLICK);
        supportedMarkups.add(Attention.AttentionTarget.FRAME);
        supportedMarkups.add(Attention.AttentionTarget.PANEL);
        supportedMarkups.add(Attention.AttentionType.BLINK);
        supportedMarkups.add(Attention.AttentionType.HIGHLIGHT);
        supportedMarkups.add(Priority.Ranking.LOW);
        supportedMarkups.add(Priority.Ranking.MEDIUM);
        supportedMarkups.add(Priority.Ranking.HIGH);
        supportedMarkups.add(Priority.Ranking.CRITICAL);

        for (Class widgetClass : widgetClasses) {
            if ((UiWidget.class).isAssignableFrom(widgetClass)) {
                try {
                    Field field = widgetClass.getField("supportedMarkups");
                    System.out.println(field);
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(WorldWindPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}