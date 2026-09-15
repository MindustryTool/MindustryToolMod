package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.overlay.Popup;
import solim.signal.Readable;

/**
 * QuickAccess popup shell for GodMode. Preserves the existing cheat tools by
 * rendering the same provider-bound controls in a stacked vertical mini-panel,
 * under the same placement, suppression, fallback, and localization rules as
 * the TimeControl popup.
 */
public final class GodModePopup {

    private static @Nullable Popup<GodModeFeature> menu;

    private GodModePopup() {
    }

    public static void show(GodModeFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(GodModePopup::buildContent).rounded(2);
        }
        float stageW = Core.scene != null ? Core.scene.getWidth() : 0f;
        float stageH = Core.scene != null ? Core.scene.getHeight() : 0f;
        float barX = 0f;
        float barY = 0f;
        float barW = 0f;
        float barH = 0f;
        if (quickAccessBar != null) {
            barX = quickAccessBar.x;
            barY = quickAccessBar.y;
            barW = quickAccessBar.getWidth();
            barH = quickAccessBar.getHeight();
        }
        float barCenterX = barX + barW / 2f;
        float barTop = barY + barH;
        float barCenterY = barY + barH / 2f;
        boolean upperHalf = stageH > 0f ? barCenterY >= stageH / 2f : false;
        float anchorX = quickAccessBar != null ? barCenterX : stageW / 2f;
        float anchorY = quickAccessBar != null ? (upperHalf ? barY : barTop) : stageH / 2f;

        menu.show(feature, anchorX, anchorY);

        if (Core.scene != null && quickAccessBar != null && menu.table() != null) {
            try {
                float menuW = menu.table().getWidth();
                float menuH = menu.table().getHeight();
                float desiredX = barCenterX - menuW / 2f;
                float desiredY = upperHalf ? barY - menuH : barTop;
                float clampedX = stageW > menuW ? Math.max(0f, Math.min(desiredX, stageW - menuW)) : 0f;
                float clampedY = stageH > menuH ? Math.max(0f, Math.min(desiredY, stageH - menuH)) : 0f;
                menu.table().setPosition(clampedX, clampedY);
            } catch (Exception ignored) {
            }
        }
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable GodModeFeature feature) {
        if (feature == null) {
            return column().children(() -> text(Core.bundle.get("feature.god-mode.popup.title")));
        }
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));
        Readable<Boolean> canEdit = feature.enabled();

        return column()
                .growX()
                .padding(unit(2))
                .gap(unit(1))
                .children(() -> {
                    text(Core.bundle.get("feature.god-mode.popup.title")).center();
                    dynamic(feature.providerSignal(),
                            provider -> provider != null
                                    ? buildActiveTools(feature, provider, buttonSize, iconSize, canEdit)
                                    : buildUnavailableContent(feature, buttonSize, iconSize, canEdit));
                });
    }

    private static Component buildActiveTools(GodModeFeature feature, GodModeProvider provider,
            Readable<Float> buttonSize, Readable<Float> iconSize, Readable<Boolean> canEdit) {
        return column().growX().gap(unit(1)).children(() -> {
            toolRow(Core.bundle.get("feature.god-mode.hud.team"), Icon.players, buttonSize, iconSize, canEdit,
                    () -> new GodModeTeamDialog(provider).show());
            toolRow(Core.bundle.get("feature.god-mode.hud.items"), Icon.box, buttonSize, iconSize, canEdit,
                    () -> new GodModeItemsDialog(provider).show());
            toolRow(Core.bundle.get("feature.god-mode.hud.units"), Icon.units, buttonSize, iconSize, canEdit,
                    () -> new GodModeUnitsDialog(provider).show());
            toolRow(Core.bundle.get("feature.god-mode.hud.effects"), Icon.effect, buttonSize, iconSize, canEdit,
                    () -> new GodModeEffectsDialog(provider).show());
            toolRow(Core.bundle.get("feature.god-mode.hud.core"), Icon.hammer, buttonSize, iconSize, canEdit,
                    () -> new GodModeCoreDialog(provider).show());

            button()
                    .style(WebStyles.filterChip())
                    .height(buttonSize)
                    .growX()
                    .checked(feature.fogDisabledSignal())
                    .enabled(canEdit)
                    .tooltip(feature.fogDisabledSignal().map(disabled -> Boolean.TRUE.equals(disabled)
                            ? Core.bundle.get("feature.god-mode.hud.fog-off")
                            : Core.bundle.get("feature.god-mode.hud.fog-on")))
                    .onClick(feature::toggleFog)
                    .children(() -> {
                        icon(Icon.eye).size(iconSize).color(feature.fogDisabledSignal()
                                .map(dis -> Boolean.TRUE.equals(dis) ? Color.gold : WebStyles.Colors.GHOST_FG));
                        text(feature.fogDisabledSignal().map(disabled -> Boolean.TRUE.equals(disabled)
                                ? Core.bundle.get("feature.god-mode.hud.fog-off")
                                : Core.bundle.get("feature.god-mode.hud.fog-on")));
                    });
        });
    }

    private static Component toolRow(String label, Drawable icon, Readable<Float> buttonSize,
            Readable<Float> iconSize, Readable<Boolean> canEdit, Runnable onClick) {
        return button()
                .style(WebStyles.ghost())
                .height(buttonSize)
                .growX()
                .enabled(canEdit)
                .tooltip(label)
                .onClick(onClick)
                .children(() -> {
                    icon(icon).size(iconSize);
                    text(label);
                });
    }

    private static Component buildUnavailableContent(GodModeFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, Readable<Boolean> canEdit) {
        return column().growX().gap(unit(1)).center().children(() -> {
            text(Core.bundle.get("feature.god-mode.hud.no-provider"))
                    .color(WebStyles.Colors.DANGER)
                    .center();

            button()
                    .style(WebStyles.outline())
                    .height(buttonSize)
                    .growX()
                    .enabled(canEdit)
                    .tooltip(Core.bundle.get("feature.god-mode.hud.refresh-provider"))
                    .onClick(feature::checkProvider)
                    .children(() -> {
                        icon(Icon.refresh).size(iconSize).color(WebStyles.Colors.PRIMARY_FG);
                        text(Core.bundle.get("feature.god-mode.hud.refresh-provider"));
                    });
        });
    }
}
