package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.signal.Readable;

public class GodModeHudView extends BaseComponent {

    private final GodModeFeature parentFeature;
    private @Nullable Hud hud;

    public GodModeHudView(GodModeFeature parentFeature) {
        this.parentFeature = parentFeature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = parentFeature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        hud = hud(() -> {
            row()
                    .padding(unit(1))
                    .gap(unit(1))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1.5f, WebStyles.Colors.BORDER)
                    .center()
                    .children(() -> {
                        button()
                                .style(WebStyles.ghost())
                                .size(buttonSize)
                                .children(() -> icon(Icon.move).size(iconSize))
                                .draggable(parentFeature.xSignal, parentFeature.ySignal);

                        dynamic(parentFeature.providerSignal(), provider ->
                                provider != null
                                        ? buildActiveTools(provider, buttonSize, iconSize)
                                        : buildUnavailableContent(buttonSize, iconSize));
                    });
        });

        hud.position(parentFeature.xSignal, parentFeature.ySignal);

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            scale.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    private Component buildActiveTools(GodModeProvider provider, Readable<Float> buttonSize, Readable<Float> iconSize) {
        return row().gap(unit(1)).center().children(() -> {
            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.team"))
                    .onClick(() -> new GodModeTeamDialog(provider).show())
                    .children(() -> icon(Icon.players).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.items"))
                    .onClick(() -> new GodModeItemsDialog(provider).show())
                    .children(() -> icon(Icon.box).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.units"))
                    .onClick(() -> new GodModeUnitsDialog(provider).show())
                    .children(() -> icon(Icon.units).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.effects"))
                    .onClick(() -> new GodModeEffectsDialog(provider).show())
                    .children(() -> icon(Icon.effect).size(iconSize));

            button()
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.core"))
                    .onClick(() -> new GodModeCoreDialog(provider).show())
                    .children(() -> icon(Icon.hammer).size(iconSize));

            button()
                    .style(WebStyles.filterChip())
                    .size(buttonSize)
                    .checked(parentFeature.fogDisabledSignal())
                    .tooltip(parentFeature.fogDisabledSignal().map(disabled ->
                            Boolean.TRUE.equals(disabled)
                                    ? Core.bundle.get("feature.god-mode.hud.fog-off")
                                    : Core.bundle.get("feature.god-mode.hud.fog-on")))
                    .onClick(parentFeature::toggleFog)
                    .children(() -> icon(Icon.eye).size(iconSize).color(
                            parentFeature.fogDisabledSignal().map(dis -> Boolean.TRUE.equals(dis) ? Color.gold : WebStyles.Colors.GHOST_FG)));
        });
    }

    private Component buildUnavailableContent(Readable<Float> buttonSize, Readable<Float> iconSize) {
        return row().gap(unit(1.5f)).center().paddingLeft(unit(1)).paddingRight(unit(1)).children(() -> {
            text(Core.bundle.get("feature.god-mode.hud.no-provider"))
                    .color(WebStyles.Colors.DANGER)
                    .center();

            button()
                    .style(WebStyles.outline())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.refresh-provider"))
                    .onClick(parentFeature::checkProvider)
                    .children(() -> icon(Icon.refresh).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));
        });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
