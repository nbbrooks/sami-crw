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
public class BoatAsset extends VehicleAsset {

    public BoatAsset() {
        super();
        init();
    }

    public BoatAsset(int id) {
        super("boat" + id);
        init();
    }

    public void init() {
        ArrayList<Feature> features = new ArrayList<Feature>();
        Feature feature = new Feature();
        feature.setType(new FeatureType("Water"));
        feature.setType(new FeatureType("DO"));
        feature.setType(new FeatureType("ES2"));
        features.add(feature);
        ModelCapability modelCapability = new ModelCapability(new ModelCapabilityType("Boat"));
        modelCapability.setFeatures(features);
        addAssetCapability(modelCapability);
    }
}
