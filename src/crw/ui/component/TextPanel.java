package crw.ui.component;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.proxy.BoatProxy;
import crw.ui.ColorSlider;
import dreaam.developer.UiComponentGenerator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import sami.allocation.ResourceAllocation;
import sami.engine.Mediator;
import sami.event.Event;
import sami.markup.Markup;
import sami.mission.MissionPlanSpecification;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupComponentWidget;
import sami.uilanguage.MarkupManager;
import sami.variable.VariableName;

/**
 *
 * @author nbb
 */
public class TextPanel implements MarkupComponent {

    // MarkupComponent variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses = new Hashtable<Class, ArrayList<Class>>();
    public final Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses = new Hashtable<Class, ArrayList<Class>>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    public final ArrayList<Class> widgetClasses = new ArrayList<Class>();
    public JComponent component = null;
    //
    private final static Logger LOGGER = Logger.getLogger(TextPanel.class.getName());

    public TextPanel() {
        populateLists();
    }

    private void populateLists() {
        // Creation
        supportedCreationClasses.add(String.class);
        supportedCreationClasses.add(Double.class);
        supportedCreationClasses.add(Float.class);
        supportedCreationClasses.add(Integer.class);
        supportedCreationClasses.add(Long.class);
        supportedCreationClasses.add(Boolean.class);
        supportedCreationClasses.add(double.class);
        supportedCreationClasses.add(float.class);
        supportedCreationClasses.add(int.class);
        supportedCreationClasses.add(long.class);
        supportedCreationClasses.add(boolean.class);
        supportedCreationClasses.add(Enum.class);
        supportedCreationClasses.add(Color.class);
        supportedCreationClasses.add(MissionPlanSpecification.class);
        supportedCreationClasses.add(VariableName.class);
        // Visualization
        supportedSelectionClasses.add(String.class);
        supportedSelectionClasses.add(Double.class);
        supportedSelectionClasses.add(Float.class);
        supportedSelectionClasses.add(Integer.class);
        supportedSelectionClasses.add(Long.class);
        supportedSelectionClasses.add(Boolean.class);
        supportedSelectionClasses.add(double.class);
        supportedSelectionClasses.add(float.class);
        supportedSelectionClasses.add(int.class);
        supportedSelectionClasses.add(long.class);
        supportedSelectionClasses.add(boolean.class);
        supportedSelectionClasses.add(Enum.class);
        supportedSelectionClasses.add(Color.class);
        supportedSelectionClasses.add(ResourceAllocation.class);
        supportedSelectionClasses.add(BoatProxy.class);
        supportedSelectionClasses.add(MissionPlanSpecification.class);
        supportedSelectionClasses.add(VariableName.class);
        // Markups
        // Instructional text
    }

    @Override
    public int getCreationComponentScore(Type type, Field field, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationComponentScore(supportedCreationClasses, supportedHashtableCreationClasses, supportedMarkups, widgetClasses, type, field, markups);
    }

    @Override
    public int getSelectionComponentScore(Type type, Object object, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionComponentScore(supportedSelectionClasses, supportedHashtableSelectionClasses, supportedMarkups, widgetClasses, type, object, markups);
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        return MarkupComponentHelper.getMarkupComponentScore(supportedMarkups, widgetClasses, markups);
    }

    @Override
    public MarkupComponent useCreationComponent(Type type, Field field, ArrayList<Markup> markups) {
        if (type instanceof Class) {
            Class objectClass = (Class) type;
            if (objectClass.equals(Color.class)) {
                component = new ColorSlider();
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
            } else if (objectClass.equals(String.class)
                    || objectClass.equals(Double.class)
                    || objectClass.equals(Float.class)
                    || objectClass.equals(Integer.class)
                    || objectClass.equals(Long.class)
                    || objectClass.equals(double.class)
                    || objectClass.equals(float.class)
                    || objectClass.equals(int.class)
                    || objectClass.equals(long.class)) {
                component = new JTextField();
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
            } else if (objectClass.equals(MissionPlanSpecification.class)) {
                if (Mediator.getInstance().getProject() != null) {
                    ArrayList<MissionPlanSpecification> mSpecs = Mediator.getInstance().getProject().getAllMissionPlans();
                    mSpecs.add(0, null);
                    component = new JComboBox(mSpecs.toArray());
                }
                if (component == null) {
                    LOGGER.severe("No loaded project spec to retrieve mission plan list from");
                }
            } else if (objectClass.equals(Boolean.class)
                    || objectClass.equals(boolean.class)) {
                component = new JComboBox(new Object[]{null, true, false});
            } else if (objectClass.isEnum()) {
                Object[] enumConstants = objectClass.getEnumConstants();
                Vector<Object> vector = new Vector<Object>(enumConstants.length + 1);
                vector.add(null);
                for (Object o : enumConstants) {
                    vector.add(o);
                }
                component = new JComboBox(vector);
            } else if (objectClass.equals(VariableName.class)) {
                ArrayList<String> existingVariables = Mediator.getInstance().getProject().getVariables();
                ArrayList<VariableName> variableNames = new ArrayList<VariableName>();
                variableNames.add(null);
                for (String var : existingVariables) {
                    variableNames.add(new VariableName(var));
                }
                component = new JComboBox(variableNames.toArray());
            } else {
                LOGGER.severe("Could not generate component for object class: " + objectClass);
            }
        } else {
            LOGGER.severe("Tried to generate a componenent for type that was not a ParameterizedType or Class: " + type);

        }
        return this;
    }

    @Override
    public MarkupComponent useSelectionComponent(Object object, ArrayList<Markup> markups) {
        if (object instanceof ResourceAllocation) {
            ResourceAllocation allocation = (ResourceAllocation) object;
            Map<AbstractAsset, ArrayList<ITask>> assetToTasks = allocation.getAssetToTasks();
            if (assetToTasks.isEmpty()) {
                return null;
            }
            ArrayList<ITask> unallocatedTasks = allocation.getUnallocatedTasks();
            int numRows = unallocatedTasks.isEmpty() ? assetToTasks.size() : assetToTasks.size() + 1;
            Object[][] table = new Object[numRows][3];
            Object[] header = new Object[]{"", "", ""};
            int row = 0;
            // List tasks for each asset
            for (AbstractAsset asset : assetToTasks.keySet()) {
                table[row][0] = asset.getName() + " (" + asset.getClass().getSimpleName() + ")";
                table[row][1] = " \u21e8 ";
                String taskString = "[";
                ArrayList<ITask> iTasks = assetToTasks.get(asset);
                for (int i = 0; i < iTasks.size() - 1; i++) {
                    taskString += iTasks.get(i).getName() + " (" + iTasks.get(i).getClass().getSimpleName() + "), ";
                }
                if (!iTasks.isEmpty()) {
                    taskString += iTasks.get(iTasks.size() - 1).getName() + " (" + iTasks.get(iTasks.size() - 1).getClass().getSimpleName() + ")]";
                }
                table[row][2] = taskString;
                row++;
            }
            // List unallocated tasks if any
            if (!unallocatedTasks.isEmpty()) {
                table[row][0] = "Unallocated tasks";
                table[row][1] = ":";
                String unallocatedString = "";
                for (int i = 0; i < unallocatedTasks.size() - 1; i++) {
                    unallocatedString += unallocatedTasks.get(i).getName() + ", ";
                }
                if (!unallocatedTasks.isEmpty()) {
                    unallocatedString += unallocatedTasks.get(unallocatedTasks.size() - 1).getName();
                }
                table[row][2] = unallocatedString;
            }
            // Make the table
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
        } else if (object instanceof BoatProxy) {
            BoatProxy boatProxy = (BoatProxy) object;
            component = new JLabel(boatProxy.toString());
            component.setFont(new java.awt.Font("Lucida Grande", 1, 13));
            component.setBorder(BorderFactory.createLineBorder(boatProxy.getColor(), 6));
            component.setForeground(boatProxy.getColor());
            component.setOpaque(true);
        } else if (object instanceof Color) {
            component = new JPanel();
            component.setBackground((Color) object);
        } else if (object instanceof String
                || object instanceof Double
                || object instanceof Float
                || object instanceof Integer
                || object instanceof Long
                || object instanceof Boolean) {
            component = new JLabel(object.toString());
        } else if (object instanceof MissionPlanSpecification) {
            component = new JLabel(((MissionPlanSpecification) object).getName());
        } else if (object instanceof VariableName) {
            component = new JLabel(((VariableName) object).variableName);
        } else {
            component = new JLabel("No component found");
            LOGGER.severe("Could not selection component for object class: " + object.getClass().getSimpleName());

        }
        return this;
    }

    public JComponent handleCreationHashtable(ParameterizedType type) {
        JComponent component = new JLabel("No component found");
        return component;
    }

    public JComponent handleSelectionHashtable(Hashtable hashtable, Object keyObject, Object valueObject) {
        if (keyObject == null || valueObject == null) {
            return null;
        }
        JComponent component = null;
        return component;
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public Object getComponentValue(Class componentClass) {
        Object value = null;
        if (component instanceof JTextField) {
            String text = ((JTextField) component).getText();

            if (text.length() > 0) {
                try {
                    if (componentClass.equals(String.class)) {
                        value = text;
                    } else if (componentClass.equals(Double.class)) {
                        value = new Double(text);
                    } else if (componentClass.equals(Float.class)) {
                        value = new Float(text);
                    } else if (componentClass.equals(Integer.class)) {
                        value = new Integer(text);
                    } else if (componentClass.equals(Long.class)) {
                        value = new Long(text);
                    } else if (componentClass.equals(Boolean.class)) {
                        value = Boolean.valueOf(text);
                    } else if (componentClass.equals(double.class)) {
                        value = Double.parseDouble(text);
                    } else if (componentClass.equals(float.class)) {
                        value = Float.parseFloat(text);
                    } else if (componentClass.equals(int.class)) {
                        value = Integer.parseInt(text);
                    } else if (componentClass.equals(long.class)) {
                        value = Long.parseLong(text);
                    } else if (componentClass.equals(boolean.class)) {
                        value = Boolean.parseBoolean(text);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Exception encountered when trying to get value for field type: " + componentClass + " from text: " + text + ", setting value to null with exception: " + e);
                    value = null;
                }
            }
        } else if (component instanceof JComboBox) {
            value = ((JComboBox) component).getSelectedItem();
        } else if (component instanceof ColorSlider) {
            if (componentClass.equals(Color.class)) {
                value = ((ColorSlider) component).getColor();
            }
        }
        return value;
    }

    @Override
    public boolean setComponentValue(Object value) {
        LOGGER.fine("setComponentValue: " + value + ", component: " + component);
        boolean success = false;
        if (component instanceof JTextField) {
            ((JTextField) component).setText(value.toString());
        } else if (component instanceof JComboBox) {
            ((JComboBox) component).setSelectedItem(value);
        } else if (component instanceof ColorSlider) {
            if (value instanceof Color) {
                ((ColorSlider) component).setColor((Color) value);
            }
        } else {
            LOGGER.severe("Could not set component value for component: " + component + " and value class: " + value.getClass().getSimpleName());
        }
        return success;
    }

    @Override
    public void handleMarkups(ArrayList<Markup> markups, MarkupManager manager) {
        // No dynamic markups handled
    }

    @Override
    public void disableMarkup(Markup markup) {
        // No dynamic markups handled
    }

    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        ArrayList<Class> compCreationClasses = new ArrayList<Class>();
        compCreationClasses.addAll(supportedCreationClasses);
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget temp = (MarkupComponentWidget) widgetClass.newInstance();
                ArrayList<Class> widgetCreationClasses = temp.getSupportedCreationClasses();
                for (Class widgetCreationClass : widgetCreationClasses) {
                    if (!compCreationClasses.contains(widgetCreationClass)) {
                        compCreationClasses.add(widgetCreationClass);
                    }
                }
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return compCreationClasses;
    }
}
