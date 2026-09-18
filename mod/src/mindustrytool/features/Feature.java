package mindustrytool.features;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import solim.config.ConfigGroup;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

public abstract class Feature {
	private static final String KEYBIND_CATEGORY = "MindustryTool";

	private final FeatureMetadata metadata;
	private @Nullable Signal<Boolean> enabled;
	private final Seq<FeatureKeybind> keybinds = new Seq<>();

	public Feature(FeatureMetadata metadata) {
		this.metadata = metadata;
	}

	public FeatureMetadata getMetadata() {
		return metadata;
	}

	public Signal<Boolean> enabled() {
		if (enabled == null) {
			FeatureMetadata meta = getMetadata();
			boolean defaultVal = meta != null && meta.isEnabledByDefault();
			boolean stored = Core.settings.getBool(getSettingKey(), defaultVal);
			enabled = Signal.of(meta != null && meta.isDevelopment() ? false : stored);
		}
		return enabled;
	}

	public boolean isEnabled() {
		return Boolean.TRUE.equals(enabled().peek());
	}

	public void setEnabled(boolean enabled) {
		if (enabled) {
			enable();
		} else {
			disable();
		}
	}

	public void enable() {
		if (getMetadata() != null && getMetadata().isDevelopment()) {
			return;
		}

		if (isEnabled()) {
			return;
		}

		Core.settings.put(getSettingKey(), true);
		enabled().set(true);
		onEnable();
		Events.fire(new FeatureStateChanged(this, true));
	}

	public void disable() {
		if (!isEnabled()) {
			return;
		}

		Core.settings.put(getSettingKey(), false);
		enabled().set(false);
		onDisable();
		Events.fire(new FeatureStateChanged(this, false));
	}

	public void onEnable() {}

	public void onDisable() {}

	public String getSettingKey() {
		return "mindustrytool.feature." + getMetadata().getId() + ".enabled";
	}

	public ConfigGroup configGroup() {
		return ConfigGroup.of("mindustrytool.features." + getMetadata().getId());
	}

	public @Nullable Prov<SolimDialog> getSettingDialog() {
		return null;
	}

	public @Nullable Prov<SolimDialog> getMainDialog() {
		return null;
	}

	public Seq<FeatureKeybind> getKeybinds() {
		return keybinds;
	}

	protected final KeyBind bindToggle(String name, KeyCode defaultKey) {
		return bindAction(name, defaultKey, () -> setEnabled(!isEnabled()), false);
	}

	protected final KeyBind bindAction(String name, KeyCode defaultKey, Runnable action) {
		return bindAction(name, defaultKey, action, true);
	}

	protected final KeyBind bindAction(String name, KeyCode defaultKey, Runnable action, boolean requireEnabled) {
		KeyBind bind = KeyBind.add(name, defaultKey, KEYBIND_CATEGORY);
		keybinds.add(new FeatureKeybind(bind, action, requireEnabled));
		return bind;
	}

	protected final KeyBind bindDialog(String name, KeyCode defaultKey, Runnable showDialog) {
		return bindDialog(name, defaultKey, showDialog, true);
	}

	protected final KeyBind bindDialog(String name, KeyCode defaultKey, Runnable showDialog, boolean requireEnabled) {
		return bindAction(name, defaultKey, showDialog, requireEnabled);
	}

	protected final KeyBind bindDialog(String name, KeyCode defaultKey, @Nullable Prov<SolimDialog> dialogProvider) {
		return bindDialog(name, defaultKey, dialogProvider, true);
	}

	protected final KeyBind bindDialog(String name, KeyCode defaultKey, @Nullable Prov<SolimDialog> dialogProvider,
			boolean requireEnabled) {
		return bindAction(name, defaultKey, () -> {
			SolimDialog dialog = dialogProvider != null ? dialogProvider.get() : null;
			if (dialog != null) {
				dialog.show();
			}
		}, requireEnabled);
	}

	public void onQuickAccessClick(@Nullable Element anchor) {
		onQuickAccessClick();
	}

	public void onQuickAccessClick() {
		setEnabled(!isEnabled());
	}

	public void onQuickAccessLongClick(@Nullable Element anchor) {
		onQuickAccessLongClick();
	}

	public void onQuickAccessLongClick() {
		Prov<SolimDialog> dlg = getSettingDialog() != null ? getSettingDialog() : getMainDialog();
		if (dlg != null) {
			dlg.get().show();
		}
	}

	public String getName() {
		String id = getMetadata().getId();
		String nameKey = "feature." + id + ".name";
		if (Core.bundle.has(nameKey)) {
			return Core.bundle.get(nameKey);
		}
		String directKey = "feature." + id;
		if (Core.bundle.has(directKey)) {
			return Core.bundle.get(directKey);
		}
		return id;
	}

	public String getDescription() {
		String id = getMetadata().getId();
		String descKey = "feature." + id + ".description";
		if (Core.bundle.has(descKey)) {
			return Core.bundle.get(descKey);
		}
		return "";
	}

	public String getHelp() {
		String id = getMetadata().getId();
		String helpKey = "feature." + id + ".help";
		if (Core.bundle.has(helpKey)) {
			return Core.bundle.get(helpKey);
		}
		return "";
	}
}
