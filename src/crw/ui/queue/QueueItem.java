package crw.ui.queue;

import crw.ui.component.QueueContent;
import crw.ui.component.QueueThumbnail;
import sami.uilanguage.MarkupManager;
import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.fromui.CrwFromUiMessageGenerator;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import sami.engine.Engine;
import sami.event.ReflectedEventSpecification;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class QueueItem {

    private final static Logger LOGGER = Logger.getLogger(QueueItem.class.getName());
    final static int BUTTON_WIDTH = 100;
    final static int BUTTON_HEIGHT = 50;
    public final static int THUMB_SCALED_WIDTH = 200;
    public final static int THUMB_SCALED_HEIGHT = 60;
    final static JComponent BLANK_COMPONENT = new JPanel();
    final static BufferedImage BLANK_THUMBNAIL = new BufferedImage(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    protected ActionListener listener;
    protected ToUiMessage decisionMessage;
    protected final AtomicBoolean viewed = new AtomicBoolean(false);
    List<Object> optionList = new ArrayList<Object>();
    Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>> eventSpecToComponentTable = new Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>>();
    protected QueueThumbnail thumbnail = null;
    protected QueueContent content = null;
    protected MarkupManager markupManager = null;

    public QueueItem(ToUiMessage decisionMessage, ActionListener listener) {
        this.decisionMessage = decisionMessage;
        this.listener = listener;
    }

    public QueueItem(ToUiMessage decisionMessage, ActionListener listener, MarkupManager markupManager) {
        this.decisionMessage = decisionMessage;
        this.listener = listener;
        this.markupManager = markupManager;
    }

    public boolean getViewed() {
        return viewed.get();
    }

    public synchronized QueueContent getInteractionPanel() {
        if (content == null) {

            content = new QueueContent();
            markupManager.addComponent(content);

            content.setLayout(new GridBagLayout());
            // Setting height border messes up cumul height
            content.setBorder(new EmptyBorder(0, 10, 0, 10));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridy = 0;
            constraints.gridx = 0;
            constraints.weightx = 1.0;

            if (decisionMessage instanceof CreationMessage) {
                CreationMessage creationMessage = (CreationMessage) decisionMessage;

                int maxColWidth = BUTTON_WIDTH;
                int cumulComponentHeight = 0;
                for (ReflectedEventSpecification eventSpec : creationMessage.getEventSpecToFieldDescriptions().keySet()) {
                    Hashtable<Field, String> fieldDescriptions = creationMessage.getEventSpecToFieldDescriptions().get(eventSpec);
                    Hashtable<Field, MarkupComponent> componentTable = new Hashtable<Field, MarkupComponent>();
                    eventSpecToComponentTable.put(eventSpec, componentTable);
                    for (Field field : fieldDescriptions.keySet()) {
                        // Add in description of item to be created
                        String description = fieldDescriptions.get(field);
                        JLabel descriptionLabel = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): " + description);
                        content.add(descriptionLabel, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) descriptionLabel.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) descriptionLabel.getPreferredSize().getHeight();

                        // Add in component for creating the item
                        MarkupComponent markupComponent = null;
                        JComponent visualization = null;
                        markupComponent = CrwUiComponentGenerator.getInstance().getCreationComponent(field.getType(), creationMessage.getMarkups());
                        if (markupComponent == null) {
                            LOGGER.severe("Got null creation component for field: " + field);
                            visualization = new JLabel("");
                        } else {
                            if (eventSpec != null) {
                                Object definition = eventSpec.getFieldValues().get(field.getName());
                                if (definition != null) {
                                    CrwUiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
                                }
                            } else {
                                LOGGER.severe("Failed to retrieve eventSpec for field: " + field.getName());
                            }
                            componentTable.put(field, markupComponent);
                            visualization = markupComponent.getComponent();
                        }
                        content.add(visualization, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) visualization.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) visualization.getPreferredSize().getHeight();
                    }
                }
                // Add "Done" button
                JButton button = new JButton("Done");
                button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        creationDone();
                        listener.actionPerformed(ae);
                    }
                });
                content.add(button, constraints);
                constraints.gridy = constraints.gridy + 1;
                maxColWidth = Math.max(maxColWidth, BUTTON_WIDTH);
                cumulComponentHeight += BUTTON_HEIGHT;

                content.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));
                content.revalidate();
            } else if (decisionMessage instanceof SelectionMessage) {
                SelectionMessage selectionMessage = (SelectionMessage) decisionMessage;

                int row = 0;
                int maxColWidth = BUTTON_WIDTH;
                int cumulComponentHeight = 0;
                for (final Object option : selectionMessage.getOptionsList()) {
                    MarkupComponent markupComponent = CrwUiComponentGenerator.getInstance().getSelectionComponent(option.getClass(), option, selectionMessage.getMarkups());
                    JComponent visualization;
                    if (markupComponent != null) {
                        visualization = markupComponent.getComponent();
                    } else {
                        LOGGER.severe("No component available");
                        visualization = new JLabel("No component available");
                    }

                    JComponent button;
                    if (selectionMessage.getAllowMultiple()) {
                        button = new JCheckBox("Use " + row, false);
                        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
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
                        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                        ((JButton) button).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                singleSelectionDone(option);
                                listener.actionPerformed(ae);
                            }
                        });
                    }

                    content.add(visualization, constraints);
                    constraints.gridx = 1;
                    content.add(button, constraints);
                    constraints.gridx = 0;
                    constraints.gridy = constraints.gridy + 1;
                    maxColWidth = Math.max(maxColWidth, (int) visualization.getPreferredSize().getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max((int) visualization.getPreferredSize().getHeight(), BUTTON_HEIGHT);
                    row++;
                }
                if (selectionMessage.getAllowMultiple()) {
                    // Add "Accept" button
                    JPanel blank = new JPanel();
                    blank.setSize(10, 10);
                    JButton button = new JButton("Accept");
                    button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            multipleSelectionDone();
                            listener.actionPerformed(ae);
                        }
                    });
                    content.add(blank, constraints);
                    constraints.gridx = 1;
                    content.add(button, constraints);
                    constraints.gridx = 0;
                    constraints.gridy = constraints.gridy + 1;
                    maxColWidth = Math.max(maxColWidth, blank.getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max(blank.getHeight(), BUTTON_HEIGHT);
                    row++;
                }
                if (selectionMessage.getAllowRejection()) {
                    // Add "Reject" button
                    JPanel blank = new JPanel();
                    blank.setSize(10, 10);
                    JButton button = new JButton("Reject");
                    button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            singleSelectionDone(null);
                            listener.actionPerformed(ae);
                        }
                    });
                    content.add(blank, constraints);
                    constraints.gridx = 1;
                    content.add(button, constraints);
                    constraints.gridx = 0;
                    constraints.gridy = constraints.gridy + 1;
                    maxColWidth = Math.max(maxColWidth, blank.getWidth() + BUTTON_WIDTH);
                    cumulComponentHeight += Math.max(blank.getHeight(), BUTTON_HEIGHT);
                    row++;
                }

                content.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));
            }
        }
        return content;
    }

    public synchronized QueueThumbnail getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new QueueThumbnail(decisionMessage);
            markupManager.addComponent(thumbnail);
        }
        return thumbnail;
    }

    public void creationDone() {
        FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((CreationMessage) decisionMessage, eventSpecToComponentTable);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
    }

    public void multipleSelectionDone() {
        FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) decisionMessage, optionList);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
//        viddb.setViewed(this, viewed);
    }

    public void singleSelectionDone(Object option) {
        FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) decisionMessage, option);
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
