package solim.core;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.util.Log;
import solim.display.Text;
import solim.input.Button;
import solim.layout.Column;
import solim.layout.Direction;
import solim.layout.Divider;
import solim.layout.Row;
import solim.layout.Spacer;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;

/** Example final API settings panel from requirement.md §16. */
public final class SettingsPanel extends BaseComponent {

	private final Signal<Boolean> darkMode = Signal.of(false);

	private final Signal<Boolean> dirty = Signal.of(false);

	private final Computed<String> saveText = dirty.map(value ->
			value ? t("solim.settings.save-dirty", "● Save Changes") : t("solim.settings.save", "Save Changes"));

	private Effect logger;

	private static String t(String key, String fallback) {
		if (Core.bundle != null && Core.bundle.has(key)) {
			try {
				return Core.bundle.get(key);
			} catch (Throwable ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	private static final TextButtonStyle PRIMARY_STYLE = new TextButtonStyle() {{
		fontColor = Color.white;
		overFontColor = Color.white;
	}};
	private static final TextButtonStyle GHOST_STYLE = new TextButtonStyle() {{
		fontColor = Color.white;
		overFontColor = Color.white;
	}};

	@Override
	protected Element build() {

		logger = Effect.of(() -> {
			Log.info("Dirty state: @", dirty.get());
		});

		return new Column().padding(24)
				.gap(16)
				.children(() -> {
					text(t("solim.settings.title", "Settings"));

					Divider divider = new Divider(Direction.X);
					AttachmentStack.attachToParent(divider.element());

					new Row().children(() -> {
						text(t("solim.settings.dark-mode", "Dark Mode"));

						button(() -> {
							darkMode.set(!darkMode.get());
							dirty.set(true);
						})
								.style(darkMode.get() ? PRIMARY_STYLE : GHOST_STYLE)
								.children(() -> text(darkMode.map(value -> value
										? t("solim.settings.dark-mode.on", "On")
										: t("solim.settings.dark-mode.off", "Off"))));
					});

					Spacer spacer = new Spacer();
					AttachmentStack.attachToParent(spacer.element());

					new Row().right().gap(8).children(() -> {
						button(() -> {
							dirty.set(false);
						}).children(() -> text(t("solim.settings.discard", "Discard")));

						button(() -> {
									dirty.set(false);
								})
								.enabled(dirty)
								.style(PRIMARY_STYLE)
								.children(() -> text(saveText));
					});
				})
				.element();
	}

	private static void text(String value) {
		AttachmentStack.attachToParent(Text.of(value).label());
	}

	private static void text(Readable<String> value) {
		AttachmentStack.attachToParent(Text.of(value).label());
	}

	private static Button button(Runnable onClick) {
		Button button = new Button(onClick);
		AttachmentStack.attachToParent(button.element());
		return button;
	}

	@Override
	public void dispose() {
		if (logger != null) {
			logger.dispose();
		}
		super.dispose();
	}

	public Signal<Boolean> darkMode() {
		return darkMode;
	}

	public Signal<Boolean> dirty() {
		return dirty;
	}

	public Computed<String> saveText() {
		return saveText;
	}

	public Effect logger() {
		return logger;
	}
}
