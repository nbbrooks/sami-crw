package crw.ui.queue;

import crw.ui.component.QueueFrame;
import static crw.ui.queue.QueueItem.BLANK_THUMBNAIL;
import static crw.ui.queue.QueueItem.THUMB_SCALED_HEIGHT;
import static crw.ui.queue.QueueItem.THUMB_SCALED_WIDTH;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import sami.uilanguage.MarkupManager;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author owens
 * @author nbb
 */
public class DecisionQueuePanel extends JPanel implements QueuePanelInt, MouseWheelListener {

    private final static Logger LOGGER = Logger.getLogger(DecisionQueuePanel.class.getName());
    final static int MISC_BORDER = 15;
    final static int BORDER_SIZE = 0;
    private final static Color COLOR_ONE = new Color(169, 218, 146);
    private final static Color COLOR_TWO = new Color(218, 235, 201);
    public final static String NAME = "main_panel";
    public final static int NUM_THUMBNAILS = 3;
    private final Object lock = new Object();
    private final QueuePanelInt thisQueuePanel = this;
    private int index = 0;
    private QueueItem currentComponent = null;
    private ToUiMessage currentMessage = null;
    private int k;
    private final static int ITEMS_PER_FETCH = 3;
    private List<ToUiMessage> loadedMessages;
    // Use a ScrollPane instead of JScrollPane to prevent GLCanvas components from drawing outside of the scroll pane area
    private JPanel iconBar = new JPanel();
    private QueueFrame queueFrame;
    private QueueDatabase queueDatabase;
    private Hashtable<ToUiMessage, QueueItem> messageToItem = new Hashtable<ToUiMessage, QueueItem>();
    // Is this queue the queue frame's active queue?
    private boolean isActive = false;

    /**
     * Default constructor for the demo.
     */
    public DecisionQueuePanel(QueueFrame queueFrame, QueueDatabase queueDatabase) {
        this.queueFrame = queueFrame;
        this.queueDatabase = queueDatabase;
        loadedMessages = new ArrayList<ToUiMessage>();

        //Lets set the size of this panel itself
        setName(NAME);

        // Create header panel used to select the active queue panel 
        HeaderPanel headerPanel = new HeaderPanel("Main Queue");

        // Create icon bar used to show and select queue item thumbnails
        iconBar.setFocusable(true);
        iconBar.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, (COLOR_ONE)));
        iconBar.setBackground(COLOR_TWO);
        iconBar.addMouseWheelListener(this);
        iconBar.setLayout(new BoxLayout(iconBar, BoxLayout.X_AXIS));
        for (int x = 0; x < NUM_THUMBNAILS; x++) {
            JButton filler = new JButton(new ImageIcon(QueueItem.getFillerThumbnail()));
            iconBar.add(filler);
        }
        LOGGER.log(Level.FINEST, "INIT Count Now:" + iconBar.getComponentCount());

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;

        add(headerPanel, constraints);
        constraints.gridy = constraints.gridy + 1;
        add(iconBar, constraints);
        constraints.gridy = constraints.gridy + 1;

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
    private int oldIndex = -1;
    private int oldIndexLimit = -1;
    private DecimalFormat fmt = new DecimalFormat("0.000");

    public boolean createImageIcon() {
        updateIndex(0);
        int indexLimit = 0;
        ArrayList<ToUiMessage> displayedList = new ArrayList<ToUiMessage>();
        synchronized (loadedMessages) {
            indexLimit = index + 3;
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
        LOGGER.log(Level.FINEST, "set current component " + index);
        setCurrentComponent(index);
        LOGGER.log(Level.FINEST, "createImageIcon: index=" + index + " interactionList.size()=" + displayedList.size() + " loadedInteractions.size() = " + loadedMessages.size());
        iconBar.removeAll();

        for (k = 0; k < displayedList.size(); k++) {
            LOGGER.log(Level.FINEST, "Created icons :" + k);

            QueueItem component = messageToItem.get(displayedList.get(k));
            JButton button = new JButton();
            button.add(component.getThumbnail());
            if (component.getViewed()) {
                button.setBackground(Color.BLUE);
            }
            //Add action listener to button
            button.addActionListener(thumbnailListener);
            button.setActionCommand(Integer.toString(index + k));
            iconBar.add(button);
        }

        LOGGER.log(Level.FINEST, "createImageIcon: components in iconbar before adding fillers =  " + iconBar.getComponentCount());
        for (int y = iconBar.getComponentCount(); y < NUM_THUMBNAILS; y++) {
            LOGGER.log(Level.FINEST, "Creating filler # " + y);

            // Need to add JPanel to filler buttons or Layout sizes actual item thumbnail buttons and filler buttons differently
            JButton filler = new JButton();
            JPanel panel = new JPanel();
            panel.add(new JLabel(new ImageIcon(BLANK_THUMBNAIL.getScaledInstance(THUMB_SCALED_WIDTH, THUMB_SCALED_HEIGHT, Image.SCALE_FAST))));
            filler.add(panel);
            iconBar.add(filler);
        }

        iconBar.revalidate();
        return true;
    }

    private void setCurrentComponent(int val) {
        QueueItem component = null;
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
            int imagesLeft = loadedMessages.size() - index;
            if (imagesLeft < ITEMS_PER_FETCH) {
                int fetched = queueDatabase.getHighPriorityInteractions(loadedMessages, ITEMS_PER_FETCH);
//                LOGGER.info("Fetched " + fetched + " images from VidDB, wanted " + (ITEMS_PER_FETCH) + ", list size = " + loadedInteractions.size() + ", index=" + index);
                // Create the components for the newly loaded messages
                for (int i = 1; i <= fetched; i++) {
                    ToUiMessage message = loadedMessages.get(loadedMessages.size() - i);
                    MarkupManager manager = queueDatabase.getParent(message);
                    messageToItem.put(message, new QueueItem(message, componentListener, manager));

                }
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
                iconBar.requestFocus();
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

    @Override
    public QueueItem getCurrentContent() {
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
    }
}
