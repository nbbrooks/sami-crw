package crw.proxy.clickthrough;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import sami.engine.Engine;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerListenerInt;

/**
 *
 * @author nbb
 */
public class ClickthroughProxyManagerF extends JFrame implements ProxyServerListenerInt {

    Hashtable<String, JPanel> panelLookup = new Hashtable<String, JPanel>();
    JPanel visibleP = new JPanel();
    JComboBox<String> panelCB = new JComboBox<String>();

    public ClickthroughProxyManagerF() {
        super("ClickthroughProxyManagerF");

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        visibleP.setLayout(new BoxLayout(visibleP, BoxLayout.Y_AXIS));

        panelCB.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() instanceof JComboBox) {
                    JComboBox cb = (JComboBox) e.getSource();
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (panelLookup.containsKey(cb.getSelectedItem().toString())) {
                            visibleP.removeAll();
                            visibleP.add(panelLookup.get(cb.getSelectedItem().toString()));
                            if (panelLookup.get(cb.getSelectedItem().toString()) instanceof ManageSimProxyP) {
                                ((ManageSimProxyP) panelLookup.get(cb.getSelectedItem().toString())).refreshInputEventList();
                            }
                            visibleP.revalidate();
                            revalidate();
                        }
                    }
                }
            }
        });

        CreateSimProxyP createP = new CreateSimProxyP();
        panelLookup.put("Create", createP);
        panelCB.addItem("Create");

        getContentPane().add(panelCB, constraints);
        constraints.gridy = constraints.gridy + 1;
        getContentPane().add(visibleP, constraints);
        constraints.gridy = constraints.gridy + 1;

        Engine.getInstance().getProxyServer().addListener(this);

        pack();
        setVisible(true);
    }

    @Override
    public void proxyAdded(ProxyInt p) {
        if (p instanceof ClickthroughProxy) {
            ManageSimProxyP manageP = new ManageSimProxyP((ClickthroughProxy) p);
            panelLookup.put(p.getProxyName(), manageP);
            panelCB.addItem(p.getProxyName());
        }
    }

    @Override
    public void proxyRemoved(ProxyInt p) {
        if (panelCB.getSelectedItem() != null && panelCB.getSelectedItem().toString().equalsIgnoreCase(p.getProxyName())) {
            // Have manage p for removed proxy selected - switch to creation p
            panelCB.setSelectedItem(0);
        }
        // Remove proxy
        panelLookup.remove(p.getProxyName());
        panelCB.removeItem(panelCB.getSelectedIndex());
    }

    public static void main(String[] args) {
        ClickthroughProxyManagerF f = new ClickthroughProxyManagerF();
        f.setVisible(true);
    }
}
