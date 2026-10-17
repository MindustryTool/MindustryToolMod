package mindustrytool.features.modmanager;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import solim.layout.Card;

public final class ModStatusBadges {

    public static final Color LIGHT_BLUE = Color.valueOf("D3FDFF");

    private ModStatusBadges() {
    }

    public static void renderCornerIcons(
            int stat,
            boolean isRemote,
            @Nullable String updateVersion,
            boolean isInstalled) {
        row().gap(unit(1)).right().top().children(() -> {
            if (isRemote) {
                if (isInstalled) {
                    if (updateVersion != null && !updateVersion.isEmpty()) {
                        cornerIcon(
                                Icon.starSmall,
                                LIGHT_BLUE,
                                Core.bundle.format("mod-manager.stat.update-available", updateVersion));
                    } else {
                        cornerIcon(
                                Icon.okSmall,
                                Pal.heal,
                                Core.bundle.get("mod-browser.badge.installed"));
                    }
                }
            } else {
                if (updateVersion != null && !updateVersion.isEmpty()) {
                    cornerIcon(
                            Icon.upSmall,
                            LIGHT_BLUE,
                            Core.bundle.format("mod-manager.stat.update-available", updateVersion));
                } else if (ModStat.isValid(stat) && ModStat.isEnabled(stat)) {
                    cornerIcon(
                            Icon.okSmall,
                            Pal.heal,
                            Core.bundle.get("mod-manager.stat.up-to-date.desc"));
                }
                if (ModStat.isLocalFile(stat)) {
                    cornerIcon(
                            Icon.fileSmall,
                            Color.white,
                            Core.bundle.get("mod-manager.stat.local.desc"));
                }
            }

            if (ModStat.isJAR(stat)) {
                cornerIcon(
                        Icon.terminal,
                        Pal.reactorPurple,
                        Core.bundle.get("mod-manager.stat.jar.desc"));
            }
            if (ModStat.isJS(stat)) {
                jsBadge(Core.bundle.get("mod-manager.stat.js.desc"));
            }
            if (ModStat.isClientOnly(stat)) {
                cornerIcon(
                        Icon.hostSmall,
                        Pal.techBlue,
                        Core.bundle.get("mod-manager.stat.client-only.desc"));
            }

            if (ModStat.isDeprecated(stat)) {
                int minVer = ModStat.isJAR(stat) ? Vars.minJavaModGameVersion : Vars.minModGameVersion;
                cornerIcon(
                        Icon.warningSmall,
                        Color.crimson,
                        Core.bundle.format("mod-manager.stat.deprecated.detail", minVer));
            } else if (ModStat.isUnsupported(stat)) {
                cornerIcon(
                        Icon.warningSmall,
                        Color.crimson,
                        Core.bundle.get("mod-manager.stat.unsupported.desc"));
            }

            if (!ModStat.isValid(stat)) {
                if (ModStat.isLibMissing(stat)) {
                    cornerIcon(
                            Icon.layersSmall,
                            Color.crimson,
                            Core.bundle.get("mod-manager.stat.lib-missing.desc"));
                } else if (ModStat.isLibIncomplete(stat)) {
                    cornerIcon(
                            Icon.warningSmall,
                            Color.crimson,
                            Core.bundle.get("mod-manager.stat.lib-incomplete.desc"));
                } else if (ModStat.isLibCircleDepending(stat)) {
                    cornerIcon(
                            Icon.refreshSmall,
                            Color.crimson,
                            Core.bundle.get("mod-manager.stat.lib-circle.desc"));
                }
                if (ModStat.isError(stat)) {
                    cornerIcon(
                            Icon.cancelSmall,
                            Color.crimson,
                            Core.bundle.get("mod-manager.stat.error.desc"));
                }
                if (ModStat.isBlackListed(stat)) {
                    cornerIcon(
                            Icon.infoCircleSmall,
                            Color.crimson,
                            Core.bundle.get("mod-manager.stat.blacklist.desc"));
                }
            }
        });
    }

    public static void renderDetailedStatusRows(
            int stat,
            @Nullable String updateVersion,
            boolean isRemote,
            boolean isInstalled) {
        column().growX().gap(unit(0.8f)).left().children(() -> {
            if (isRemote) {
                if (isInstalled) {
                    if (updateVersion != null && !updateVersion.isEmpty()) {
                        statusLine(
                                Icon.starSmall,
                                Core.bundle.format("mod-manager.stat.update-available", updateVersion),
                                LIGHT_BLUE);
                    } else {
                        statusLine(
                                Icon.okSmall,
                                Core.bundle.get("mod-browser.badge.installed"),
                                Pal.heal);
                    }
                }
            } else {
                if (updateVersion != null && !updateVersion.isEmpty()) {
                    statusLine(
                            Icon.upSmall,
                            Core.bundle.format("mod-manager.stat.update-available", updateVersion),
                            LIGHT_BLUE);
                }
                if (ModStat.isLocalFile(stat)) {
                    statusLine(
                            Icon.fileSmall,
                            Core.bundle.get("mod-manager.stat.local.desc"),
                            Color.white);
                }
                if (ModStat.isValid(stat)) {
                    statusLine(
                            Icon.okSmall,
                            Core.bundle.get("mod-manager.stat.up-to-date.desc"),
                            Pal.heal);
                } else {
                    statusLine(
                            Icon.cancelSmall,
                            Core.bundle.get("mod-manager.stat.error.desc"),
                            Color.crimson);
                }
            }

            if (ModStat.isJAR(stat)) {
                statusLine(
                        Icon.terminal,
                        Core.bundle.get("mod-manager.stat.jar.desc"),
                        Pal.reactorPurple);
            }
            if (ModStat.isJS(stat)) {
                statusLineJs(Core.bundle.get("mod-manager.stat.js.desc"), Pal.accent);
            }
            if (ModStat.isClientOnly(stat)) {
                statusLine(
                        Icon.hostSmall,
                        Core.bundle.get("mod-manager.stat.client-only.desc"),
                        Pal.techBlue);
            }

            if (ModStat.isDeprecated(stat)) {
                int minVer = ModStat.isJAR(stat) ? Vars.minJavaModGameVersion : Vars.minModGameVersion;
                statusLine(
                        Icon.warningSmall,
                        Core.bundle.format("mod-manager.stat.deprecated.detail", minVer),
                        Color.crimson);
            } else if (ModStat.isUnsupported(stat)) {
                statusLine(
                        Icon.warningSmall,
                        Core.bundle.get("mod-manager.stat.unsupported.desc"),
                        Color.crimson);
            }

            if (ModStat.isLibMissing(stat)) {
                statusLine(
                        Icon.layersSmall,
                        Core.bundle.get("mod-manager.stat.lib-missing.desc"),
                        Color.crimson);
            } else if (ModStat.isLibIncomplete(stat)) {
                statusLine(
                        Icon.warningSmall,
                        Core.bundle.get("mod-manager.stat.lib-incomplete.desc"),
                        Color.crimson);
            } else if (ModStat.isLibCircleDepending(stat)) {
                statusLine(
                        Icon.refreshSmall,
                        Core.bundle.get("mod-manager.stat.lib-circle.desc"),
                        Color.crimson);
            }

            if (ModStat.isError(stat)) {
                statusLine(
                        Icon.cancelSmall,
                        Core.bundle.get("mod-manager.stat.error.desc"),
                        Color.crimson);
            }
            if (ModStat.isBlackListed(stat)) {
                statusLine(
                        Icon.infoCircleSmall,
                        Core.bundle.get("mod-manager.stat.blacklist.desc"),
                        Color.crimson);
            }
        });
    }

    public static void statusChip(String labelText, Color color, @Nullable String tooltipText) {
        Color bg = new Color(color.r * 0.22f, color.g * 0.22f, color.b * 0.22f, 0.85f);
        Card chip = card()
                .rounded(unit(2), bg)
                .border(1f, color)
                .padding(unit(0.4f), unit(1.2f), unit(0.4f), unit(1.2f));
        if (tooltipText != null && !tooltipText.isEmpty()) {
            chip.tooltip(tooltipText);
        }
        chip.children(() -> text(labelText).color(color).fontScale(0.75f));
    }

    private static void cornerIcon(Drawable drawable, Color color, String tooltipText) {
        icon(drawable)
                .size(unit(6f))
                .color(color)
                .tooltip(tooltipText);
    }

    private static void jsBadge(@Nullable String tooltipText) {
        Card box = card()
                .rounded(unit(1), Pal.accent)
                .size(unit(5.5f))
                .center();
        if (tooltipText != null && !tooltipText.isEmpty()) {
            box.tooltip(tooltipText);
        }
        box.children(() -> text("JS").color(Color.black).fontScale(0.68f));
    }

    private static void statusLine(Drawable drawable, String description, Color color) {
        row().gap(unit(1.5f)).left().children(() -> {
            icon(drawable).size(unit(5.5f)).color(color);
            text(description).color(color).fontScale(0.85f).wrap();
        });
    }

    private static void statusLineJs(String description, Color color) {
        row().gap(unit(1.5f)).left().children(() -> {
            jsBadge(null);
            text(description).color(color).fontScale(0.85f).wrap();
        });
    }
}
