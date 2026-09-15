package mindustrytool.features;

import lombok.Data;

@Data
public class FeatureStateChanged {
	private final Feature feature;
	private final boolean enabled;
}
