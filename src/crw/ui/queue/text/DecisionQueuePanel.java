package crw.ui.queue.text;

import crw.ui.queue.QueueDatabase;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sami.markup.Priority;
import static sami.markup.Priority.Ranking.CRITICAL;
import static sami.markup.Priority.Ranking.LOW;
import sami.uilanguage.MarkupManager;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author owens
 * @author nbb
 */
public class DecisionQueuePanel extends JPanel implements QueuePanelInt, MouseWheelListener {

    private final static Logger LOGGER = Logger.getLogger(DecisionQueuePanel.class.getName());
    private final static int ITEMS_PER_FETCH = 10;
    final static int MISC_BORDER = 15;
    final static int BORDER_SIZE = 0;
    private final static Color COLOR_ONE = new Color(169, 218, 146);
    private final static Color COLOR_TWO = new Color(218, 235, 201);
    public final String name;
    public final static int INDEX_LIMIT_INCREMENT = 10;
    private final Object lock = new Object();
    private final QueuePanelInt thisQueuePanel = this;
    private int index = 0;
    private QueueItemText currentComponent = null;
    private ToUiMessage currentMessage = null;
    private int k;
    private int oldIndex = -1;
    private int oldIndexLimit = -1;
    private List<ToUiMessage> loadedMessages;
    private QueueFrame queueFrame;
    private QueueDatabase queueDatabase;
    private JScrollPane thumbnailSP;
    private JPanel thumbnailP;
    private Hashtable<ToUiMessage, QueueItemText> messageToItem = new Hashtable<ToUiMessage, QueueItemText>();
    // Is this queue the queue frame's active queue?
    private boolean isActive = false;

    /**
     * Default constructor for the demo.
     */
    public DecisionQueuePanel(QueueFrame queueFrame, QueueDatabase queueDatabase, String name) {
        this.queueFrame = queueFrame;
        this.queueDatabase = queueDatabase;
        this.name = name;
        loadedMessages = new ArrayList<ToUiMessage>();

        //Lets set the size of this panel itself
        setName(name);

        // Create header panel used to select the active queue panel 
        HeaderPanel headerPanel = new HeaderPanel(name);
        headerPanel.setMinimumSize(new Dimension(0, 25));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        headerPanel.setPreferredSize(new Dimension(400, 25));

        //  Create icon bar used to show and select queue item thumbnails
        thumbnailP = new JPanel();
        thumbnailP.setLayout(new BoxLayout(thumbnailP, BoxLayout.Y_AXIS));
        thumbnailP.setFocusable(true);
        // Create scroll pane to house thumbnails
        thumbnailSP = new JScrollPane();
        thumbnailSP.setMinimumSize(new Dimension(0, 100));
//        thumbnailSP.setPreferredSize(new Dimension(400, 100));
        thumbnailSP.setViewportView(thumbnailP);
        thumbnailSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Add content
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(headerPanel);
        add(thumbnailSP);
//        // Add content
//        setLayout(new GridBagLayout());
//        GridBagConstraints constraints = new GridBagConstraints();
//        constraints.fill = GridBagConstraints.HORIZONTAL;
//        constraints.gridx = 0;
//        constraints.gridy = 0;
//        constraints.weightx = 1.0;
//        add(headerPanel, constraints);
//        constraints.gridy = constraints.gridy + 1;
//        add(thumbnailSP, constraints);
//        constraints.gridy = constraints.gridy + 1;

        // Periodically check if we have enough images, and update the list, the screen, etc.
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    updateIndex(0);
                    try {
                        EventQueue.invokeAndWait(new Runnable() {
                            public void run() {
                                if (createImageIcon()) {
                                    DecisionQueuePanel.this.repaint();
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        e.printStackTrace();
                    }
//                    System.out.println("sleep");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }.start();
    }

    public boolean createImageIcon() {
        updateIndex(0);
        int indexLimit = 0;
        ArrayList<ToUiMessage> displayedList = new ArrayList<ToUiMessage>();
        synchronized (loadedMessages) {
            indexLimit = index + INDEX_LIMIT_INCREMENT;
            if (indexLimit >= loadedMessages.size()) {
                indexLimit = loadedMessages.size() - 1;
            }
            if (oldIndex == index && oldIndexLimit == indexLimit) {
                // Indicies didn't change - nothing to do
                return false;
            }
            oldIndex = index;
            oldIndexLimit = indexLimit;
            for (k = index; k <= indexLimit; k++) {
                displayedList.add(loadedMessages.get(k));
            }
        }
        // If this is the active queue, update the visualized component
        if (isActive) {
            LOGGER.log(Level.FINEST, "set current component " + index);
            setCurrentComponent(index);
        }
        LOGGER.log(Level.FINEST, "createImageIcon: index=" + index + " interactionList.size()=" + displayedList.size() + " loadedInteractions.size() = " + loadedMessages.size());
        thumbnailP.removeAll();

        for (k = 0; k < displayedList.size(); k++) {
            LOGGER.log(Level.FINEST, "Created icons :" + k);

            QueueItemText component = messageToItem.get(displayedList.get(k));
            JButton button = new JButton();
            button.add(component.getThumbnail());
            if (component.getViewed()) {
                button.setBackground(Color.BLUE);
            }
            //Add action listener to button
            button.addActionListener(thumbnailListener);
            button.setActionCommand(Integer.toString(index + k));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            thumbnailP.add(button);
        }

        thumbnailP.revalidate();
        return true;
    }

    private void setCurrentComponent(int val) {
        QueueItemText component = null;
        synchronized (loadedMessages) {
            if (val >= 0 && val < loadedMessages.size()) {
                currentMessage = loadedMessages.get(val);
                component = messageToItem.get(currentMessage);
            }
        }
        if (component != null) {
            synchronized (lock) {
                currentComponent = component;
            }

            // Update queue frame's active queue panel's content
            queueFrame.setActiveQueueContent(currentComponent.getInteractionPanel());
            queueFrame.revalidate();

        } else {
            synchronized (lock) {
                currentComponent = component;
                // Clear queue frame's active queue panel
                queueFrame.setActiveQueueContent(null);
            }
        }
    }

    /**
     * Move forward or backward in the queue
     *
     * @param val the number of items to move by
     */
    private void updateIndex(int val) {
//        System.out.println("### updateIndex " + val);
        synchronized (loadedMessages) {
            index += val;
            if (index < 0) {
                // Tried to move beyond "left" end of the queue
                index = 0;
            } else if (index >= loadedMessages.size()) {
                // Tried to move beyond "right" end of the queue
                if (loadedMessages.size() == 0) {
                    index = 0;
                } else {
                    index = loadedMessages.size() - 1;
                }
            }
            // If we don't have at least ITEMS_PER_FETCH left after the current index, get some more items
//            LOGGER.info("UpdateIndex: Image index=" + index);

            // Create queue items for each fetched message
            // Keep track of relative imporance of items as we may change the currently viewed item
            int criticalIndex = -1;
            int highestIndex = -1;
            int highestPriority = -1;
            int imagesLeft = loadedMessages.size() - index;
            if (imagesLeft < ITEMS_PER_FETCH) {
                int fetched = queueDatabase.getHighPriorityInteractions(loadedMessages, ITEMS_PER_FETCH);
//                LOGGER.info("Fetched " + fetched + " images from VidDB, wanted " + (ITEMS_PER_FETCH) + ", list size = " + loadedInteractions.size() + ", index=" + index);
                // Create the components for the newly loaded messages
                for (int i = 1; i <= fetched; i++) {
                    ToUiMessage message = loadedMessages.get(loadedMessages.size() - i);
                    if (loadedMessages.get(loadedMessages.size() - i).getPriority() == Priority.priorityToInt.get(CRITICAL)) {
                        // This message is of critical importance
                        criticalIndex = loadedMessages.size() - i;
                        highestIndex = loadedMessages.size() - i;
                        highestPriority = Priority.priorityToInt.get(CRITICAL);
                    } else if (loadedMessages.get(loadedMessages.size() - i).getPriority() > highestPriority) {
                        highestIndex = loadedMessages.size() - i;
                        highestPriority = loadedMessages.get(loadedMessages.size() - i).getPriority();
                    }
                    MarkupManager manager = queueDatabase.getParent(message);
                    messageToItem.put(message, new QueueItemText(message, componentListener, manager));
                }
            }
            if (currentMessage != null && currentMessage.getPriority() != Priority.priorityToInt.get(CRITICAL) && criticalIndex != -1) {
                // The currently viewed message is not critical and we have received a critical message
                //  Switch the viewed message to the critical one as if we had clicked on its thumbnail
                index = criticalIndex;
                setCurrentComponent(criticalIndex);
                createImageIcon();
                repaint();
            } else if (currentMessage != null && currentMessage.getPriority() == Priority.priorityToInt.get(LOW) && highestIndex != -1) {
                // The currently viewed message is of low importance and we have received message of greater than low importance
                //  Switch the viewed message to the non-low one as if we had clicked on its thumbnail
                index = highestIndex;
                setCurrentComponent(highestIndex);
                createImageIcon();
                repaint();
            }
        }
    }

    ActionListener thumbnailListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Make the clicked thumbnail the enlarged component and refresh the queue
            String tag = e.getActionCommand();
            index = Integer.parseInt(tag);
            setCurrentComponent(Integer.parseInt(tag));
            createImageIcon();
            DecisionQueuePanel.this.repaint();
        }
    };
    ActionListener componentListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Remove the current component from the loaded lists
            loadedMessages.remove(currentMessage);
            messageToItem.remove(currentMessage);
            thumbnailP.repaint();

            // Display highest ranked item
            index = 0;
            createImageIcon();
        }
    };

    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
        // System.out.println("Mouse: " + mwe.getScrollAmount() + " " + mwe.getScrollType() + " " + mwe.getWheelRotation());
        createImageIcon();
        DecisionQueuePanel.this.repaint();
    }

    private class HeaderPanel extends JPanel implements MouseListener {

        String text_;
        Font font;
        BufferedImage open, closed;
        final int OFFSET = 30, PAD = 5;

        public HeaderPanel(String text) {
            addMouseListener(this);
            text_ = text;
            font = new Font("sans-serif", Font.PLAIN, 12);
            setBackground(COLOR_ONE);
            int w = getWidth();
            int h = getHeight();
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        HeaderPanel.this.repaint();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }.start();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setFont(font);
            FontRenderContext frc = g2.getFontRenderContext();
            LineMetrics lm = font.getLineMetrics(text_, frc);
            float height = lm.getAscent() + lm.getDescent();
            float x = OFFSET;
            float y = (h + height) / 2 - lm.getDescent();
            g2.drawString(text_ + " " + queueDatabase.getAllInteractionsCount(), x, y);
        }

        public void mouseClicked(MouseEvent e) {
            // Make this the active queue panel
            if (!isActive) {
                isActive = true;
                thumbnailP.requestFocus();

                queueFrame.moveToTop(thisQueuePanel);
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    public QueueItemText getCurrentContent() {
        return currentComponent;
    }

    @Override
    public boolean removeMessageId(UUID messageId) {
        ArrayList<ToUiMessage> messagesToRemove = new ArrayList<ToUiMessage>();
        synchronized (loadedMessages) {
            for (ToUiMessage message : loadedMessages) {
                if (message.getMessageId().equals(messageId)) {
                    messagesToRemove.add(message);
                }
            }
            for (ToUiMessage message : messagesToRemove) {
                loadedMessages.remove(message);
                messageToItem.remove(message);
            }
        }
        if (!messagesToRemove.isEmpty()) {
            // Redraw thumbnail list if we removed anything
            createImageIcon();
        }

        return !messagesToRemove.isEmpty();
    }

    @Override
    public int removeMissionId(UUID missionId) {
        ArrayList<ToUiMessage> messagesToRemove = new ArrayList<ToUiMessage>();
        synchronized (loadedMessages) {
            for (ToUiMessage message : loadedMessages) {
                if (message.getMissionId().equals(missionId)) {
                    messagesToRemove.add(message);
                }
            }
            for (ToUiMessage message : messagesToRemove) {
                loadedMessages.remove(message);
            }
        }
        if (!messagesToRemove.isEmpty()) {
            // Redraw thumbnail list if we removed anything
            createImageIcon();
        }

        return messagesToRemove.size();
    }

    @Override
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;

        if (isActive) {
            // Reload current component for this queue into the QueueFrame content panel
            setCurrentComponent(index);
        }
    }

    public String toString() {
        return "DecisionQueuePanel [" + name + ", " + queueDatabase.getAllInteractionsCount() + "]";
    }
}
