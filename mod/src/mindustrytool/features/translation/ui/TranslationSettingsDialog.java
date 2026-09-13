package mindustrytool.features.translation.ui;

import arc.Core;
import arc.util.Nullable;
import mindustrytool.features.translation.TranslationFeature;
import solim.overlay.SolimDialog;

public class TranslationSettingsDialog extends SolimDialog {

	private @Nullable TranslationSettingsView view;

	public TranslationSettingsDialog(TranslationFeature feature) {
		super(Core.bundle.get("feature.translation.settings.title", "Chat Translation Settings"));

		name("translationSettingsDialog");
		addCloseButton();
		closeOnBack();

		children(() -> {
			view = new TranslationSettingsView(feature);
		});
		hidden(() -> {
			if (view != null) {
				view.closeDropdown();
			}
		});
	}
}
