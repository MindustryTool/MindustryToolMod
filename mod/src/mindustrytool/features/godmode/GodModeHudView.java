package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.draggable;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.paddingLeft;
import static solim.modifier.Values.paddingRight;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Readable;

public class GodModeHudView extends Component {

    private final GodModeFeature feature;
    private @Nullable Hud hud;

    public GodModeHudView(GodModeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        hud = hud(() -> {
            row()
                    .props(padding(unit(1)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER), gap(unit(1)), center())
                    .children(() -> {
                        when(feature.hideDragHandleConfig.signal())
                                .elseDo(() -> button()
                                        .style(WebStyles.ghost())
                                        .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize))
                                        .children(() -> icon(Icon.move).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)))
                                        .props(draggable(feature.xSignal, feature.ySignal)));

                        buildControls(feature, null);
                    });
        });

        hud.props(position(feature.xSignal, feature.ySignal));

        effect(() -> {
            scale.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildControls(GodModeFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        return dynamic(feature.providerSignal(), provider -> {
            if (provider != null) {
                buildActiveTools(feature, provider, buttonSize, iconSize, canEdit);
            } else {
                buildUnavailableContent(feature, buttonSize, iconSize, canEdit);
            }
        });
    }

    private static Component buildActiveTools(GodModeFeature feature, GodModeProvider provider,
            Readable<Float> buttonSize, Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        return row().props(gap(unit(1)), center()).children(() -> {
            button()
                    .style(WebStyles.ghost())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.team")))
                    .onClick(() -> new GodModeTeamDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.players).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .style(WebStyles.ghost())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.items")))
                    .onClick(() -> new GodModeItemsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.box).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .style(WebStyles.ghost())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.units")))
                    .onClick(() -> new GodModeUnitsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.units).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .style(WebStyles.ghost())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.effects")))
                    .onClick(() -> new GodModeEffectsDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.effect).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .style(WebStyles.ghost())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.core")))
                    .onClick(() -> new GodModeCoreDialog(provider).show())
                    .enabled(canEdit)
                    .children(() -> icon(Icon.hammer).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .style(WebStyles.filterChip())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize))
                    .checked(feature.fogDisabledSignal())
                    .props(tooltip(feature.fogDisabledSignal().map(disabled -> Boolean.TRUE.equals(disabled)
                            ? Core.bundle.get("feature.god-mode.hud.fog-off")
                            : Core.bundle.get("feature.god-mode.hud.fog-on"))))
                    .onClick(feature::toggleFog)
                    .enabled(canEdit)
                    .children(() -> icon(Icon.eye).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)).color(
                            feature.fogDisabledSignal()
                                    .map(dis -> Boolean.TRUE.equals(dis) ? Color.gold : WebStyles.Colors.GHOST_FG)));
        });
    }

    private static Component buildUnavailableContent(GodModeFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        return row().props(paddingLeft(unit(1)), paddingRight(unit(1)), gap(unit(1.5f)), center()).children(() -> {
            text(Core.bundle.get("feature.god-mode.hud.no-provider"))
                    .color(WebStyles.Colors.DANGER)
                    .center();

            button()
                    .style(WebStyles.outline())
                    .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.refresh-provider")))
                    .onClick(feature::checkProvider)
                    .enabled(canEdit)
                    .children(() -> icon(Icon.refresh).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)).color(WebStyles.Colors.PRIMARY_FG));
        });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
