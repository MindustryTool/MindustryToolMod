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
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.PopupDisplayFeature;
import mindustrytool.features.settings.FeatureSettingDialog;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.signal.Readable;
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

    private final QuickAccessFeature parentFeature;
    private @Nullable Hud hud;

    public QuickAccessHudView(QuickAccessFeature parentFeature) {
        this.parentFeature = parentFeature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = parentFeature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11f) * s);
        Readable<Float> iconSize = scale.map(s -> unit(7f) * s);

        Readable<List<HudItem>> items = parentFeature.hiddenFeaturesConfig.signal().map(this::computeVisibleItems);

        hud = hud(() -> {
            row()
                    .padding(unit(1))
                    .gap(unit(1))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1.5f, WebStyles.Colors.BORDER)
                    .center()
                    .children(() -> {
                        dynamic(parentFeature.hideDragHandleConfig.signal(), hide -> {
                            if (!Boolean.TRUE.equals(hide)) {
                                return button()
                                        .style(WebStyles.ghost())
                                        .size(buttonSize)
                                        .children(() -> icon(Icon.move).size(iconSize))
                                        .draggable(parentFeature.xSignal, parentFeature.ySignal);
                            }
                            return null;
                        });

                        grid(parentFeature.colsConfig.signal().map(c -> Math.min(c, items.get().size())), items,
                                HudItem::id,
                                item -> createItemButton(item, buttonSize, iconSize))
                                        .gap(unit(1));
                    });
        });

        hud.opacity(parentFeature.opacityConfig.signal());
        hud.position(parentFeature.xSignal, parentFeature.ySignal);

        return hud.element();
    }

    private List<HudItem> computeVisibleItems(@Nullable Set<String> hidden) {
        List<HudItem> list = new ArrayList<>();
        Seq<Feature> features = FeatureManager.getFeatures();
        for (Feature f : features) {
            if (f == parentFeature)
                continue;

            FeatureMetadata meta = f.getMetadata();
            if (meta.isDevelopment() || !meta.isQuickAccess())
                continue;
            if (hidden != null && hidden.contains(meta.getId()))
                continue;

            list.add(new HudItem(meta.getId(), f));
        }
        list.add(new HudItem("__settings__", null));
        return list;
    }

    private Component createItemButton(HudItem item, Readable<Float> buttonSize, Readable<Float> iconSize) {

        if (item.feature != null) {
            Feature f = item.feature;
            FeatureMetadata meta = f.getMetadata();

            return button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(f.getName())
                    .onClick(() -> {
                        if (f instanceof PopupDisplayFeature && ((PopupDisplayFeature) f).isPopupMode()) {
                            Element bar = hud != null ? hud.element() : null;
                            ((PopupDisplayFeature) f).togglePopup(bar);
                        } else {
                            f.setEnabled(!f.isEnabled());
                        }
                    })
                    .onLongClick(300L, () -> {
                        Prov<SolimDialog> settingDlg = f.getSettingDialog();
                        if (settingDlg != null) {
                            settingDlg.get().show();
                        }
                    })
                    .children(() -> icon(meta.getIcon()).size(iconSize)
                            .color(f.enabled().map(en -> en ? Color.white : Color.darkGray)));
        } else {
            return button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .onClick(() -> new FeatureSettingDialog().show())
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
