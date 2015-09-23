package crw.proxy.clickthrough;

import com.perc.mitpas.adi.common.datamodels.Feature;
import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.common.datamodels.ModelCapability;
import com.perc.mitpas.adi.common.datamodels.ModelCapabilityType;
import com.perc.mitpas.adi.common.datamodels.VehicleAsset;
import crw.proxy.CrwProxyServer;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import sami.engine.Engine;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class CreateSimProxyP extends JPanel {

    protected JTextField nameTF;
    protected JPanel capabP;
    protected JScrollPane capabSP;
    protected JButton createB;
    protected ArrayList<JCheckBox> checkBoxL;

    public CreateSimProxyP() {
        super();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        nameTF = new JTextField("Clickthrough" + ((CrwProxyServer) (Engine.getInstance().getProxyServer())).getProxyCounter());

        capabP = new JPanel();
        capabP.setLayout(new BoxLayout(capabP, BoxLayout.Y_AXIS));
        checkBoxL = new ArrayList<JCheckBox>();
        String[] capabList = new String[]{"DO", "ES2", "Sample"};
        for (String capab : capabList) {
            JCheckBox capabCheckBox = new JCheckBox(capab, false);
            checkBoxL.add(capabCheckBox);
            capabP.add(capabCheckBox);
        }
        capabSP = new JScrollPane();
        capabSP.setViewportView(capabP);

        createB = new JButton("CREATE");
        createB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProxyInt proxy = new ClickthroughProxy(nameTF.getText(), Color.WHITE, ((CrwProxyServer) (Engine.getInstance().getProxyServer())).getProxyCounter());

                VehicleAsset v = new VehicleAsset(nameTF.getText());
                ModelCapabilityType capType = new ModelCapabilityType("Airboat");
                ArrayList<Feature> mFeatures = new ArrayList<Feature>();
                for (JCheckBox cb : checkBoxL) {
                    if (cb.isSelected()) {
                        mFeatures.add(new Feature(new FeatureType(cb.getText())));
                    }
                }
                ModelCapability modelCap = new ModelCapability(capType);
                modelCap.setFeatures(mFeatures);

                ((CrwProxyServer) (Engine.getInstance().getProxyServer())).addProxy(proxy, v);

                nameTF.setText("Clickthrough" + ((CrwProxyServer) (Engine.getInstance().getProxyServer())).getProxyCounter());

            }
        });

        add(nameTF);
        add(capabSP);
        add(createB);
    }
}
