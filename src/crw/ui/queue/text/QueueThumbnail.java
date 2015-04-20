package crw.ui.queue.text;

import crw.CrwHelper;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JLabel;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.markup.Description;
import sami.markup.Markup;
import sami.markup.Priority;
import sami.markup.Priority.Ranking;
import sami.uilanguage.UiPanel;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class QueueThumbnail extends UiPanel {

    protected ToUiMessage message;

    public static final String CRITICAL_COLOR = "rgb(186,0,0)"; // red    
    public static final String HIGH_COLOR = "rgb(255,186,18)"; // orange
    public static final String MEDIUM_COLOR = "rgb(255,220,0)";  // yellow
    public static final String LOW_COLOR = "rgb(75,204,0)"; // green

    public QueueThumbnail(ToUiMessage message) {
        this.message = message;

        // Left align thumbnail text
        setLayout(new FlowLayout(FlowLayout.LEFT));
        // Not sure if this is needed
        setAlignmentX(Component.LEFT_ALIGNMENT);
        // Colored rectangle corresponding to priority level
        String priority = "<font style='background-color:" + priorityToHtmlColor(message.getPriority()) + "; color:" + priorityToHtmlColor(message.getPriority()) + ";'>&nbsp;&nbsp;&nbsp;</font>";
        PlanManager pm = Engine.getInstance().getPlanManager(message.getMissionId());
        // Name of plan which generated this message
        String planName = "";
        if (pm != null) {
            Color[] doubleColor = Engine.getInstance().getPlanManagerColor(pm);
            planName = CrwHelper.padLeftHtml(pm.getPlanName(), 20);
            planName = "<font style='background-color:" + CrwHelper.colorToHtmlColor(doubleColor[0]) + "; color:" + CrwHelper.colorToHtmlColor(doubleColor[1]) + ";'>" + planName + "</font>";
        }
        // Class of this UI message
        String messageClass = CrwHelper.padLeftHtml(message.getClass().getSimpleName(), 20);
        // Class of OE which generated this message
        // @todo incorporate markups (Description, RelevantProxy, RelevantTask, etc) into description
        String description = "";
        for (Markup markup : message.getMarkups()) {
            if (markup instanceof Description) {
                description += CrwHelper.padLeftHtml(((Description) markup).textOption.text, 20);
            }
        }
        if (pm != null && message.getRelevantOutputEventId() != null) {
            if (!description.isEmpty()) {
                description += "&#x007C ";
            }
            description += CrwHelper.padLeftHtml(pm.getOutputEvent(message.getRelevantOutputEventId()).getClass().getSimpleName(), 20);
        }
        JLabel label = new JLabel();
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        // Concatenate sections with vertical bar dividers
        String text = "<html>" + priority + " " + planName + "&#x007C " + messageClass + "&#x007C " + description + "</html>";
        label.setText(text);
        add(label);
    }

    private String priorityToHtmlColor(int priority) {
        Ranking rank = Priority.getPriority(priority);
        switch (rank) {
            case CRITICAL:
                return CRITICAL_COLOR;
            case HIGH:
                return HIGH_COLOR;
            case MEDIUM:
                return MEDIUM_COLOR;
            case LOW:
            default:
                return LOW_COLOR;
        }
    }
}
