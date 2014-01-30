package crw.ui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import sami.markup.Markup;
import sami.markup.RelevantInformation;
import sami.markup.RelevantProxy;

/**
 *
 * @author nbb
 */
public class UiComponent extends JPanel {

    public static List<Class> supportedCreationClasses = new ArrayList<Class>();
    public static List<Class> supportedSelectionClasses = new ArrayList<Class>();
    public static List<Enum> supportedMarkups = new ArrayList<Enum>();
    public static List<Class> widgetClasses = new ArrayList<Class>();

    public static int getCreationComponentScore(Class creationClass, ArrayList<Markup> markups) {
        int score = -1;
        if (supportedCreationClasses.contains(creationClass)) {
            score = 0;
        } else {
            for (Class widgetClass : widgetClasses) {
                if ((UiWidget.class).isAssignableFrom(widgetClass)) {
                    try {
                        List<Class> creationClasses = (List<Class>) widgetClass.getField("supportedCreationClasses").get(null);
                        if (creationClasses.contains(creationClass)) {
                            score = 0;
                        }
                    } catch (NoSuchFieldException ex) {
                        Logger.getLogger(UiComponent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (SecurityException ex) {
                        Logger.getLogger(UiComponent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(UiComponent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(UiComponent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (score < 0) {
            return score;
        }

        for (Markup markup : markups) {
        }

        return score;
    }

//    public static int getDisplayComponentScore(Class displayClass, ArrayList<Markup> markups) {
//        int score = -1;
//        if (supportedSelectionClasses) {
//            return score;
//        }
//    }
    public static void main(String[] args) {
        ArrayList<Enum> supMarkups = new ArrayList<Enum>();
        supMarkups.add(RelevantInformation.Information.SPECIFY);
        supMarkups.add(RelevantInformation.Visualization.CONTOUR);
        supMarkups.add(RelevantInformation.Visualization.HEATMAP);

        System.out.println(supMarkups.toString());

        System.out.println(supMarkups.contains(RelevantInformation.Visualization.HEATMAP));
        System.out.println(supMarkups.contains(RelevantProxy.Proxies.ALL_PROXIES));
    }
}
