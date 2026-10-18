package mindustrytool.services.update;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.padding;

import arc.Core;
import arc.graphics.Color;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import mindustry.Vars;
import mindustrytool.Config;
import solim.modifier.Values;
import solim.overlay.SolimDialog;

public class UpdateDialog extends SolimDialog {

    private final @Nullable String releaseTag;

    public UpdateDialog(String currentVer, String latestVer, String changelog, Runnable done) {
        this(currentVer, latestVer, null, changelog, null, done);
    }

    public UpdateDialog(String currentVer, String latestVer, String changelog, @Nullable String releaseTag,
            Runnable done) {
        this(currentVer, latestVer, null, changelog, releaseTag, done);
    }

    public UpdateDialog(
            String currentVer,
            String latestVer,
            @Nullable ChangelogFormatter.PartitionedReleases partitioned,
            @Nullable String fallbackChangelog,
            @Nullable String releaseTag,
            Runnable done) {
        super(Core.bundle.get("update.dialog.title"));

        this.releaseTag = releaseTag;

        name("updateAvailableDialog");
        closeOnBack();

        String newVersionText = Core.bundle.format(
                "update.message.new-version",
                "[#" + Color.crimson.toString() + "]" + currentVer,
                "[#" + Color.green.toString() + "]" + latestVer);

        children(() -> {
            column().props(growX(), Values.maxWidth(800f), gap(unit(2)), Values.left()).children(() -> {
                text(newVersionText)
                        .props(Values.wrap());

                button("Discord: " + Config.DISCORD_INVITE_URL, () -> Core.app.openURI(Config.DISCORD_INVITE_URL))
                        .props(Values.color(Color.royal), Values.left());

                divider();

                scroll().props(maxHeight(800f), Values.maxWidth(800f), growX(), solim.modifier.Values.scrollX(false)).children(() -> {
                    column().props(growX(), gap(unit(2)), Values.left()).children(() -> {
                        if (partitioned != null && partitioned.targetRelease != null) {
                            card().props(growX(), padding(unit(2))).children(() -> {
                                text(ChangelogFormatter.formatTargetRelease(partitioned.targetRelease))
                                        .props(Values.wrap(), Values.left());
                            });

                            if (!partitioned.otherReleases.isEmpty()) {
                                divider();
                                text("[accent]" + Core.bundle.get("update.changelog.other-releases") + "[white]")
                                        .props(Values.left());
                                text(ChangelogFormatter.formatReleases(partitioned.otherReleases))
                                        .props(Values.wrap(), Values.left());
                            }
                        } else {
                            String textToShow = fallbackChangelog != null
                                    ? fallbackChangelog
                                    : (partitioned != null
                                            ? ChangelogFormatter.formatReleases(partitioned.otherReleases)
                                            : "");
                            text(textToShow != null ? textToShow : "")
                                    .props(Values.wrap(), Values.left());
                        }
                    });
                });
            });
        });

        String cancelLabel = Core.bundle.get("update.button.cancel");
        String updateLabel = Core.bundle.get("update.button.update");

        actionButton(cancelLabel, () -> {
            hide();
            done.run();
        });

        actionButton(updateLabel, () -> {
            try {
                hide();
                Vars.ui.mods.show();
                String repoUrl = Config.ORG_NAME + "/" + Config.REPO_NAME;
                if (releaseTag != null && !releaseTag.trim().isEmpty()) {
                    String tagParam = releaseTag.startsWith("tags/") || releaseTag.matches("\\d+") ? releaseTag
                            : "tags/" + releaseTag;
                    Vars.ui.mods.githubImportMod(repoUrl, true, tagParam, true);
                } else {
                    Vars.ui.mods.githubImportMod(repoUrl, true, true);
                }
                Vars.ui.mods.toFront();
                Timer.schedule(() -> Vars.ui.loadfrag.toFront(), 0.2f);
            } catch (Throwable e) {
                Log.err(e);
                Vars.ui.showException(e);
            }
        });
    }
}