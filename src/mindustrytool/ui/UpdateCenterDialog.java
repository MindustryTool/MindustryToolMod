package mindustrytool.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Icon;
import arc.struct.ObjectMap;
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
    private static final String COMMITS_API = "https://api.github.com/repos/MindustryTool/MindustryToolMod/commits";
    private static final String BRANCHES_API = "https://api.github.com/repos/MindustryTool/MindustryToolMod/branches";
    private static final String REPO_URL = "MindustryTool/MindustryToolMod";

    private final Seq<ReleaseInfo> releases = new Seq<>();
    private final Seq<CommitInfo> commits = new Seq<>();
    private ReleaseInfo selectedRelease = null;
    private ReleaseInfo currentRelease = null;
    private final mindustrytool.utils.Version currentVersion;

    // Needed fields
    private TextButton updateButton;
    private boolean loading = true;
    private String errorMessage = null;

    // Filter state
    private final ObjectSet<mindustrytool.utils.Version.SuffixType> enabledFilters = new ObjectSet<>();
    private final ObjectSet<String> activeBranches = new ObjectSet<>();
    private boolean showArchived = false;

    public UpdateCenterDialog() {
        super("Update Center");

        // Load saved filters
        String savedFilters = Core.settings.getString("mindustrytool-filters", "STABLE");
        String[] parts = savedFilters.split(",");
        for (String part : parts) {
            try {
                if (!part.isEmpty()) {
                    enabledFilters.add(mindustrytool.utils.Version.SuffixType.valueOf(part));
                }
            } catch (Exception e) {
                // Ignore invalid values
            }
        }
        // Ensure at least STABLE is present if empty (fallback)
        if (enabledFilters.isEmpty()) {
            enabledFilters.add(mindustrytool.utils.Version.SuffixType.STABLE);
        }

        // Get current version
        mindustry.mod.Mods.LoadedMod mod = Vars.mods.getMod(mindustrytool.Main.class);
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
        commits.clear();
        activeBranches.clear();
        selectedRelease = null;
        currentRelease = null;

        rebuildUI();
        fetchReleases();
        fetchCommits();
        fetchBranches();
    }

    private void fetchCommits() {
        if (cachedCommits != null && System.currentTimeMillis() - lastCommitFetch < CACHE_DURATION) {
            commits.clear();
            commits.addAll(cachedCommits);
            // Ensure UI shows cached commits if already loaded
            if (!loading)
                Core.app.post(this::rebuildUI);
            return;
        }

        Http.get(COMMITS_API)
                .header("User-Agent", "MindustryToolMod")
                .error(e -> handleError(e))
                .submit(response -> {
                    try {
                        Jval array = Jval.read(response.getResultAsString());
                        if (array.isArray()) {
                            Seq<CommitInfo> newCommits = new Seq<>();
                            for (Jval item : array.asArray()) {
                                newCommits.add(new CommitInfo(item));
                            }
                            cachedCommits = newCommits;
                            lastCommitFetch = System.currentTimeMillis();

                            commits.clear();
                            commits.addAll(newCommits);
                        }

                        // Always trigger rebuild to show the new commits
                        if (!loading) {
                            Core.app.post(this::rebuildUI);
                        }
                    } catch (Exception e) {
                        Log.err("Failed to parse commits", e);
                    }
                });
    }

    private void fetchBranches() {
        if (cachedBranches != null && System.currentTimeMillis() - lastBranchFetch < CACHE_DURATION) {
            activeBranches.clear();
            activeBranches.addAll(cachedBranches);
            // If already loaded (e.g. from cache), we might need to trigger rebuild if
            // releases are ready
            // But usually rebuildUI is triggered by release loading finishing.
            // If we are just refreshing branches, we might want to rebuild.
            if (!loading)
                Core.app.post(this::rebuildUI);
            return;
        }

        Http.get(BRANCHES_API)
                .header("User-Agent", "MindustryToolMod")
                .error(e -> handleError(e))
                .submit(response -> {
                    try {
                        Jval array = Jval.read(response.getResultAsString());
                        if (array.isArray()) {
                            ObjectSet<String> newBranches = new ObjectSet<>();
                            for (Jval item : array.asArray()) {
                                newBranches.add(item.getString("name", ""));
                            }
                            cachedBranches = newBranches;
                            lastBranchFetch = System.currentTimeMillis();

                            activeBranches.clear();
                            activeBranches.addAll(newBranches);
                        }
                        if (!loading) {
                            Core.app.post(this::rebuildUI);
                        }
                    } catch (Exception e) {
                        Log.err("Failed to parse branches", e);
                    }
                });
    }

    // Cache
    private static Seq<ReleaseInfo> cachedReleases;
    private static long lastReleaseFetch;
    private static Seq<CommitInfo> cachedCommits;
    private static long lastCommitFetch;
    private static ObjectSet<String> cachedBranches;
    private static ObjectMap<String, TextureRegion> avatarCache = new ObjectMap<>();
    private static long lastBranchFetch;
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10 minutes

    private void refresh() {
        cachedReleases = null;
        cachedCommits = null;
        cachedBranches = null;
        if (avatarCache != null)
            avatarCache.clear();

        lastReleaseFetch = 0;
        lastCommitFetch = 0;
        lastBranchFetch = 0;

        onShown();
    }

    private void handleError(Throwable error) {
        String msg = error.getMessage();
        if (msg != null && msg.contains("403")) {
            // Suppress stack trace for rate limit, just warn
            Log.warn("GitHub Rate Limit Exceeded: " + msg);
            errorMessage = "GitHub Rate Limit Exceeded.\nPlease wait a moment before trying again.";
        } else {
            Log.err("Network error", error);
            errorMessage = "Network error: " + (msg != null ? msg : "Unknown error");
        }
        loading = false;
        Core.app.post(this::rebuildUI);
    }

    private void fetchReleases() {
        // Check cache first
        if (cachedReleases != null && System.currentTimeMillis() - lastReleaseFetch < CACHE_DURATION) {
            releases.clear();
            releases.addAll(cachedReleases);
            finishReleaseLoading();
            return;
        }

        Http.get(RELEASES_API)
                .header("User-Agent", "MindustryToolMod")
                .error(e -> handleError(e))
                .submit(response -> {
                    try {
                        Jval array = Jval.read(response.getResultAsString());
                        if (array.isArray()) {
                            Seq<ReleaseInfo> newReleases = new Seq<>();
                            for (Jval item : array.asArray()) {
                                ReleaseInfo info = new ReleaseInfo(item);
                                if (!info.draft) { // Skip draft releases
                                    newReleases.add(info);
                                }
                            }
                            // Update cache
                            cachedReleases = newReleases;
                            lastReleaseFetch = System.currentTimeMillis();

                            releases.clear();
                            releases.addAll(newReleases);
                        }

                        finishReleaseLoading();

                    } catch (Exception e) {
                        Log.err("Failed to parse releases", e);
                        errorMessage = "Failed to parse release data: " + e.getMessage();
                        loading = false;
                        Core.app.post(this::rebuildUI);
                    }
                });
    }

    private void finishReleaseLoading() {
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
        buttons.defaults().size(210f, 64f).pad(8f);
        buttons.button("@back", Icon.left, this::hide);

        if (!loading && errorMessage == null && selectedRelease != null) {
            updateButton = buttons.button(getUpdateButtonText(), Icon.download, this::performUpdate)
                    .color(getUpdateButtonColor()).get();
        }
    }

    private void buildLoadingUI() {
        Table table = new Table();
        table.image(Icon.refresh).color(Pal.accent).size(50f).with(img -> {
            img.actions(arc.scene.actions.Actions.forever(arc.scene.actions.Actions.rotateBy(360f, 1f)));
        });
        table.row();
        table.add("Loading releases...").color(Color.lightGray).padTop(10f);

        cont.add(table).center();
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

        // Dynamic sizing logic
        float contentWidth = Core.graphics.isPortrait() ? Core.graphics.getWidth() - 60f : 600f;
        // Calculate available height for both lists
        // Screen height - padding (increased to 280f to prevent overlap)
        float totalAvailableHeight = Core.graphics.getHeight() - 280f;
        float splitHeight = Math.max(200f, totalAvailableHeight / 2f);

        float availableHeight = splitHeight;
        float timelineHeight = splitHeight;

        // Wrapper to constrain width on PC
        cont.table(main -> {
            main.top();
            main.defaults().width(contentWidth).padBottom(5f);

            // 1. Header (Removed)

            // 2. Filters (Horizontally scrollable)
            Table filterRow = new Table();
            filterRow.left();

            // Reload button
            filterRow.button(Icon.refresh, Styles.clearNonei, this::refresh).size(40f).padRight(10f);

            // filterRow.add("Filters:").color(Color.gray).padRight(10f);

            // Dynamic Filter Generation
            addFilterButton(filterRow, "Main", mindustrytool.utils.Version.SuffixType.STABLE, Pal.heal);

            ObjectSet<mindustrytool.utils.Version.SuffixType> foundTypes = new ObjectSet<>();
            for (ReleaseInfo r : releases) {
                foundTypes.add(r.getVersion().type);
            }

            if (foundTypes.contains(mindustrytool.utils.Version.SuffixType.BETA)) {
                addFilterButton(filterRow, "Beta", mindustrytool.utils.Version.SuffixType.BETA, Color.orange);
            }

            if (foundTypes.contains(mindustrytool.utils.Version.SuffixType.DEV)) {
                addFilterButton(filterRow, "Dev", mindustrytool.utils.Version.SuffixType.DEV, Color.pink);
            }

            if (foundTypes.contains(mindustrytool.utils.Version.SuffixType.FIX)) {
                addFilterButton(filterRow, "Hotfix", mindustrytool.utils.Version.SuffixType.FIX, Color.scarlet);
            }

            // Separator and Archived toggle
            filterRow.add("|").color(Color.darkGray).padLeft(10f).padRight(10f);
            filterRow.button("Archived", Styles.flatTogglet, () -> {
                showArchived = !showArchived;
                rebuildUI();
            }).checked(showArchived).size(100f, 40f).color(Color.gray);

            ScrollPane filterScroll = new ScrollPane(filterRow, Styles.smallPane);
            filterScroll.setScrollingDisabledY(true);
            filterScroll.setFadeScrollBars(false);
            main.add(filterScroll).height(50f).row();

            // 3. List
            Table items = new Table();
            ScrollPane scroll = new ScrollPane(items, Styles.smallPane);
            scroll.setFadeScrollBars(false);
            main.add(scroll).height(availableHeight).row();

            buildReleaseList(items);

            // 4. Timeline
            main.add("Recent Activity").color(Color.lightGray).left().row();
            main.image().height(2f).color(Color.darkGray).fillX().padBottom(5f).row();

            Table timeline = new Table();
            ScrollPane timeScroll = new ScrollPane(timeline, Styles.smallPane);
            timeScroll.setFadeScrollBars(false);
            main.add(timeScroll).height(timelineHeight).row();

            buildTimeline(timeline);
        }).width(contentWidth);
    }

    private void addFilterButton(Table t, String text, mindustrytool.utils.Version.SuffixType type, Color color) {
        t.button(text, Styles.flatTogglet, () -> {
            boolean isEnabled = enabledFilters.contains(type);
            if (isEnabled) {
                // Prevent disabling if it's the last one
                if (enabledFilters.size <= 1) {
                    Vars.ui.showInfoToast("Must have at least one filter enabled.", 2f);
                    return;
                }
                enabledFilters.remove(type);
            } else {
                enabledFilters.add(type);
            }

            // Save settings
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (mindustrytool.utils.Version.SuffixType s : enabledFilters) {
                sb.append(s.name());
                if (i++ < enabledFilters.size - 1)
                    sb.append(",");
            }
            Core.settings.put("mindustrytool-filters", sb.toString());

            rebuildUI();
        }).checked(enabledFilters.contains(type)).size(120f, 40f).color(color).padRight(5f);
    }

    private void buildReleaseList(Table table) {
        table.top().left();
        table.defaults().growX().padBottom(4f);

        // Filter releases based on enabled set and archive status
        Seq<ReleaseInfo> targetList = releases.select(r -> {
            if (!enabledFilters.contains(r.getVersion().type))
                return false;

            // Only check for archived status if we actually know what the active branches
            // are.
            // If activeBranches is empty (loading failed or not yet loaded), assume it's
            // NOT archived.
            boolean isArchived = !activeBranches.isEmpty() && !r.targetBranch.isEmpty()
                    && !activeBranches.contains(r.targetBranch);

            if (isArchived && !showArchived)
                return false;
            return true;
        });

        if (targetList.isEmpty()) {
            table.add("No releases found matching filters.").color(Color.gray).pad(20f);
            if (enabledFilters.isEmpty()) {
                table.row();
                table.add("(Enable a filter above to see releases)").color(Color.lightGray).fontScale(0.8f);
            }
            return;
        }

        for (ReleaseInfo r : targetList) {
            // Re-calc archived status for display
            boolean isArchived = !activeBranches.isEmpty() && !r.targetBranch.isEmpty()
                    && !activeBranches.contains(r.targetBranch);
            boolean isSelected = selectedRelease == r;
            Color textColor = isArchived ? Color.darkGray : getVersionColor(r);

            table.table(row -> {
                row.left();
                row.setBackground(isSelected ? Styles.flatDown : (Drawable) null);

                // Combined button for Date + Tag (Clickable Row effect)
                row.button(b -> {
                    b.left();
                    // 1. Date (Relative Time)
                    String dateText = r.getRelativeDate();
                    if (isArchived) {
                        dateText += " [gray](A)[]";
                    }
                    b.add(dateText).color(Color.gray).fontScale(0.8f).width(100f).padRight(10f).padLeft(10f);

                    // Critical Update Indicator
                    boolean isCritical = r.getVersion().major > currentVersion.major;
                    if (isCritical) {
                        b.image(Icon.warning).color(Pal.remove).size(20f).padRight(5f);
                    }

                    // 2. Tag
                    b.add(r.tagName).color(textColor).growX().left();
                }, Styles.cleart, () -> {
                    selectedRelease = r;
                    rebuildUI();
                }).growX().height(40f).padRight(5f);

                // 3. Info Icon (only if body has meaningful content)
                if (hasReleaseNotes(r)) {
                    row.button(Icon.info, Styles.clearNonei, () -> {
                        showReleaseNotesDialog(r);
                    }).size(30f).padLeft(0f);
                }
            }).growX().height(40f).padBottom(4f).row();
        }
    }

    private void showReleaseNotesDialog(ReleaseInfo r) {
        BaseDialog dialog = new BaseDialog(r.name);

        // Responsive width: On mobile portrait, use nearly full width; on desktop, cap
        // at 500
        float dialogWidth = Core.graphics.isPortrait()
                ? Core.graphics.getWidth() - 80f
                : Math.min(500f, Core.graphics.getWidth() * 0.6f);
        float contentWidth = dialogWidth - 40f;

        ScrollPane pane = dialog.cont.pane(p -> {
            p.top().left();
            p.defaults().width(contentWidth).left();
            MarkdownRenderer renderer = new MarkdownRenderer().setContentWidth(contentWidth);
            renderer.render(p, r.body);
        }).width(dialogWidth).grow().pad(10f).get();

        // Disable horizontal scroll to force content wrapping
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);

        // Custom buttons: Back and Install
        dialog.buttons.defaults().size(150f, 55f).pad(8f);
        dialog.buttons.button("@back", Icon.left, dialog::hide);
        dialog.buttons.button("Install", Icon.download, () -> {
            dialog.hide();
            selectedRelease = r;
            performUpdate();
        }).color(Pal.accent);

        dialog.show();
    }

    private boolean hasReleaseNotes(ReleaseInfo r) {
        if (r.body == null)
            return false;
        String cleaned = r.body.trim();
        // Check for truly empty or just common placeholder text
        if (cleaned.isEmpty())
            return false;
        if (cleaned.equals("No changelog available."))
            return false;
        if (cleaned.equals("No description provided."))
            return false;
        if (cleaned.contains("No changes found"))
            return false;
        // Must have at least some meaningful characters (more than 5)
        return cleaned.length() > 5;
    }

    private void buildTimeline(Table t) {
        t.top().left();
        t.defaults().growX().padBottom(0f); // Tight spacing for connected lines

        if (commits.isEmpty()) {
            t.add("Loading commits...").color(Color.gray).pad(20f);
            return;
        }

        for (int i = 0; i < commits.size; i++) {
            CommitInfo c = commits.get(i);
            boolean isLast = i == commits.size - 1;

            t.table(row -> {
                row.left();

                // Timeline Graphic
                row.table(line -> {
                    line.top();
                    // Dot
                    line.image(Icon.add).color(Pal.accent).size(14f).padTop(4f).row();
                    // Vertical line (if not last)
                    if (!isLast) {
                        line.image().color(Color.darkGray).width(2f).growY().padTop(-2f).padBottom(-2f);
                    }
                }).width(20f).growY().padRight(10f);

                // Content
                row.table(content -> {
                    content.left().defaults().left();
                    content.add(c.message).color(Color.white).wrap().width(500f).row();
                    content.table(meta -> {
                        // Avatar
                        meta.table(avatar -> {
                            renderAvatar(avatar, c.avatarUrl, 16f);
                        }).size(16f).padRight(6f);

                        // meta.image(Icon.admin).size(10f).color(Color.gray).padRight(3f);
                        meta.add(c.authorName).color(Pal.accent).fontScale(0.8f).padRight(10f);
                        meta.add(c.date.replace("T", " ").replace("Z", "")).color(Color.lightGray).fontScale(0.7f);
                    }).padTop(2f);
                }).growX().padBottom(15f); // Spacing between items

                // Action button (View on GitHub)
                row.button(Icon.link, Styles.clearNonei, () -> {
                    if (!c.htmlUrl.isEmpty())
                        Core.app.openURI(c.htmlUrl);
                }).size(30f).right().top();

            }).padLeft(10f).growX().row();
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

    private void renderAvatar(Table container, String url, float size) {
        if (url == null || url.isEmpty()) {
            container.image(Icon.admin).size(size).color(Color.gray);
            return;
        }

        if (avatarCache.containsKey(url)) {
            container.image(avatarCache.get(url)).size(size);
            return;
        }

        // Placeholder
        container.image(Icon.admin).size(size).color(Color.darkGray);

        Http.get(url)
                // .header("User-Agent", "MindustryToolMod") // Avatar URLs get public access
                .error(e -> {
                    // Fail silently or verify connection
                })
                .submit(response -> {
                    try {
                        byte[] bytes = response.getResult();
                        Core.app.post(() -> {
                            try {
                                Pixmap pixmap = new Pixmap(bytes);
                                Texture texture = new Texture(pixmap);
                                texture.setFilter(Texture.TextureFilter.linear);
                                TextureRegion region = new TextureRegion(texture);
                                pixmap.dispose();

                                avatarCache.put(url, region);

                                container.clearChildren();
                                container.image(region).size(size);
                            } catch (Exception e) {
                                // ignore
                            }
                        });
                    } catch (Exception e) {
                        // ignore
                    }
                });
    }

    private String getUpdateButtonText() {
        if (selectedRelease == null)
            return "Update";

        boolean isCurrent = selectedRelease.getVersion().equals(currentVersion);
        boolean isNewer = selectedRelease.getVersion().isNewerThan(currentVersion);
        boolean isOlder = currentVersion.isNewerThan(selectedRelease.getVersion());

        if (isCurrent)
            return "Re-install";
        if (isNewer) {
            // Check for mandatory update
            if (selectedRelease.getVersion().major > currentVersion.major) {
                return "CRITICAL UPDATE";
            }
            return "Update Now";
        }
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

        // Critical update (Major version)
        if (selectedRelease.getVersion().major > currentVersion.major) {
            return Pal.remove; // Red
        }

        return Pal.accent; // Normal update
    }

    private void performUpdate() {
        if (selectedRelease == null)
            return;

        // Check for mandatory update (Major version change)
        if (selectedRelease.getVersion().major > currentVersion.major) {
            BaseDialog confirm = new BaseDialog("Critical Update");
            confirm.cont.add("This is a major update (" + selectedRelease.tagName + ").").row();
            confirm.cont.add("It may contain breaking changes or new features.").row();
            confirm.cont.add("You must update to continue using online features.").color(Pal.remove).row();

            confirm.buttons.defaults().size(120f, 50f).pad(10f);
            confirm.buttons.button("@cancel", Icon.cancel, confirm::hide);
            confirm.buttons.button("@ok", Icon.ok, () -> {
                confirm.hide();
                hide();
                startUpdateProcess();
            }).color(Pal.accent);

            confirm.show();
        } else {
            hide();
            startUpdateProcess();
        }
    }

    private void startUpdateProcess() {
        if (selectedRelease != null && !selectedRelease.jarDownloadUrl.isEmpty()) {
            downloadDirectly(selectedRelease.jarDownloadUrl);
        } else {
            Vars.ui.mods.githubImportMod(REPO_URL, true, null);
        }
    }

    private void downloadDirectly(String url) {
        BaseDialog dialog = new BaseDialog("Downloading");
        dialog.cont.add("Downloading update...").pad(20f).row();
        // dialog.cont.image(Icon.refresh).color(Pal.accent).update(i ->
        // i.setOrigin(Align.center));
        // Simple progress bar
        mindustry.ui.Bar bar = new mindustry.ui.Bar();
        dialog.cont.add(bar).width(400f).height(30f).pad(10f);
        dialog.buttons.button("@cancel", null).size(0f); // Hide default handle
        dialog.buttons.button("@cancel", dialog::hide).size(120f, 50f);
        dialog.show();

        // Use Http directly to file
        arc.files.Fi tmp = arc.Core.files.cache("mod-update-" + System.currentTimeMillis() + ".jar");

        Http.get(url)
                .error(e -> {
                    dialog.hide();
                    Vars.ui.showException("Download Failed", e);
                })
                // .block() // remove blocked call as it breaks fluent interface if handled
                // incorrectly with return types
                .submit(response -> {
                    try {
                        byte[] data = response.getResult();
                        // Update bar? We have no progress callback in standard Http.get without content
                        // length hack
                        // Just set to 1 after done.

                        tmp.writeBytes(data);

                        dialog.hide();

                        // Import logic similar to ModsDialog.importMod
                        Core.app.post(() -> {
                            try {
                                mindustry.mod.Mods.LoadedMod mod = Vars.mods.importMod(tmp);
                                if (mod != null) {
                                    tmp.delete();

                                    // Prompt restart
                                    Vars.ui.showInfoOnHidden("@mods.reloadexit", () -> {
                                        Core.app.exit();
                                    });
                                } else {
                                    Vars.ui.showErrorMessage("Failed to import mod.");
                                }
                            } catch (Exception e) {
                                Vars.ui.showException("Import Failed", e);
                            }
                        });
                    } catch (Exception e) {
                        Core.app.post(() -> {
                            dialog.hide();
                            Vars.ui.showException("Write Failed", e);
                        });
                    }
                });
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
        mindustry.mod.Mods.LoadedMod mod = Vars.mods.getMod(mindustrytool.Main.class);
        if (mod == null)
            return;

        mindustrytool.utils.Version current = new mindustrytool.utils.Version(mod.meta.version);

        Http.get(RELEASES_API, response -> {
            try {
                Jval array = Jval.read(response.getResultAsString());
                if (array.isArray() && array.asArray().size > 0) {
                    // Iterate to find the latest STABLE release
                    ReleaseInfo latestStable = null;
                    for (Jval val : array.asArray()) {
                        ReleaseInfo info = new ReleaseInfo(val);
                        if (info.getVersion().type == mindustrytool.utils.Version.SuffixType.STABLE) {
                            latestStable = info;
                            break;
                        }
                    }

                    if (latestStable != null && latestStable.getVersion().isNewerThan(current)) {
                        Log.info("Stable update available: @ -> @", current, latestStable.tagName);
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
