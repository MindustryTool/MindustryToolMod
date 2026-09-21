package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Readable;

public class GodModeHudView extends BaseComponent {

    private final GodModeFeature feature;
    private @Nullable Hud hud;

    public GodModeHudView(GodModeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));

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

                        buildControls(feature, null);
                    });
        });

        hud.position(feature.xSignal, feature.ySignal);

        effect(() -> {
            scale.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildControls(GodModeFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));

        return dynamic(feature.providerSignal(), provider -> provider != null
                ? buildActiveTools(feature, provider, buttonSize, iconSize, canEdit)
                : buildUnavailableContent(feature, buttonSize, iconSize, canEdit));
    }

    private static Component buildActiveTools(GodModeFeature feature, GodModeProvider provider,
            Readable<Float> buttonSize, Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        return row().gap(unit(1)).center().children(() -> {
            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.team"))
                    .onClick(() -> new GodModeTeamDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.players).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.items"))
                    .onClick(() -> new GodModeItemsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.box).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.units"))
                    .onClick(() -> new GodModeUnitsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.units).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.effects"))
                    .onClick(() -> new GodModeEffectsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.effect).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.core"))
                    .onClick(() -> new GodModeCoreDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.hammer).size(iconSize));

            button()
                    .style(WebStyles.filterChip())
                    .size(buttonSize)
                    .checked(feature.fogDisabledSignal())
                    .tooltip(feature.fogDisabledSignal().map(disabled -> Boolean.TRUE.equals(disabled)
                            ? Core.bundle.get("feature.god-mode.hud.fog-off")
                            : Core.bundle.get("feature.god-mode.hud.fog-on")))
                    .onClick(feature::toggleFog)
                    .enabled(canEdit)
                    .children(() -> icon(Icon.eye).size(iconSize).color(
                            feature.fogDisabledSignal()
                                    .map(dis -> Boolean.TRUE.equals(dis) ? Color.gold : WebStyles.Colors.GHOST_FG)));
        });
    }

    private static Component buildUnavailableContent(GodModeFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        return row().gap(unit(1.5f)).center().paddingLeft(unit(1)).paddingRight(unit(1)).children(() -> {
            text(Core.bundle.get("feature.god-mode.hud.no-provider"))
                    .color(WebStyles.Colors.DANGER)
                    .center();

            button()
                    .style(WebStyles.outline())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.refresh-provider"))
                    .onClick(feature::checkProvider)
                    .enabled(canEdit)
                    .children(() -> icon(Icon.refresh).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));
        });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
