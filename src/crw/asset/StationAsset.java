package crw.asset;

import com.perc.mitpas.adi.common.datamodels.Feature;
import com.perc.mitpas.adi.common.datamodels.FeatureType;
import com.perc.mitpas.adi.common.datamodels.ModelCapability;
import com.perc.mitpas.adi.common.datamodels.ModelCapabilityType;
import com.perc.mitpas.adi.common.datamodels.VehicleAsset;
import java.util.ArrayList;

/**
 *
 * @author nbb
 */
public class StationAsset extends VehicleAsset {

    public StationAsset() {
        super();
        init();
    }

    public StationAsset(int id) {
        super("station" + id);
        init();
    }

    public void init() {
        ArrayList<Feature> features = new ArrayList<Feature>();
        Feature feature = new Feature();
        feature.setType(new FeatureType("Recharge"));
        features.add(feature);
        ModelCapability modelCapability = new ModelCapability(new ModelCapabilityType("Station"));
        modelCapability.setFeatures(features);
        addAssetCapability(modelCapability);
    }
}
