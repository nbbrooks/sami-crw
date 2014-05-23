package crw.ui.component;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import crw.proxy.BoatProxy;
import crw.ui.ColorSlider;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
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
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.MarkupComponentHelper;
import sami.uilanguage.MarkupManager;

/**
 *
 * @author nbb
 */
public class TextPanel implements MarkupComponent {

    // MarkupComponent variables
    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
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
        // Markups
        // Instructional text
    }

    @Override
    public int getCreationComponentScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getCreationComponentScore(supportedCreationClasses, supportedMarkups, widgetClasses, type, markups);
    }

    @Override
    public int getSelectionComponentScore(Type type, ArrayList<Markup> markups) {
        return MarkupComponentHelper.getSelectionComponentScore(supportedSelectionClasses, supportedMarkups, widgetClasses, type, markups);
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        return MarkupComponentHelper.getMarkupComponentScore(supportedMarkups, widgetClasses, markups);
    }

    @Override
    public MarkupComponent useCreationComponent(Type type, ArrayList<Markup> markups) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                component = handleCreationHashtable(pt);
            }
            return this;
        } else if (type instanceof Class) {
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
            } else if (objectClass.equals(Boolean.class)
                    || objectClass.equals(boolean.class)) {
                component = new JComboBox(new Object[]{true, false});
            } else if (objectClass.isEnum()) {
                component = new JComboBox(objectClass.getEnumConstants());
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
        if (object instanceof Hashtable) {
            Hashtable hashtable = (Hashtable) object;
            if (hashtable.size() > 0) {
                for (Object key : hashtable.keySet()) {
                    if (key != null && hashtable.get(key) != null) {
                        component = handleSelectionHashtable(hashtable, key, object);
                        break;
                    }
                }
            } else {
                component = new JLabel("Empty");
            }
            return this;
        } else if (object instanceof ResourceAllocation) {
            Map<ITask, AbstractAsset> allocation = ((ResourceAllocation) object).getAllocation();
            if (allocation.isEmpty()) {
                return null;
            }
            Object[][] table = new Object[allocation.size()][3];
            Object[] header = new Object[]{"", "", ""};
            int row = 0;
            for (ITask task : allocation.keySet()) {
                AbstractAsset asset = allocation.get(task);
                table[row][0] = asset.getName() + " (" + asset.getClass().getSimpleName() + ")";
                table[row][1] = " \u21e8 ";
                table[row][2] = task.getName() + " (" + task.getClass().getSimpleName() + ")";
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
    public Object getComponentValue(Field field) {
        Object value = null;
        if (component instanceof JTextField) {
            String text = ((JTextField) component).getText();

            if (text.length() > 0) {
                try {
                    if (field.getType().equals(String.class)) {
                        value = text;
                    } else if (field.getType().equals(Double.class)) {
                        value = new Double(text);
                    } else if (field.getType().equals(Float.class)) {
                        value = new Float(text);
                    } else if (field.getType().equals(Integer.class)) {
                        value = new Integer(text);
                    } else if (field.getType().equals(Long.class)) {
                        value = new Long(text);
                    } else if (field.getType().equals(Boolean.class)) {
                        value = Boolean.valueOf(text);
                    } else if (field.getType().equals(double.class)) {
                        value = Double.parseDouble(text);
                    } else if (field.getType().equals(float.class)) {
                        value = Float.parseFloat(text);
                    } else if (field.getType().equals(int.class)) {
                        value = Integer.parseInt(text);
                    } else if (field.getType().equals(long.class)) {
                        value = Long.parseLong(text);
                    } else if (field.getType().equals(boolean.class)) {
                        value = Boolean.parseBoolean(text);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Exception encountered when trying to get value for field type: " + field.getType() + " from text: " + text + ", setting value to null with exception: " + e);
                    value = null;
                }
            }
        } else if (component instanceof JComboBox) {
            value = ((JComboBox) component).getSelectedItem();

        } else if (component instanceof ColorSlider) {
            if (field.getType().equals(Color.class)) {
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
}
