package crw.proxy.clickthrough;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sami.event.InputEvent;

/**
 *
 * @author nbb
 */
public class ManageSimProxyP extends JPanel {

    protected ClickthroughProxy clickthroughProxy;
    protected JScrollPane ieSP;
    protected JPanel ieP;
    protected JButton refreshIeB, generateIeB, lowBatteryB, hardwareFailureB;

    public ManageSimProxyP(final ClickthroughProxy clickthroughProxy) {
        super();

        this.clickthroughProxy = clickthroughProxy;

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        ieP = new JPanel();
        ieP.setLayout(new BoxLayout(ieP, BoxLayout.Y_AXIS));
        ieSP = new JScrollPane();
        ieSP.setViewportView(ieP);
        ieSP.setPreferredSize(new Dimension(0, 200));
        refreshInputEventList();

        refreshIeB = new JButton("Refresh IE");
        refreshIeB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshInputEventList();
            }
        });
        generateIeB = new JButton("Generate top IE");
        generateIeB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clickthroughProxy.generateTopInputEvent();
                refreshInputEventList();
            }
        });
        lowBatteryB = new JButton("Low Battery");
        lowBatteryB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        hardwareFailureB = new JButton("Autonomy failure");
        hardwareFailureB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        add(ieSP, constraints);
        constraints.gridy = constraints.gridy + 1;
        add(refreshIeB, constraints);
        constraints.gridy = constraints.gridy + 1;
        add(generateIeB, constraints);
        constraints.gridy = constraints.gridy + 1;
        add(lowBatteryB, constraints);
        constraints.gridy = constraints.gridy + 1;
        add(hardwareFailureB, constraints);
        constraints.gridy = constraints.gridy + 1;
    }

    public void refreshInputEventList() {
        ieP.removeAll();
        for (InputEvent ie : clickthroughProxy.getSequentialInputEvents()) {
            ieP.add(new JLabel(ie.toString()));
        }
        revalidate();
        repaint();
    }
}
