package mindustrytool.features.godmode;

import static solim.UI.*;

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
                                        .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), draggable(feature.xSignal, feature.ySignal))
                                        .children(() -> icon(Icon.move).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize))));

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
                    .onClick(() -> new GodModeTeamDialog(provider).show())
                    .props(enabled(canEdit), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.team")))
                    .children(() -> icon(Icon.players).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .onClick(() -> new GodModeItemsDialog(provider).show())
                    .props(enabled(canEdit), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.items")))
                    .children(() -> icon(Icon.box).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .onClick(() -> new GodModeUnitsDialog(provider).show())
                    .props(enabled(canEdit), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.units")))
                    .children(() -> icon(Icon.units).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .onClick(() -> new GodModeEffectsDialog(provider).show())
                    .props(enabled(canEdit), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.effects")))
                    .children(() -> icon(Icon.effect).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .onClick(() -> new GodModeCoreDialog(provider).show())
                    .props(enabled(canEdit), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.core")))
                    .children(() -> icon(Icon.hammer).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

            button()
                    .onClick(feature::toggleFog)
                    .props(enabled(canEdit), 
                            style(WebStyles.filterChip()),
                            size(buttonSize),
                            prefWidth(buttonSize),
                            prefHeight(buttonSize),
                            checked(feature.fogDisabledSignal()),
                            tooltip(feature.fogDisabledSignal().map(disabled -> Boolean.TRUE.equals(disabled)
                                    ? Core.bundle.get("feature.god-mode.hud.fog-off")
                                    : Core.bundle.get("feature.god-mode.hud.fog-on"))))
                    .children(() -> icon(Icon.eye).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(
                            feature.fogDisabledSignal()
                                    .map(dis -> Boolean.TRUE.equals(dis) ? Color.gold : WebStyles.Colors.GHOST_FG))));
        });
    }

    private static Component buildUnavailableContent(GodModeFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        return row().props(paddingLeft(unit(1)), paddingRight(unit(1)), gap(unit(1.5f)), center()).children(() -> {
            text(Core.bundle.get("feature.god-mode.hud.no-provider"))
                    .props(color(WebStyles.Colors.DANGER), center());

            button()
                    .onClick(feature::checkProvider)
                    .props(enabled(canEdit), style(WebStyles.outline()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.god-mode.hud.refresh-provider")))
                    .children(() -> icon(Icon.refresh).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(WebStyles.Colors.PRIMARY_FG)));
        });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
