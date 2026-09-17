package mindustrytool.features.quickaccess;

import static solim.UI.*;

import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.settings.FeatureSettingDialog;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;
import mindustrytool.components.WebStyles;

/**
 * Fully reactive and declarative QuickAccess HUD overlay. Uses reactive
 * bindings for opacity, scale, position, column reflow, and feature state.
 */
public class QuickAccessHudView extends BaseComponent {

    private static class HudItem {
        final String id;
        final @Nullable Feature feature;

        HudItem(String id, @Nullable Feature feature) {
            this.id = id;
            this.feature = feature;
        }

        String id() {
            return id;
        }
    }

    private final QuickAccessFeature feature;
    private @Nullable Hud hud;

    public QuickAccessHudView(QuickAccessFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        feature.healDisplayOrder();
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11f) * s);
        Readable<Float> iconSize = scale.map(s -> unit(7f) * s);

        Readable<List<HudItem>> items = Signal.computed(() -> {
            Seq<String> stored = feature.displayOrderConfig.signal().get();
            Set<String> hidden = feature.hiddenFeaturesConfig.signal().get();
            Set<String> shown = feature.shownFeaturesConfig.signal().get();
            Seq<String> normalized = QuickAccessFeature.normalizeDisplayOrder(
                    feature.quickAccessFeatures(), stored);
            return computeVisibleItems(normalized, hidden, shown);
        });

        hud = hud(() -> {
            row()
                    .padding(unit(1))
                    .gap(unit(1))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1.5f, WebStyles.Colors.BORDER)
                    .center()
                    .children(() -> {
                        dynamic(feature.hideDragHandleConfig.signal(), hide -> {
                            if (!Boolean.TRUE.equals(hide)) {
                                return button()
                                        .style(WebStyles.ghost())
                                        .size(buttonSize)
                                        .children(() -> icon(Icon.move).size(iconSize))
                                        .draggable(feature.xSignal, feature.ySignal);
                            }
                            return null;
                        });

                        grid(feature.colsConfig.signal().map(c -> Math.min(c, items.get().size())), items,
                                HudItem::id,
                                item -> createItemButton(feature, item, buttonSize, iconSize))
                                        .gap(unit(1));
                    });
        });

        hud.opacity(feature.opacityConfig.signal());
        hud.position(feature.xSignal, feature.ySignal);

        return hud.element();
    }

    private List<HudItem> computeVisibleItems(
            @Nullable Seq<String> order,
            @Nullable Set<String> hidden,
            @Nullable Set<String> shown) {
        List<HudItem> list = new ArrayList<>();
        for (Feature f : feature.orderedFeatures(order)) {
            FeatureMetadata meta = f.getMetadata();
            String id = meta.getId();
            boolean isHidden = hidden != null && hidden.contains(id);
            boolean isShown = shown != null && shown.contains(id);
            boolean visible = isHidden ? false : (isShown ? true : meta.isQuickAccessByDefault());
            if (!visible) {
                continue;
            }
            list.add(new HudItem(id, f));
        }
        list.add(new HudItem("__settings__", null));
        return list;
    }

    private Component createItemButton(QuickAccessFeature feature, HudItem item, Readable<Float> buttonSize,
            Readable<Float> iconSize) {
        if (item.feature != null) {
            Feature f = item.feature;
            FeatureMetadata meta = f.getMetadata();
            Element bar = hud != null ? hud.element() : null;

            return button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(f.getName())
                    .onClick(() -> f.onQuickAccessClick(bar))
                    .onLongClick(300L, () -> f.onQuickAccessLongClick(bar))
                    .children(() -> icon(meta.getIcon()).size(iconSize)
                            .color(f.enabled().map(en -> en ? Color.white : Color.darkGray)));
        } else {
            return button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .onClick(() -> new FeatureSettingDialog().show())
                    .onLongClick(300L, () -> {
                        Prov<SolimDialog> dlg = feature.getSettingDialog();
                        if (dlg != null) {
                            dlg.get().show();
                        }
                    })
                    .children(() -> icon(Icon.settings).size(iconSize));
        }
    }

    public @Nullable Hud getHud() {
        return hud;
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
