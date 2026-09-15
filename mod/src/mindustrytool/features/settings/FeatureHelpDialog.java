package mindustrytool.features.settings;

import arc.Core;
import mindustrytool.features.Feature;
import solim.overlay.SolimDialog;

/**
 * Dedicated dialog displaying help and usage documentation for a specific {@link Feature}. Built
 * using SolimDialog with automatic lifecycle management and declarative Solim components.
 */
public class FeatureHelpDialog extends SolimDialog {

	public FeatureHelpDialog(Feature feature) {
		super(Core.bundle.format("feature.help.title", feature.getName()));

		addCloseButton();
		closeOnBack();

		children(() -> new FeatureHelpView(feature));
	}
}
