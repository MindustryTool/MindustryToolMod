package mindustrytool.features.quickaccess;

import static solim.UI.*;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
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

        Readable<Boolean> isCollapsed = Signal.computed(() ->
                Boolean.TRUE.equals(feature.collapsibleConfig.signal().get())
                && Boolean.TRUE.equals(feature.collapsedConfig.signal().get())
        );

        Readable<List<HudItem>> items = Signal.computed(() -> {
            Seq<String> stored = feature.displayOrderConfig.signal().get();
            Set<String> hidden = feature.hiddenFeaturesConfig.signal().get();
            Set<String> shown = feature.shownFeaturesConfig.signal().get();
            Seq<String> normalized = QuickAccessFeature.normalizeDisplayOrder(
                    feature.quickAccessFeatures(), stored);
            return computeVisibleItems(normalized, hidden, shown);
        });

        hud = hud(() -> {
            when(isCollapsed)
                    .thenDo(() -> buildCollapsedBadge(buttonSize, iconSize))
                    .elseDo(() -> buildExpandedBar(buttonSize, iconSize, items));
        });

        hud.opacity(feature.opacityConfig.signal());
        hud.position(feature.xSignal, feature.ySignal);

        effect(() -> {
            isCollapsed.get();
            Core.app.post(this::keepInScreen);
        });

        return hud.element();
    }

    private void buildCollapsedBadge(Readable<Float> buttonSize, Readable<Float> iconSize) {
        row()
                .padding(unit(1))
                .gap(unit(1))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER)
                .center()
                .children(() -> button()
                        .style(WebStyles.ghost())
                        .size(buttonSize)
                        .tooltip(Core.bundle.get("feature.quick-access.ui.expand"))
                        .onClick(() -> feature.collapsedConfig.set(false))
                        .draggable(feature.xSignal, feature.ySignal)
                        .children(() -> icon(FileIcon.of("grid-2x2.png")).size(iconSize)));
    }

    private void buildExpandedBar(Readable<Float> buttonSize, Readable<Float> iconSize,
            Readable<List<HudItem>> items) {
        row()
                .padding(unit(1))
                .gap(unit(1))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER)
                .center()
                .children(() -> {
                    when(feature.hideDragHandleConfig.signal())
                            .elseDo(() -> button()
                                    .style(WebStyles.ghost())
                                    .size(buttonSize)
                                    .children(() -> icon(Icon.move).size(iconSize))
                                    .draggable(feature.xSignal, feature.ySignal));

                    when(feature.collapsibleConfig.signal())
                            .thenDo(() -> button()
                                    .style(WebStyles.ghost())
                                    .size(buttonSize)
                                    .tooltip(Core.bundle.get("feature.quick-access.ui.collapse"))
                                    .onClick(() -> feature.collapsedConfig.set(true))
                                    .children(() -> icon(FileIcon.of("chevron-left.png")).size(iconSize)));

                    reactiveGrid(items).key(HudItem::id)
                            .columns(feature.colsConfig.signal().map(c -> Math.min(c, items.get().size())))
                            .gap(unit(1))
                            .children(item -> createItemButton(feature, item, buttonSize, iconSize));
                });
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

    private @Nullable Element getBar() {
        return hud != null ? hud.element() : null;
    }

    private Component createItemButton(QuickAccessFeature feature, HudItem item, Readable<Float> buttonSize,
            Readable<Float> iconSize) {
        if (item.feature != null) {
            Feature f = item.feature;
            FeatureMetadata meta = f.getMetadata();

            return button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(f.getName())
                    .onClick(() -> f.onQuickAccessClick(getBar()))
                    .onLongClick(300L, () -> f.onQuickAccessLongClick(getBar()))
                    .children(() -> icon(meta.getIcon()).size(iconSize)
                            .color(f.quickAccessHighlight().map(en -> en ? Color.white : Color.darkGray)));
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
