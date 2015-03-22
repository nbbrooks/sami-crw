package crw.ui.queue.text;

import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import sami.uilanguage.UiPanel;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class QueueThumbnail extends UiPanel {

    protected ToUiMessage message;

    public QueueThumbnail(ToUiMessage message) {
        this.message = message;

        setLayout(new FlowLayout(FlowLayout.LEFT));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel(message.toString());
        add(label);
    }
}
