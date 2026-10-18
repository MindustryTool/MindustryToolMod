package mindustrytool.features.quickaccess;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.draggable;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.growY;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.color;
import static solim.modifier.Values.tooltip;

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
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;

public class QuickAccessHudView extends Component {

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
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11f) * s);
        Readable<Float> iconSize = scale.map(s -> unit(6f) * s);

        Readable<Boolean> isCollapsed = Signal
                .computed(() -> Boolean.TRUE.equals(feature.collapsibleConfig.signal().get())
                        && Boolean.TRUE.equals(feature.collapsedConfig.signal().get()));

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

        hud.props(solim.modifier.Values.opacity(feature.opacityConfig.signal()));
        hud.props(position(feature.xSignal, feature.ySignal));

        effect(() -> {
            isCollapsed.get();
            Core.app.post(this::keepInScreen);
        });

        return hud.element();
    }

    private void buildCollapsedBadge(Readable<Float> buttonSize, Readable<Float> iconSize) {
        row()
                .props(padding(unit(1)), gap(unit(1)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER), center())
                .children(() -> button()
                        .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.quick-access.ui.expand")))
                        .onClick(() -> feature.collapsedConfig.set(false))
                        .props(draggable(feature.xSignal, feature.ySignal))
                        .children(() -> icon(FileIcon.of("grid-2x2.png")).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize))));
    }

    private void buildExpandedBar(Readable<Float> buttonSize, Readable<Float> iconSize,
            Readable<List<HudItem>> items) {
        row()
                .props(padding(unit(1)), gap(unit(1)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER), center())
                .children(() -> {
                    when(feature.hideDragHandleConfig.signal())
                            .props(growY(), center(), gap(unit(1)))
                            .thenDo(() -> button()
                                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.quick-access.ui.collapse")))
                                    .onClick(() -> feature.collapsedConfig.set(true))
                                    .children(() -> icon(FileIcon.of("chevron-left.png")).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize))))
                            .elseDo(() -> {
                                button()
                                        .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), draggable(feature.xSignal, feature.ySignal))
                                        .children(() -> icon(Icon.move).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

                                divider(Direction.Y);
                            });

                    Readable<Integer> columns = Signal.computed(() -> {
                        Integer cols = feature.colsConfig.signal().get();
                        int maxCols = cols != null ? cols : 4;
                        List<HudItem> currentItems = items.get();
                        int count = currentItems != null ? currentItems.size() : 0;
                        return Math.max(1, Math.min(maxCols, count));
                    });

                    reactiveGrid(items)
                            .key(HudItem::id)
                            .columns(columns)
                            .props(gap(unit(1)))
                            .items(item -> createItemButton(feature, item, buttonSize, iconSize));
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
                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(f.getName()))
                    .onClick(() -> f.onQuickAccessClick(getBar()))
                    .onLongClick(300L, () -> f.onQuickAccessLongClick(getBar()))
                    .children(() -> icon(meta.getIcon()).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(f.quickAccessHighlight().map(en -> en ? Color.white : Color.darkGray))));
        } else {
            return button()
                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize))
                    .onClick(() -> new FeatureSettingDialog().show())
                    .onLongClick(300L, () -> {
                        Prov<SolimDialog> dlg = feature.getSettingDialog();
                        if (dlg != null) {
                            dlg.get().show();
                        }
                    })
                    .children(() -> icon(Icon.settings).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));
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