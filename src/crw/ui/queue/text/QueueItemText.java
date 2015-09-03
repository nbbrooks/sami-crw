package crw.ui.queue.text;

import crw.ui.queue.QueueContent;
import sami.uilanguage.MarkupManager;
import crw.ui.CrwUiComponentGenerator;
import crw.uilanguage.message.fromui.CrwFromUiMessageGenerator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import sami.engine.Engine;
import sami.event.ReflectedEventSpecification;
import sami.logging.Recorder;
import sami.markup.Description;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;
import sami.variable.VariableName;

/**
 *
 * @author nbb
 */
public class QueueItemText {

    private final static Logger LOGGER = Logger.getLogger(QueueItemText.class.getName());
    final static int BUTTON_WIDTH = 100;
    final static int BUTTON_HEIGHT = 50;
    final static JComponent BLANK_COMPONENT = new JPanel();
    final static JComponent BLANK_THUMBNAIL = new JLabel("BLANK");
    protected ActionListener listener;
    protected ToUiMessage decisionMessage;
    protected final AtomicBoolean viewed = new AtomicBoolean(false);
    List<Object> optionList = new ArrayList<Object>();
    Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>> eventSpecToComponentTable = new Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>>();
    Hashtable<Field, MarkupComponent> fieldToComponentTable = new Hashtable<Field, MarkupComponent>();
    Hashtable<String, MarkupComponent> variableNameToComponentTable = new Hashtable<String, MarkupComponent>();
    protected QueueThumbnail thumbnail = null;
    protected QueueContent content = null;
    protected MarkupManager markupManager = null;
    final JButton doneButton = new JButton();

    public QueueItemText(ToUiMessage decisionMessage, ActionListener listener) {
        this.decisionMessage = decisionMessage;
        this.listener = listener;
    }

    public QueueItemText(ToUiMessage decisionMessage, ActionListener listener, MarkupManager markupManager) {
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
            int maxColWidth = BUTTON_WIDTH;
            int cumulComponentHeight = 0;

            // If the event had a Description markup, put its text at the top of this panel
            for (Markup markup : decisionMessage.getMarkups()) {
                if (markup instanceof Description) {
                    JLabel descriptionMarkupLabel = new JLabel(((Description) markup).textOption.text);
                    content.add(descriptionMarkupLabel, constraints);
                    constraints.gridy = constraints.gridy + 1;
                    maxColWidth = Math.max(maxColWidth, (int) descriptionMarkupLabel.getPreferredSize().getWidth());
                    cumulComponentHeight += (int) descriptionMarkupLabel.getPreferredSize().getHeight();
                }
            }

            if (decisionMessage instanceof CreationMessage) {
                CreationMessage creationMessage = (CreationMessage) decisionMessage;
                // There are 3 different constructors for CreationMessage, each of which define only 1 of the following:
                //  Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions
                //      Used by MissingParamsRequest/GetParamsMessage to get missing field definitions for several event specs
                //  Hashtable<Field, String> fieldToDescriptions
                //      Used by generic (non-extended) CreationMessages
                //  Hashtable<VariableName, String> variableNameToDescription
                //      Used by RedefineVariablesRequest/VariablesDefinedMessage to redefine values for variables (as opposed to fields)

                if (creationMessage.getEventSpecToFieldDescriptions() != null) {
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
                            markupComponent = CrwUiComponentGenerator.getInstance().getCreationComponent(field.getType(), field, creationMessage.getMarkups(), null, Engine.getInstance().getPlanManager(creationMessage.getMissionId()));
                            if (markupComponent == null) {
                                LOGGER.severe("Got null creation component for field: " + field);
                                visualization = new JLabel("");
                            } else {
                                if (eventSpec != null) {
                                    if (eventSpec.getFieldValues().containsKey(field.getName())) {
                                        Object definition = eventSpec.getFieldValues().get(field.getName());
                                        if (definition != null) {
                                            CrwUiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
                                        } else {
                                            LOGGER.warning("NULL definition for field " + field.getName() + " in message " + decisionMessage.toString());
                                        }
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
                }
                if (creationMessage.getFieldToDescriptions() != null) {
                    for (Field field : creationMessage.getFieldToDescriptions().keySet()) {
                        // Add in description of item to be created
                        String description = creationMessage.getFieldToDescriptions().get(field);
                        JLabel descriptionLabel = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): " + description);
                        content.add(descriptionLabel, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) descriptionLabel.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) descriptionLabel.getPreferredSize().getHeight();

                        // Add in component for creating the item
                        MarkupComponent markupComponent = null;
                        JComponent visualization = null;
                        markupComponent = CrwUiComponentGenerator.getInstance().getCreationComponent(field.getType(), field, creationMessage.getMarkups(), null, Engine.getInstance().getPlanManager(creationMessage.getMissionId()));
                        if (markupComponent == null) {
                            LOGGER.severe("Got null creation component for field: " + field);
                            visualization = new JLabel("");
                        } else {
                            fieldToComponentTable.put(field, markupComponent);
                            visualization = markupComponent.getComponent();
                        }
                        content.add(visualization, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) visualization.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) visualization.getPreferredSize().getHeight();
                    }
                }
                if (creationMessage.getVariableNameToDescription() != null) {
                    for (VariableName key : creationMessage.getVariableNameToDescription().keySet()) {
                        // Add in description of item to be created
                        String variableName = key.variableName;
                        String description = creationMessage.getVariableNameToDescription().get(key);
                        Object currentDefinition = Engine.getInstance().getVariableValue(variableName, Engine.getInstance().getPlanManager(creationMessage.getMissionId()));

                        Class variableClass = currentDefinition.getClass();
                        JLabel descriptionLabel = new JLabel(variableName + " (" + variableClass.getSimpleName() + "): " + description);
                        content.add(descriptionLabel, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) descriptionLabel.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) descriptionLabel.getPreferredSize().getHeight();

                        // Add in component for creating the item
                        MarkupComponent markupComponent = null;
                        JComponent visualization = null;

                        markupComponent = CrwUiComponentGenerator.getInstance().getCreationComponent(variableClass, null, new ArrayList<Markup>(), null, Engine.getInstance().getPlanManager(creationMessage.getMissionId()));
                        if (markupComponent == null) {
                            LOGGER.severe("Got null creation component for variable: " + variableName + " (" + variableClass.getSimpleName() + ")");
                            visualization = new JLabel("");
                        } else {
                            CrwUiComponentGenerator.getInstance().setComponentValue(markupComponent, currentDefinition);
                            variableNameToComponentTable.put(variableName, markupComponent);
                            visualization = markupComponent.getComponent();
                        }
                        content.add(visualization, constraints);
                        constraints.gridy = constraints.gridy + 1;
                        maxColWidth = Math.max(maxColWidth, (int) visualization.getPreferredSize().getWidth());
                        cumulComponentHeight += (int) visualization.getPreferredSize().getHeight();
                    }
                }

                // Add "Done" button
                doneButton.setText("Done");
                doneButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                doneButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (checkIfAllDefined()) {
                            creationDone();
                            listener.actionPerformed(ae);
                        } else {
                            doneButton.setText("Missing Definition");
                            doneButton.setForeground(Color.RED);
                            doneButton.setBorder(BorderFactory.createLineBorder(Color.RED));
                        }
                    }
                });
                content.add(doneButton, constraints);
                constraints.gridy = constraints.gridy + 1;
                maxColWidth = Math.max(maxColWidth, BUTTON_WIDTH);
                cumulComponentHeight += BUTTON_HEIGHT;

                content.setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));
                content.revalidate();
            } else if (decisionMessage instanceof SelectionMessage) {
                SelectionMessage selectionMessage = (SelectionMessage) decisionMessage;

                int row = 0;
                for (final Object option : selectionMessage.getOptionsList()) {
                    MarkupComponent markupComponent = CrwUiComponentGenerator.getInstance().getSelectionComponent(option.getClass(), option, selectionMessage.getMarkups(), null, Engine.getInstance().getPlanManager(selectionMessage.getMissionId()));
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

    /**
     * Checks for any creation components which have not been used to define a
     * value
     *
     * @return True if all creation components have a definition
     */
    public boolean checkIfAllDefined() {
        for (ReflectedEventSpecification eventSpec : eventSpecToComponentTable.keySet()) {
            Hashtable<Field, MarkupComponent> componentTable = eventSpecToComponentTable.get(eventSpec);
            for (Field field : componentTable.keySet()) {
                Object value = CrwUiComponentGenerator.getInstance().getComponentValue(componentTable.get(field), field.getType());
                if (value == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public void creationDone() {
        if (Recorder.ENABLED) {
            // Take a screenshot
            Recorder.getInstance().recordScreenshot();
        }
        if (!eventSpecToComponentTable.isEmpty()) {
            FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((CreationMessage) decisionMessage, eventSpecToComponentTable);
            if (Engine.getInstance().getUiServer() != null) {
                Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
            } else {
                LOGGER.warning("NULL UiServer!");
            }
            viewed.set(true);
        }
        if (!fieldToComponentTable.isEmpty()) {
            FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((CreationMessage) decisionMessage, fieldToComponentTable, 0);
            if (Engine.getInstance().getUiServer() != null) {
                Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
            } else {
                LOGGER.warning("NULL UiServer!");
            }
            viewed.set(true);
        }
        if (!variableNameToComponentTable.isEmpty()) {
            FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((CreationMessage) decisionMessage, variableNameToComponentTable, 0, 0);
            if (Engine.getInstance().getUiServer() != null) {
                Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
            } else {
                LOGGER.warning("NULL UiServer!");
            }
            viewed.set(true);
        }
    }

    public void multipleSelectionDone() {
        if (Recorder.ENABLED) {
            // Take a screenshot
            Recorder.getInstance().recordScreenshot();
        }
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
        if (Recorder.ENABLED) {
            // Take a screenshot
            Recorder.getInstance().recordScreenshot();
        }
        FromUiMessage fromUiMessage = CrwFromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) decisionMessage, option);
        if (Engine.getInstance().getUiServer() != null) {
            Engine.getInstance().getUiServer().UIMessage(fromUiMessage);
        } else {
            LOGGER.warning("NULL UiServer!");
        }
        viewed.set(true);
//        viddb.setViewed(this, viewed);
    }
}
