package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Tmp;
import java.util.Locale;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.LanguageDialog;
import mindustrytool.features.translation.TranslationFeature;
import solim.core.BaseComponent;
import solim.input.Button;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Dropdown selector for Outgoing Translation target language.
 * Displays current language with dropdown arrow indicator, and opens
 * a native Mindustry scrollable popup menu on click.
 */
public class LanguageDropdown extends BaseComponent {

	private final TranslationFeature feature;
	private final Signal<Boolean> isOpen = signal(false);
	private @Nullable Element backdrop;
	private @Nullable Table popup;
	private @Nullable Button triggerButton;

	public LanguageDropdown(TranslationFeature feature) {
		this.feature = feature;
	}

	@Override
	protected Element build() {
		Readable<String> targetLangName = feature.outgoingTargetLangConfig.signal()
				.map(feature::getOutgoingTargetLanguageDisplayName);
		Readable<Drawable> arrowIcon = isOpen
				.map(open -> Boolean.TRUE.equals(open) ? Icon.upOpenSmall : Icon.downOpenSmall);

		triggerButton = button(this::toggle)
				.style(Styles.defaultb)
				.height(unit(10))
				.width(unit(50))
				.margin(unit(1), unit(3), unit(1), unit(3));

		triggerButton.children(() -> {
			text(targetLangName).left();
			spacer();
			icon(arrowIcon).size(unit(4));
		});

		return triggerButton.element();
	}

	public void toggle() {
		if (Boolean.TRUE.equals(isOpen.peek())) {
			close();
		} else {
			open();
		}
	}

	public void open() {
		if (triggerButton == null || Core.scene == null)
			return;

		close();

		Element btn = triggerButton.element();
		Vec2 stagePos = btn.localToStageCoordinates(Tmp.v1.set(0f, 0f));
		float btnW = btn.getWidth();
		float btnH = btn.getHeight();
		float menuW = Math.max(btnW, 300f);
		float menuH = 270f;

		float posX = Math.max(10f, Math.min(stagePos.x + btnW - menuW, Core.scene.getWidth() - menuW - 10f));
		float posY = stagePos.y - menuH;
		if (posY < 10f) {
			posY = stagePos.y + btnH;
		}

		backdrop = new Element();
		backdrop.setFillParent(true);
		backdrop.clicked(this::close);

		popup = new Table(Tex.paneSolid);
		popup.setSize(menuW, menuH);
		popup.setPosition(posX, posY);

		Table list = new Table();
		list.top().left();

		TextButton selectedItem = null;

		String noneText = Core.bundle.get("feature.translation.outgoing.none", Core.bundle.get("none", "None"));
		boolean isNoneSelected = !Boolean.TRUE.equals(feature.outgoingEnabledConfig.signal().peek())
				|| "none".equalsIgnoreCase(feature.outgoingTargetLangConfig.signal().peek());

		TextButton noneItem = new TextButton(noneText, Styles.flatTogglet);
		noneItem.setChecked(isNoneSelected);
		noneItem.getLabel().setAlignment(Align.left);
		noneItem.getLabelCell().padLeft(12f).growX();
		noneItem.clicked(() -> {
			feature.outgoingEnabledConfig.set(false);
			feature.outgoingTargetLangConfig.set("none");
			close();
		});
		list.add(noneItem).growX().height(38f).pad(2f, 4f, 2f, 4f).row();
		if (isNoneSelected) {
			selectedItem = noneItem;
		}

		for (Locale loc : Vars.locales) {
			boolean isSelected = !isNoneSelected && feature.isSameLanguage(feature.getOutgoingTargetLanguage(), loc);
			TextButton item = new TextButton(LanguageDialog.getDisplayName(loc), Styles.flatTogglet);
			item.setChecked(isSelected);
			item.getLabel().setAlignment(Align.left);
			item.getLabelCell().padLeft(12f).growX();
			item.clicked(() -> {
				feature.outgoingEnabledConfig.set(true);
				feature.setOutgoingTargetLocale(loc);
				close();
			});
			list.add(item).growX().height(38f).pad(2f, 4f, 2f, 4f).row();
			if (isSelected) {
				selectedItem = item;
			}
		}

		ScrollPane pane = new ScrollPane(list, Styles.defaultPane);
		pane.setScrollingDisabled(true, false);
		pane.setOverscroll(false, false);
		pane.setFadeScrollBars(false);
		pane.update(() -> {
			if (pane.hasScroll()) {
				Element hover = Core.scene != null ? Core.scene.getHoverElement() : null;
				if (hover == null || !hover.isDescendantOf(pane) || (!pane.isScrollX() && !pane.isScrollY())) {
					Core.scene.setScrollFocus(null);
				}
			}
		});
		popup.add(pane).grow();

		Core.scene.add(backdrop);
		Core.scene.add(popup);
		isOpen.set(true);

		if (selectedItem != null) {
			final TextButton target = selectedItem;
			Core.app.post(() -> {
				pane.layout();
				pane.scrollTo(0f, target.y, target.getWidth(), target.getHeight());
			});
		}
	}

	public void close() {
		if (popup != null && Core.scene != null && Core.scene.getScrollFocus() != null
				&& Core.scene.getScrollFocus().isDescendantOf(popup)) {
			Core.scene.setScrollFocus(null);
		}
		if (backdrop != null) {
			backdrop.remove();
			backdrop = null;
		}
		if (popup != null) {
			popup.remove();
			popup = null;
		}
		isOpen.set(false);
	}

	@Override
	protected void onDispose() {
		close();
	}
}
