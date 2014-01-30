package crw.ui.queue;

import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.fromui.FromUiMessageGenerator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import sami.engine.Engine;
import sami.markup.Priority;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.GetParamsMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class DecisionQueueComponent {

    private final static Logger LOGGER = Logger.getLogger(DecisionQueueComponent.class.getName());
    final static int BUTTON_WIDTH = 100;
    final static int BUTTON_HEIGHT = 50;
    public final static int THUMB_SCALED_WIDTH = 200;
    public final static int THUMB_SCALED_HEIGHT = 60;
    final static JComponent BLANK_COMPONENT = new JPanel();
    final static BufferedImage BLANK_THUMBNAIL = new BufferedImage(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    protected ActionListener listener;
    protected ToUiMessage decisionMessage;
    protected Image thumbnail = null;
    protected final AtomicBoolean viewed = new AtomicBoolean(false);
    protected JComponent component;
    List<Object> optionList = new ArrayList<Object>();
    Hashtable<Field, JComponent> componentTable = new Hashtable<Field, JComponent>();

    public DecisionQueueComponent(ToUiMessage decisionMessage, ActionListener listener) {
        this.decisionMessage = decisionMessage;
        this.listener = listener;
    }

    public boolean getViewed() {
        return viewed.get();
    }

    public synchronized JComponent getInteractionPanel() {
        if (component == null) {
            component = new JPanel();

            if (decisionMessage instanceof CreationMessage) {
                CreationMessage creationMessage = (CreationMessage) decisionMessage;

                GroupLayout layout = new GroupLayout(component);
                component.setLayout(layout);
                GroupLayout.SequentialGroup rowSeqGroup = layout.createSequentialGroup();
                GroupLayout.ParallelGroup rowParGroup1 = layout.createParallelGroup();
                GroupLayout.SequentialGroup colSeqGroup = layout.createSequentialGroup();
                int numRows = 2 * creationMessage.getFieldDescriptions().size() + 1;
                GroupLayout.ParallelGroup[] colParGroupArr = new GroupLayout.ParallelGroup[numRows];
                int row = 0;
                int maxColWidth = BUTTON_WIDTH;
                int cumulComponentHeight = 0;
                if (decisionMessage instanceof GetParamsMessage) {
                }
                for (Field field : creationMessage.getFieldDescriptions().keySet()) {
                    // Add in description of item to be created
                    String description = creationMessage.getFieldDescriptions().get(field);
                    JLabel descriptionLabel = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): " + description);
                    rowParGroup1.addComponent(descriptionLabel);
                    colParGroupArr[row] = layout.createParallelGroup();
                    colParGroupArr[row].addComponent(descriptionLabel);
                    maxColWidth = Math.max(maxColWidth, (int) descriptionLabel.getMaximumSize().getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max((int) descriptionLabel.getMaximumSize().getHeight(), BUTTON_HEIGHT);
                    row++;

                    // Add in component for creating the item
                    JComponent objectVisualization = null;
                    objectVisualization = CrwUiComponentGenerator.getInstance().getCreationComponent(field.getType(), creationMessage.getMarkups());
                    if (objectVisualization == null) {
                        LOGGER.severe("Got null creation component for field: " + field);
                        objectVisualization = new JLabel("");
                    }
                    componentTable.put(field, objectVisualization);
                    rowParGroup1.addComponent(objectVisualization);
                    colParGroupArr[row] = layout.createParallelGroup();
                    colParGroupArr[row].addComponent(objectVisualization);
                    maxColWidth = Math.max(maxColWidth, (int) objectVisualization.getMaximumSize().getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max((int) objectVisualization.getMaximumSize().getHeight(), BUTTON_HEIGHT);
                    row++;
                }
                // Add "Done" button
                JButton button = new JButton("Done");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        creationDone();
                        listener.actionPerformed(ae);
                    }
                });
                rowParGroup1.addComponent(button, 100, 100, 100);
                colParGroupArr[row] = layout.createParallelGroup();
                colParGroupArr[row].addComponent(button, 50, 50, 50);
                maxColWidth = Math.max(maxColWidth, BUTTON_WIDTH);
                cumulComponentHeight += BUTTON_HEIGHT;
                row++;

                // Finish layout setup
                layout.setHorizontalGroup(rowSeqGroup
                        //                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1, Short.MAX_VALUE) // Spring to right-align
                        .addGroup(rowParGroup1));
                for (int i = 0; i < colParGroupArr.length; i++) {
                    GroupLayout.ParallelGroup parGroup = colParGroupArr[i];
                    colSeqGroup.addGroup(parGroup);
                    if (i < colParGroupArr.length - 1) {
                        colSeqGroup.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE);
                        cumulComponentHeight += 6;
                    }
                }
                layout.setVerticalGroup(colSeqGroup);

                component.setSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setMinimumSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setMaximumSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.revalidate();
            } else if (decisionMessage instanceof SelectionMessage) {
                SelectionMessage selectionMessage = (SelectionMessage) decisionMessage;

                GroupLayout layout = new GroupLayout(component);
                component.setLayout(layout);
                GroupLayout.SequentialGroup rowSeqGroup = layout.createSequentialGroup();
                GroupLayout.ParallelGroup rowParGroup1 = layout.createParallelGroup();
                GroupLayout.ParallelGroup rowParGroup2 = layout.createParallelGroup();
                GroupLayout.SequentialGroup colSeqGroup = layout.createSequentialGroup();
                int numRows = selectionMessage.getOptionsList().size() + (selectionMessage.getAllowRejection() ? 1 : 0) + (selectionMessage.getAllowMultiple() ? 1 : 0);
                GroupLayout.ParallelGroup[] colParGroupArr = new GroupLayout.ParallelGroup[numRows];
                int row = 0;
                int maxColWidth = BUTTON_WIDTH;
                int cumulComponentHeight = 0;
                for (final Object option : selectionMessage.getOptionsList()) {
                    JComponent objectVisualization = CrwUiComponentGenerator.getInstance().getSelectionComponent(option);

                    JComponent button;
                    if (selectionMessage.getAllowMultiple()) {
                        button = new JCheckBox("Use " + row, false);
                        ((JCheckBox) button).addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent ie) {
                                if (ie.getStateChange() == ItemEvent.SELECTED && !optionList.contains(option)) {
                                    optionList.add(option);
                                } else if (ie.getStateChange() == ItemEvent.DESELECTED && optionList.contains(option)) {
                                    optionList.remove(option);
                                }
                            }
                        });
                    } else {
                        button = new JButton("Accept " + row);
                        ((JButton) button).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                singleSelectionDone(option);
                                listener.actionPerformed(ae);
                            }
                        });
                    }

                    rowParGroup1.addComponent(objectVisualization);
                    rowParGroup2.addComponent(button, BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH);
                    colParGroupArr[row] = layout.createParallelGroup();
                    colParGroupArr[row].addComponent(objectVisualization);
                    colParGroupArr[row].addComponent(button, BUTTON_HEIGHT, BUTTON_HEIGHT, BUTTON_HEIGHT);
                    maxColWidth = Math.max(maxColWidth, (int) objectVisualization.getMaximumSize().getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max((int) objectVisualization.getMaximumSize().getHeight(), BUTTON_HEIGHT);
                    row++;
                }
                if (selectionMessage.getAllowMultiple()) {
                    // Add "Accept" button
                    JPanel blank = new JPanel();
                    blank.setSize(10, 10);
                    JButton button = new JButton("Accept");
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            multipleSelectionDone();
                            listener.actionPerformed(ae);
                        }
                    });
                    rowParGroup1.addComponent(blank);
                    rowParGroup2.addComponent(button, 100, 100, 100);
                    colParGroupArr[row] = layout.createParallelGroup();
                    colParGroupArr[row].addComponent(blank);
                    colParGroupArr[row].addComponent(button, 50, 50, 50);
                    maxColWidth = Math.max(maxColWidth, blank.getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max(blank.getHeight(), BUTTON_HEIGHT);
                    row++;
                }
                if (selectionMessage.getAllowRejection()) {
                    // Add "Reject" button
                    JPanel blank = new JPanel();
                    blank.setSize(10, 10);
                    JButton button = new JButton("Reject");
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            singleSelectionDone(null);
                            listener.actionPerformed(ae);
                        }
                    });
                    rowParGroup1.addComponent(blank);
                    rowParGroup2.addComponent(button, 100, 100, 100);
                    colParGroupArr[row] = layout.createParallelGroup();
                    colParGroupArr[row].addComponent(blank);
                    colParGroupArr[row].addComponent(button, 50, 50, 50);
                    maxColWidth = Math.max(maxColWidth, blank.getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max(blank.getHeight(), BUTTON_HEIGHT);
                    row++;
                }

                // Finish layout stuff
                layout.setHorizontalGroup(rowSeqGroup
                        .addGroup(rowParGroup2)
                        .addGroup(rowParGroup1));
                for (int i = 0; i < colParGroupArr.length; i++) {
                    GroupLayout.ParallelGroup parGroup = colParGroupArr[i];
                    colSeqGroup.addGroup(parGroup);
                    if (i < colParGroupArr.length - 1) {
                        colSeqGroup.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE);
                        cumulComponentHeight += 6;
                    }
                }
                layout.setVerticalGroup(colSeqGroup);

                component.setSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setMinimumSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setMaximumSize(new Dimension(maxColWidth, cumulComponentHeight));
                component.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));
            }
        }
        return component;
    }

    private BufferedImage addText(BufferedImage image, String line1, String line2) {
        int w = image.getWidth();
        int h = image.getHeight();
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        // Write line1
        g2d.setPaint(Color.red);
        g2d.setFont(new Font("Serif", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        int x = image.getWidth() - fm.stringWidth(line1) - 5;
        int y = fm.getHeight();
        g2d.drawString(line1, x, y);
        // Write line2
        g2d.setPaint(Color.black);
        g2d.setFont(new Font("Serif", Font.BOLD, 14));
        x = image.getWidth() - fm.stringWidth(line2) - 5;
        g2d.drawString(line2, 0, y * 2);
        g2d.dispose();
        return image;
    }

    public synchronized Image getThumbnail() {
        if (thumbnail == null) {
            BufferedImage original = new BufferedImage(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            addText(original, Priority.getPriority(decisionMessage.getPriority()).toString(), decisionMessage.getClass().getSimpleName());
            thumbnail = original.getScaledInstance(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, Image.SCALE_FAST);
        }
        return thumbnail;
    }

    public void creationDone() {
        FromUiMessage fromUiMessage = FromUiMessageGenerator.getInstance().getFromUiMessage((CreationMessage) decisionMessage, componentTable);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
    }

    public void multipleSelectionDone() {
        FromUiMessage fromUiMessage = FromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) decisionMessage, optionList);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
//        viddb.setViewed(this, viewed);
    }

    public void singleSelectionDone(Object option) {
        FromUiMessage fromUiMessage = FromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) decisionMessage, option);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
//        viddb.setViewed(this, viewed);
    }

    public static Image getFillerThumbnail() {
        return BLANK_THUMBNAIL;
    }

    public static JComponent getFillerComponent() {
        return BLANK_COMPONENT;
    }

    public static void main(String[] args) throws ClassNotFoundException {
//        try {
//            Field f = DecisionQueueComponent.class.getDeclaredField("component");
//            Hashtable<Field, String> h = new Hashtable<Field, String>();
//            h.put(f, "asdf");
//            echo(f, h);
//        } catch (NoSuchFieldException ex) {
//            Logger.getLogger(DecisionQueueComponent.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SecurityException ex) {
//            Logger.getLogger(DecisionQueueComponent.class.getName()).log(Level.SEVERE, null, ex);
//        }
        Hashtable h = new Hashtable<Field, JComponent>();
//        h.put(null, new JLabel("asdf"));
        Class c = Class.forName("crw.event.output.service.AllocationRequest");
        for (Field f : c.getFields()) {
            System.out.println(f.toString());
            h.put(f, new JLabel(""));
        }

    }

    public static void echo(Field f, Hashtable<Field, String> h) {
        System.out.println(h.containsKey(f));
    }
}
