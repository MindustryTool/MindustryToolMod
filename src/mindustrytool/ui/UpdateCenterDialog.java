package mindustrytool.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Premium Update Center Dialog
 * Features:
 * - Version list with all releases
 * - Changelog display
 * - One-click update
 * - Rollback capability
 * - Download count
 * - Color-coded update types
 */
public class UpdateCenterDialog extends BaseDialog {

    private static final String RELEASES_API = "https://api.github.com/repos/MindustryTool/MindustryToolMod/releases";
    private static final String REPO_URL = "MindustryTool/MindustryToolMod";

    private final Seq<ReleaseInfo> releases = new Seq<>();
    private ReleaseInfo selectedRelease = null;
    private ReleaseInfo currentRelease = null;
    private final mindustrytool.utils.Version currentVersion;

    private Table versionListTable;
    private Table changelogTable;
    private TextButton updateButton;

    private boolean loading = true;
    private String errorMessage = null;

    // Filter state
    private enum Filter {
        ALL, STABLE, BETA
    }

    private Filter currentFilter = Filter.ALL;

    public UpdateCenterDialog() {
        super("Update Center");

        // Get current version
        var mod = Vars.mods.getMod(mindustrytool.Main.class);
        currentVersion = new mindustrytool.utils.Version(mod != null ? mod.meta.version : "0.0.0");

        // Setup dialog
        addCloseButton();
        shown(this::onShown);

        // Build initial loading UI
        rebuildUI();
    }

    private void onShown() {
        // Reset state
        loading = true;
        errorMessage = null;
        releases.clear();
        selectedRelease = null;
        currentRelease = null;

        rebuildUI();
        fetchReleases();
    }

    private void fetchReleases() {
        Http.get(RELEASES_API, response -> {
            try {
                Jval array = Jval.read(response.getResultAsString());
                if (array.isArray()) {
                    for (Jval item : array.asArray()) {
                        ReleaseInfo info = new ReleaseInfo(item);
                        if (!info.draft) { // Skip draft releases
                            releases.add(info);
                        }
                    }
                }

                // Find current release
                for (ReleaseInfo r : releases) {
                    if (r.getVersion().equals(currentVersion)) {
                        currentRelease = r;
                        break;
                    }
                }

                // Auto-select latest if no current found or there's a newer version
                if (!releases.isEmpty()) {
                    selectedRelease = releases.first(); // First is latest
                }

                loading = false;
                Core.app.post(this::rebuildUI);

            } catch (Exception e) {
                Log.err("Failed to parse releases", e);
                errorMessage = "Failed to parse release data: " + e.getMessage();
                loading = false;
                Core.app.post(this::rebuildUI);
            }
        }, error -> {
            Log.err("Failed to fetch releases", error);
            errorMessage = "Network error: " + error.getMessage();
            loading = false;
            Core.app.post(this::rebuildUI);
        });
    }

    private void rebuildUI() {
        cont.clear();
        buttons.clear();

        if (loading) {
            buildLoadingUI();
        } else if (errorMessage != null) {
            buildErrorUI();
        } else {
            buildMainUI();
        }

        // Standard buttons
        buttons.defaults().size(160f, 55f).pad(8f);
        buttons.button("@cancel", Icon.cancel, this::hide);
        buttons.button("GitHub", Icon.github, () -> {
            Core.app.openURI("https://github.com/" + REPO_URL + "/releases");
        });

        if (!loading && errorMessage == null && selectedRelease != null) {
            updateButton = buttons.button(getUpdateButtonText(), Icon.download, this::performUpdate)
                    .color(getUpdateButtonColor()).get();
        }
    }

    private void buildLoadingUI() {
        cont.add("Loading releases...").color(Color.lightGray).pad(50f);
        // TODO: Add spinner animation
    }

    private void buildErrorUI() {
        cont.table(t -> {
            t.image(Icon.warning).size(48f).color(Pal.remove).padBottom(10f).row();
            t.add("Failed to load updates").color(Pal.remove).padBottom(5f).row();
            t.add(errorMessage).color(Color.gray).wrap().width(400f).padBottom(15f).row();
            t.button("Retry", Icon.refresh, Styles.flatt, () -> {
                loading = true;
                errorMessage = null;
                rebuildUI();
                fetchReleases();
            }).size(120f, 40f);
        }).pad(30f);
    }

    private void buildMainUI() {
        cont.defaults().pad(5f);

        // Header: Current vs Latest
        cont.table(this::buildHeader).growX().padBottom(10f).row();

        // Separator
        cont.image().height(2f).color(Color.darkGray).growX().padBottom(5f).row();

        // Main content: Version list + Changelog
        cont.table(main -> {
            main.defaults().top();

            // Left: Version List
            main.table(left -> {
                // Filter tabs
                left.table(tabs -> {
                    tabs.defaults().size(70f, 30f);
                    tabs.button("All", Styles.flatTogglet, () -> setFilter(Filter.ALL))
                            .checked(currentFilter == Filter.ALL);
                    tabs.button("Stable", Styles.flatTogglet, () -> setFilter(Filter.STABLE))
                            .checked(currentFilter == Filter.STABLE);
                    tabs.button("Beta", Styles.flatTogglet, () -> setFilter(Filter.BETA))
                            .checked(currentFilter == Filter.BETA);
                }).padBottom(5f).row();

                // Version list
                versionListTable = new Table();
                ScrollPane scroll = new ScrollPane(versionListTable, Styles.smallPane);
                scroll.setScrollingDisabled(true, false);
                left.add(scroll).size(280f, 300f);

                buildVersionList();
            }).padRight(10f);

            // Right: Changelog
            main.table(right -> {
                right.add("What's New").color(Pal.accent).left().padBottom(5f).row();
                right.image().height(1f).color(Color.darkGray).growX().padBottom(5f).row();

                changelogTable = new Table();
                ScrollPane scroll = new ScrollPane(changelogTable, Styles.smallPane);
                scroll.setScrollingDisabled(true, false);
                right.add(scroll).size(300f, 320f);

                buildChangelog();
            });
        }).grow().row();
    }

    private void buildHeader(Table t) {
        t.defaults().pad(10f);

        // Current version
        t.table(curr -> {
            curr.add("Current").color(Color.gray).row();
            curr.add(currentVersion.toString()).fontScale(1.3f)
                    .color(currentRelease != null ? Pal.heal : Color.lightGray);
        }).expandX();

        // Arrow
        t.image(Icon.rightOpen).size(30f).color(Color.gray);

        // Latest version
        ReleaseInfo latest = releases.isEmpty() ? null : releases.first();
        t.table(lat -> {
            lat.add("Latest").color(Color.gray).row();
            if (latest != null) {
                Color c = getVersionColor(latest);
                lat.add(latest.tagName).fontScale(1.3f).color(c);
                if (latest.prerelease) {
                    lat.add(" BETA").color(Color.orange).fontScale(0.8f);
                }
            } else {
                lat.add("Unknown").fontScale(1.3f).color(Color.gray);
            }
        }).expandX();
    }

    private void buildVersionList() {
        versionListTable.clear();
        versionListTable.defaults().growX().pad(2f);

        Seq<ReleaseInfo> filtered = getFilteredReleases();

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        for (ReleaseInfo release : filtered) {
            versionListTable.table(row -> {
                buildVersionRow(row, release, group);
            }).row();
        }

        if (filtered.isEmpty()) {
            versionListTable.add("No releases found").color(Color.gray).pad(20f);
        }
    }

    private void buildVersionRow(Table row, ReleaseInfo release, ButtonGroup<TextButton> group) {
        boolean isCurrent = release.getVersion().equals(currentVersion);
        boolean isLatest = releases.indexOf(release) == 0;
        boolean isSelected = release == selectedRelease;
        Color versionColor = getVersionColor(release);

        // Radio button style selection
        TextButton btn = new TextButton("", Styles.flatTogglet);
        btn.clicked(() -> {
            selectedRelease = release;
            buildChangelog();
            if (updateButton != null) {
                updateButton.setText(getUpdateButtonText());
                updateButton.setColor(getUpdateButtonColor());
            }
        });
        btn.setChecked(isSelected);
        group.add(btn);

        btn.table(inner -> {
            inner.defaults().left();

            // Version tag
            inner.add(release.tagName).color(versionColor).fontScale(1.1f).width(90f);

            // Tags
            inner.table(tags -> {
                if (isLatest && !isCurrent) {
                    tags.add("[NEW]").color(Pal.accent).fontScale(0.7f).padRight(3f);
                }
                if (isCurrent) {
                    tags.add("[YOU]").color(Pal.heal).fontScale(0.7f).padRight(3f);
                }
                if (release.prerelease) {
                    tags.add("[BETA]").color(Color.orange).fontScale(0.7f);
                }
            }).width(80f);

            // Date
            inner.add(release.getRelativeDate()).color(Color.gray).fontScale(0.8f).width(70f);

            // Download count
            inner.table(dl -> {
                dl.image(Icon.download).size(12f).color(Color.gray).padRight(2f);
                dl.add(release.getFormattedDownloads()).color(Color.gray).fontScale(0.8f);
            }).width(50f);
        }).growX().pad(5f);

        row.add(btn).growX();
    }

    private void buildChangelog() {
        changelogTable.clear();
        changelogTable.defaults().left().padBottom(3f);

        if (selectedRelease == null) {
            changelogTable.add("Select a version to see changelog").color(Color.gray);
            return;
        }

        // Title
        changelogTable.add(selectedRelease.name).color(Pal.accent).fontScale(1.1f).row();
        changelogTable.add(selectedRelease.getRelativeDate()).color(Color.gray).fontScale(0.9f).padBottom(10f).row();

        // Changelog lines
        for (String line : selectedRelease.getChangelogLines()) {
            changelogTable.add(line).wrap().width(280f).color(Color.lightGray).row();
        }
    }

    private void setFilter(Filter filter) {
        currentFilter = filter;
        buildVersionList();
    }

    private Seq<ReleaseInfo> getFilteredReleases() {
        switch (currentFilter) {
            case STABLE:
                return releases.select(r -> !r.prerelease);
            case BETA:
                return releases.select(r -> r.prerelease);
            default:
                return releases;
        }
    }

    private Color getVersionColor(ReleaseInfo release) {
        if (release.getVersion().equals(currentVersion)) {
            return Pal.heal; // Green for current
        }

        mindustrytool.utils.Version v = release.getVersion();
        if (v.major > currentVersion.major) {
            return Pal.remove; // Red for major
        } else if (v.minor > currentVersion.minor) {
            return Color.gold; // Gold for feature
        } else if (release.prerelease) {
            return Color.orange; // Orange for beta
        }
        return Color.lightGray;
    }

    private String getUpdateButtonText() {
        if (selectedRelease == null)
            return "Update";

        boolean isCurrent = selectedRelease.getVersion().equals(currentVersion);
        boolean isNewer = selectedRelease.getVersion().isNewerThan(currentVersion);
        boolean isOlder = currentVersion.isNewerThan(selectedRelease.getVersion());

        if (isCurrent)
            return "Re-install";
        if (isNewer)
            return "Update Now";
        if (isOlder)
            return "Rollback";
        return "Install";
    }

    private Color getUpdateButtonColor() {
        if (selectedRelease == null)
            return Color.gray;

        boolean isCurrent = selectedRelease.getVersion().equals(currentVersion);
        boolean isOlder = currentVersion.isNewerThan(selectedRelease.getVersion());

        if (isCurrent)
            return Color.gray;
        if (isOlder)
            return Color.orange; // Rollback warning
        return Pal.accent; // Normal update
    }

    private void performUpdate() {
        if (selectedRelease == null)
            return;

        hide();
        Vars.ui.mods.githubImportMod(REPO_URL, true, null);
    }

    /**
     * Static method to open the dialog - called from ToolsMenuDialog or anywhere
     * else.
     */
    public static void open() {
        new UpdateCenterDialog().show();
    }

    /**
     * Check for updates silently (for auto-check on startup).
     * Only shows dialog if update is available.
     */
    public static void checkSilent() {
        var mod = Vars.mods.getMod(mindustrytool.Main.class);
        if (mod == null)
            return;

        mindustrytool.utils.Version current = new mindustrytool.utils.Version(mod.meta.version);

        Http.get(RELEASES_API, response -> {
            try {
                Jval array = Jval.read(response.getResultAsString());
                if (array.isArray() && array.asArray().size > 0) {
                    ReleaseInfo latest = new ReleaseInfo(array.asArray().first());
                    if (latest.getVersion().isNewerThan(current)) {
                        Log.info("Update available: @ -> @", current, latest.tagName);
                        Core.app.post(() -> new UpdateCenterDialog().show());
                    } else {
                        Log.info("Mod is up to date (@)", current);
                    }
                }
            } catch (Exception e) {
                Log.err("Silent update check failed", e);
            }
        }, error -> {
            Log.err("Silent update check failed", error);
        });
    }
}
