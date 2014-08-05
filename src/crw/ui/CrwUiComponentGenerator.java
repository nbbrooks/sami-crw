package crw.ui;

import crw.ui.component.TextPanel;
import crw.ui.component.WorldWindPanel;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.UiComponentGeneratorInt;

/**
 *
 * @author nbb
 */
public class CrwUiComponentGenerator implements UiComponentGeneratorInt {

    private final static Logger LOGGER = Logger.getLogger(CrwUiComponentGenerator.class.getName());
    public static final ArrayList<Class> componentClasses = new ArrayList<Class>();

    static {
        // @todo move this into a config file
        componentClasses.add(TextPanel.class);
        componentClasses.add(WorldWindPanel.class);
    }

    private static class CrwUiComponentGeneratorHolder {

        public static final CrwUiComponentGenerator INSTANCE = new CrwUiComponentGenerator();
    }

    private CrwUiComponentGenerator() {
    }

    public static CrwUiComponentGenerator getInstance() {
        return CrwUiComponentGenerator.CrwUiComponentGeneratorHolder.INSTANCE;
    }

    public JComponent getFakeComponent() {
        JComponent component = new JLabel("No handler");

        int numAllocs = 3;
        Random random = new Random();
        Object[][] table = new Object[numAllocs][3];
        Object[] header = new Object[]{"", "", ""};
        int row = 0;
        for (int alloc = 0; alloc < numAllocs; alloc++) {
            table[row][0] = random.nextDouble() + " (" + random.nextDouble() + ")";
            table[row][1] = " \u21e8 ";
            table[row][2] = random.nextDouble() + " (" + random.nextDouble() + ")";
            row++;

        }
        component = new JTable(table, header);
        ((JTable) component).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int width;
        for (int c = 0; c < table[0].length; c++) {
            width = 0;
            for (int r = 0; r < table.length; r++) {
                TableCellRenderer renderer = ((JTable) component).getCellRenderer(r, c);
                Component comp = ((JTable) component).prepareRenderer(renderer, r, c);
                width = Math.max(comp.getPreferredSize().width + ((JTable) component).getIntercellSpacing().width, width);
            }
            ((JTable) component).getColumnModel().getColumn(c).setMaxWidth(width);
        }
        ((JTable) component).setShowGrid(false);
        return component;
    }

    @Override
    public MarkupComponent getCreationComponent(Type type, ArrayList<Markup> markups) {
        // Find the UiComponent class that can be used to create the object and supports the largest number of markups
        Class bestClass = null;
        int bestScore = -1;
        for (Class compClass : componentClasses) {
            if ((MarkupComponent.class).isAssignableFrom(compClass)) {
                try {
                    MarkupComponent temp = (MarkupComponent) compClass.newInstance();
                    int score = temp.getCreationComponentScore(type, markups);
//                    System.out.println("### Creation component score of class " + compClass + " for object class " + objectClass.getSimpleName() + " and markups " + markups.toString() + " is " + score);
                    if (score > -1 && score > bestScore) {
                        bestScore = score;
                        bestClass = compClass;
                    }
                } catch (InstantiationException ex) {
                    Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        MarkupComponent component = null;
        if (bestClass != null) {
//            System.out.println("### Best creation class for " + objectClass.getSimpleName() + " is " + bestClass.getSimpleName());
            try {
                MarkupComponent temp = (MarkupComponent) bestClass.newInstance();
                component = temp.useCreationComponent(type, markups);
            } catch (InstantiationException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.severe("Could not find creation component for object of type: " + type + " with markups: " + markups.toString());
        }
        return component;
    }

    @Override
    public MarkupComponent getSelectionComponent(Type type, Object value, ArrayList<Markup> markups) {
        // Find the UiComponent class that can be used to create the object and supports the largest number of markups
        Class bestClass = null;
        int bestScore = -1;
        for (Class compClass : componentClasses) {
            if ((MarkupComponent.class).isAssignableFrom(compClass)) {
                try {
                    MarkupComponent temp = (MarkupComponent) compClass.newInstance();
                    int score = temp.getSelectionComponentScore(type, markups);
                    
//                     System.out.println("### selection type: "+type.toString());
//                    System.out.println("### selection Creation component score of class " + compClass + " for object class " + compClass.getSimpleName() + " and markups " + markups.toString() + " is " + score);
//                    System.out.println("### Selection component score of class " + compClass + " for object class " + selectionObject.getClass().getSimpleName() + " and markups " + markups.toString() + " is " + score);
                    if (score > -1 && score > bestScore) {
                        bestScore = score;
                        bestClass = compClass;
                    }
                } catch (InstantiationException ex) {
                    Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        MarkupComponent component = null;
        if (bestClass != null) {
//            System.out.println("### Best selection class for " + selectionObject.getClass().getSimpleName() + " is " + bestClass.getSimpleName());
            try {
                MarkupComponent temp = (MarkupComponent) bestClass.newInstance();
                component = temp.useSelectionComponent(value, markups);
            } catch (InstantiationException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.severe("Could not find selection component for object class: " + value.getClass().getSimpleName() + " with markups: " + markups.toString());
        }
        return component;
    }

    @Override
    public Object getComponentValue(MarkupComponent component, Class componentClass) {
        return component.getComponentValue(componentClass);
    }

    @Override
    public boolean setComponentValue(MarkupComponent component, Object value) {
        return component.setComponentValue(value);
    }

    @Override
    public ArrayList<Class> getCreationClasses() {
        ArrayList<Class> creationClasses = new ArrayList<Class>();
        for (Class compClass : componentClasses) {
            try {
                MarkupComponent temp = (MarkupComponent) compClass.newInstance();
                ArrayList<Class> compCreationClasses = temp.getSupportedCreationClasses();
                for (Class creationClass : compCreationClasses) {
                    if (!creationClasses.contains(creationClass)) {
                        creationClasses.add(creationClass);
                    }
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CrwUiComponentGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return creationClasses;
    }
}
