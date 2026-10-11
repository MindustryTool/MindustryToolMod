package mindustrytool.features.griefalert;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.layout.Row;
import solim.reactive.Readable;

/**
 * Top-centered alert banner for {@link GriefAlertFeature}. Mounted inside a full-screen
 * transparent table attached to {@code Vars.ui.hudGroup}. Clicking the banner jumps the
 * camera onto the offending placement; the trailing button closes it without moving the camera.
 */
public class GriefAlertHudView extends BaseComponent {

    private static final float TOP_INSET = 16f;

    private final GriefAlertFeature feature;

    public GriefAlertHudView(GriefAlertFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Table root = new Table();
        root.name = "grief-alert-hud";
        root.setFillParent(true);
        root.top();
        root.touchable = Touchable.childrenOnly;
        root.visible(this::shouldShow);

        Readable<String> message = feature.activeAlertSignal()
                .map(alert -> alert != null ? alertText(alert) : "");

        Row banner = row()
                .padding(unit(1.5f))
                .gap(unit(2))
                .center()
                .rounded(unit(2), WebStyles.Colors.DARK_90)
                .border(1.5f, WebStyles.Colors.DANGER)
                .tooltip(Core.bundle.get("feature.grief-alert.hud.jump"))
                .onClick(this::jump)
                .children(() -> {
                    icon(Icon.warning).size(unit(6)).color(WebStyles.Colors.DANGER);

                    text(message)
                            .wrap()
                            .growX()
                            .left()
                            .color(Color.white);

                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.grief-alert.hud.dismiss"))
                            .onClick(feature::dismissAlert)
                            .stopClickPropagation(true)
                            .children(() -> icon(Icon.cancel).size(unit(6)));
                });

        root.add(banner.element()).padTop(TOP_INSET);
        banner.maxWidth(dvw(90f));

        return root;
    }

    private boolean shouldShow() {
        return feature.isEnabled()
                && feature.hasActiveAlert()
                && Vars.state != null && Vars.state.isGame()
                && Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown;
    }

    private void jump() {
        feature.jumpToAlert(feature.activeAlert());
    }

    private static String alertText(GriefAlertInfo alert) {
        return Core.bundle.format("feature.grief-alert.alert",
                alert.builderName,
                alert.blockName,
                String.valueOf(alert.tileX),
                String.valueOf(alert.tileY),
                String.valueOf(alert.coreTileX),
                String.valueOf(alert.coreTileY));
    }
}
