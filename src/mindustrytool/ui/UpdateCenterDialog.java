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
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
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
    private static final String TAGS_API = "https://api.github.com/repos/MindustryTool/MindustryToolMod/tags";
    private static final String REPO_URL = "MindustryTool/MindustryToolMod";

    private final Seq<ReleaseInfo> releases = new Seq<>();
    private final Seq<CommitInfo> commits = new Seq<>();
    private final ObjectMap<String, String> tags = new ObjectMap<>(); // SHA -> TagName
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
        fetchTags();
    }

    private void fetchTags() {
        if (cachedTags != null && System.currentTimeMillis() - lastTagFetch < CACHE_DURATION) {
            tags.clear();
            tags.putAll(cachedTags);
            if (!loading)
                Core.app.post(this::rebuildUI);
            return;
        }

        Http.get(TAGS_API)
                .header("User-Agent", "MindustryToolMod")
                .error(e -> {
                    // Try to load from disk if API fails
                    if (tags.isEmpty())
                        loadTagsFromDisk();
                    handleError(e);
                })
                .submit(response -> {
                    try {
                        String json = response.getResultAsString();
                        Jval array = Jval.read(json);
                        if (array.isArray()) {
                            ObjectMap<String, String> newTags = new ObjectMap<>();
                            for (Jval item : array.asArray()) {
                                String name = item.getString("name", "");
                                Jval commit = item.get("commit");
                                if (commit != null) {
                                    String sha = commit.getString("sha", "");
                                    if (!sha.isEmpty() && !name.isEmpty()) {
                                        newTags.put(sha, name);
                                    }
                                }
                            }
                            cachedTags = newTags;
                            lastTagFetch = System.currentTimeMillis();

                            tags.clear();
                            tags.putAll(newTags);

                            // Save to disk
                            Core.settings.put("mindustrytool-tags-json", json);

                            // Rebuild UI if timeline is already visible to show tags
                            if (!loading)
                                Core.app.post(this::rebuildUI);
                        }
                    } catch (Exception e) {
                        Log.err("Failed to parse tags", e);
                    }
                });
    }

    private void loadTagsFromDisk() {
        String json = Core.settings.getString("mindustrytool-tags-json", null);
        if (json == null)
            return;
        try {
            Jval array = Jval.read(json);
            if (array.isArray()) {
                for (Jval item : array.asArray()) {
                    String name = item.getString("name", "");
                    Jval commit = item.get("commit");
                    if (commit != null) {
                        String sha = commit.getString("sha", "");
                        if (!sha.isEmpty() && !name.isEmpty()) {
                            tags.put(sha, name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.err("Failed to load tags cache", e);
        }
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
                .error(e -> {
                    if (commits.isEmpty())
                        loadCommitsFromDisk();
                    handleError(e);
                })
                .submit(response -> {
                    try {
                        String json = response.getResultAsString();
                        Jval array = Jval.read(json);
                        if (array.isArray()) {
                            Seq<CommitInfo> newCommits = new Seq<>();
                            for (Jval item : array.asArray()) {
                                newCommits.add(new CommitInfo(item));
                            }
                            cachedCommits = newCommits;
                            lastCommitFetch = System.currentTimeMillis();

                            commits.clear();
                            commits.addAll(newCommits);

                            Core.settings.put("mindustrytool-commits-json", json); // Persist
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

    private void loadCommitsFromDisk() {
        String json = Core.settings.getString("mindustrytool-commits-json", null);
        if (json == null)
            return;
        try {
            Jval array = Jval.read(json);
            if (array.isArray()) {
                for (Jval item : array.asArray()) {
                    commits.add(new CommitInfo(item));
                }
            }
        } catch (Exception e) {
            Log.err("Failed to load commits cache", e);
        }
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
    private static ObjectMap<String, String> cachedTags;
    private static long lastTagFetch;
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10 minutes

    private void refresh() {
        cachedReleases = null;
        cachedCommits = null;
        cachedBranches = null;
        cachedTags = null;
        if (avatarCache != null)
            avatarCache.clear();

        lastReleaseFetch = 0;
        lastCommitFetch = 0;
        lastBranchFetch = 0;
        lastTagFetch = 0;

        onShown();
    }

    private void handleError(Throwable error) {
        String msg = error.getMessage();

        // If we have cached data (Offline Mode), suppress the error screen
        // and just show a toast warning.
        boolean hasData = !releases.isEmpty();

        if (msg != null && msg.contains("403")) {
            // Rate Limit
            if (hasData) {
                Log.warn("GitHub Rate Limit Exceeded (Using Cache): " + msg);
                Vars.ui.showInfoToast("Offline Mode: GitHub Rate Limit Exceeded", 3f);
                errorMessage = null; // Don't block UI
            } else {
                Log.warn("GitHub Rate Limit Exceeded: " + msg);
                errorMessage = "GitHub Rate Limit Exceeded.\nPlease wait a moment before trying again.";
            }
        } else {
            // Other network error
            Log.err("Network error", error);
            if (hasData) {
                Vars.ui.showInfoToast("Network Error: Using cached data", 3f);
                errorMessage = null;
            } else {
                errorMessage = "Network error: " + (msg != null ? msg : "Unknown error");
            }
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
                .error(e -> {
                    if (releases.isEmpty())
                        loadReleasesFromDisk();
                    handleError(e);
                })
                .submit(response -> {
                    try {
                        String json = response.getResultAsString();
                        Jval array = Jval.read(json);
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

                            Core.settings.put("mindustrytool-releases-json", json); // Persist
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

    private void loadReleasesFromDisk() {
        String json = Core.settings.getString("mindustrytool-releases-json", null);
        if (json == null)
            return;
        try {
            Jval array = Jval.read(json);
            if (array.isArray()) {
                for (Jval item : array.asArray()) {
                    ReleaseInfo info = new ReleaseInfo(item);
                    if (!info.draft)
                        releases.add(info);
                }
            }
            if (!releases.isEmpty())
                finishReleaseLoading();
        } catch (Exception e) {
            Log.err("Failed to load releases cache", e);
        }
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
            calculateGraph();
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

            t.table(btns -> {
                btns.defaults().size(140f, 40f).pad(5f);
                btns.button("Retry", Icon.refresh, Styles.flatt, () -> {
                    loading = true;
                    errorMessage = null;
                    rebuildUI();
                    fetchReleases();
                });

                // Fallback: Direct download latest even without API
                btns.button("Get Latest", Icon.download, Styles.flatt, () -> {
                    hide();
                    startUpdateProcess(); // Will use fallback URL since selectedRelease is null
                }).color(Pal.accent);
            }).row();

            // Additional fallback: Open GitHub in browser
            t.table(btns2 -> {
                btns2.defaults().size(300f, 40f).pad(5f);
                btns2.button("Open GitHub Releases", Icon.link, Styles.flatt, () -> {
                    Core.app.openURI("https://github.com/" + REPO_URL + "/releases");
                }).color(Color.sky);
            });
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
        t.defaults().growX().padBottom(0f);

        if (commits.isEmpty()) {
            t.add("Loading commits...").color(Color.gray).pad(20f);
            return;
        }

        // Calculate max lanes for width
        int maxLanes = 1;
        for (GraphNode n : graphNodes.values())
            maxLanes = Math.max(maxLanes, n.lane + 1);
        float laneSpacing = 16f;
        float graphWidth = Math.max(24f, maxLanes * laneSpacing + 10f);

        for (int i = 0; i < commits.size; i++) {
            CommitInfo c = commits.get(i);
            String tagName = tags.get(c.sha);
            boolean isTag = tagName != null;
            boolean isStableTag = isTag && !tagName.contains("-");

            GraphNode tempNode = graphNodes.get(c.sha);
            if (tempNode == null)
                tempNode = new GraphNode(); // Fallback
            final GraphNode node = tempNode;

            t.table(row -> {
                row.left();

                // Timeline Graphic (Advanced Git Graph Renderer)
                final GraphNode fNode = node;
                row.add(new Element() {
                    @Override
                    public void draw() {
                        super.draw();

                        float cy = y + height / 2f;
                        float cx = x + 10f + fNode.lane * laneSpacing;

                        // Draw connections to parents (downwards)
                        // Note: Current row is drawn. Parents are in subsequent rows (y + height or
                        // more).
                        // Since we don't know exact Y of next row easily, we blindly draw line to
                        // bottom of cell.
                        // Actually, simplified graph assumes fixed step.

                        Lines.stroke(2f);
                        for (GraphConnection conn : fNode.connections) {
                            float tx = x + 10f + conn.toLane * laneSpacing;
                            float ty = y; // Bottom of this cell

                            Draw.color(GRAPH_COLORS[conn.toLane % GRAPH_COLORS.length]);

                            // If changing lanes, curve?
                            if (fNode.lane != conn.toLane) {
                                // Simple angled line
                                // Center to bottom-target-x
                                Lines.line(cx, cy, tx, ty);
                            } else {
                                // Straight down
                                Lines.line(cx, cy, cx, ty);
                            }
                        }

                        // Draw line from top? (If we are a child of previous)
                        // Hard to look back.
                        // Instead, assume continuous lines are drawn by parents?
                        // No, in standard list view, usually we draw line from Self -> Parents.
                        // And we expect Parents to draw line from Self -> Parents.
                        // Wait, a vertical line needs to span from Top to Bottom if it passes through.
                        // My Simple Algorithm doesn't track "Pass-through" lines in the Node.
                        // I need to know which lanes are active *passing through* this commit.

                        // FIX: My calculateGraph tracks active lanes. I should snapshot 'openLanes' at
                        // each step?
                        // Too complex for now.
                        // Workaround: Draw line from Top of cell to Center if we are continued?
                        // Actually, visually acceptable hack:
                        // Draw line UP from Center to Top (Same Lane) - to connect to child.

                        Draw.color(GRAPH_COLORS[fNode.colorIdx]);
                        Lines.line(cx, y + height, cx, cy);

                        Draw.reset();

                        // Draw Node
                        if (isStableTag) {
                            Draw.color(Pal.accent);
                            Icon.star.draw(cx - 8f, cy - 8f, 16f, 16f);
                        } else {
                            Draw.color(GRAPH_COLORS[fNode.colorIdx]);
                            Fill.circle(cx, cy, isTag ? 5f : 3f);
                        }
                        Draw.reset();
                    }
                }).width(graphWidth).growY().padRight(10f);

                // Content
                row.table(content -> {
                    content.left().defaults().left();

                    if (isStableTag) {
                        content.table(badge -> {
                            badge.background(Styles.black3);
                            badge.image(Icon.github).size(16f).color(Pal.accent).padRight(4f);
                            badge.add(tagName).color(Pal.accent);
                        }).padBottom(4f).padTop(4f).row();
                    }

                    // Message
                    content.add(c.message).color(Color.white).wrap().width(500f).padBottom(4f).row();

                    // Meta (Author, Date)
                    content.table(meta -> {
                        // Avatar
                        meta.table(avatar -> {
                            renderAvatar(avatar, c.avatarUrl, 16f);
                        }).size(16f).padRight(6f);

                        meta.add(c.authorName).color(Pal.accent).fontScale(0.8f).padRight(10f);
                        meta.add(c.date.replace("T", " ").replace("Z", "")).color(Color.lightGray).fontScale(0.7f);
                    });
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
        String downloadUrl = null;

        if (selectedRelease != null) {
            Log.info("Starting update for: " + selectedRelease.tagName);
            Log.info("Has JAR URL: " + !selectedRelease.jarDownloadUrl.isEmpty());
            if (!selectedRelease.jarDownloadUrl.isEmpty()) {
                downloadUrl = selectedRelease.jarDownloadUrl;
            } else {
                // Construct URL from tag name (fallback when API didn't return assets)
                downloadUrl = "https://github.com/" + REPO_URL + "/releases/download/"
                        + selectedRelease.tagName + "/MindustryToolMod.jar";
            }
            Log.info("Download URL: " + downloadUrl);
        } else {
            // No release selected - try latest release fallback
            Log.warn("No release selected, attempting to download latest release directly");
            downloadUrl = "https://github.com/" + REPO_URL + "/releases/latest/download/MindustryToolMod.jar";
        }

        if (downloadUrl != null) {
            downloadDirectly(downloadUrl);
        } else {
            Log.warn("Unable to determine download URL, falling back to GitHub API import");
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

        Log.info("Attempting direct download from: " + url);

        Http.get(url)
                .header("User-Agent", "MindustryToolMod") // Add UA to prevent 403 on some GH asset links
                .header("Accept", "application/octet-stream") // Required for binary asset downloads
                .error(e -> {
                    dialog.hide();
                    Log.err("Direct download failed from URL: " + url, e);
                    // Show more helpful error with fallback options
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.toLowerCase().contains("forbidden")) {
                        // Offer fallback options
                        BaseDialog fallbackDialog = new BaseDialog("Download Failed");
                        fallbackDialog.cont.add("Direct download blocked.").color(Pal.remove).row();
                        fallbackDialog.cont.add("Try alternative methods:").color(Color.gray).padTop(10f).row();

                        fallbackDialog.buttons.defaults().size(180f, 50f).pad(5f);
                        fallbackDialog.buttons.button("@back", Icon.left, fallbackDialog::hide);
                        fallbackDialog.buttons.button("Use Game Import", Icon.download, () -> {
                            fallbackDialog.hide();
                            Vars.ui.mods.githubImportMod(REPO_URL, true, null);
                        }).color(Pal.accent);
                        fallbackDialog.buttons.button("Open GitHub", Icon.link, () -> {
                            fallbackDialog.hide();
                            Core.app.openURI("https://github.com/" + REPO_URL + "/releases");
                        }).color(Color.sky);

                        fallbackDialog.show();
                    } else {
                        Vars.ui.showException("Download Failed", e);
                    }
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
                        // STRICT: Only notify if it is a STABLE release (Main branch)
                        // This prevents Beta builds on Main from triggering prompts if they are
                        // accidentally tagged stable?
                        // Actually, our ReleaseInfo logic sets type based on suffix.
                        // "v2.23.0" -> STABLE
                        // "v2.23.0-beta..." -> BETA

                        if (latestStable.getVersion().type == mindustrytool.utils.Version.SuffixType.STABLE) {
                            Log.info("Stable update available: @ -> @", current, latestStable.tagName);
                            Core.app.post(() -> new UpdateCenterDialog().show());
                        } else {
                            Log.info("Update available (@) but ignored (Pre-release).", latestStable.tagName);
                        }
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

    // --- Git Graph Implementation ---

    private static final Color[] GRAPH_COLORS = {
            Pal.accent,
            Pal.lancerLaser,
            Pal.sapBullet,
            Pal.missileYellow,
            Color.cyan,
            Color.magenta,
            Color.green
    };

    // Data structure for the graph presentation
    private class GraphNode {
        int lane;
        int colorIdx;
        // Connections to parents: {Target SHA, Target Lane}
        Seq<GraphConnection> connections = new Seq<>();
    }

    private class GraphConnection {
        String toSha;
        int toLane;

        GraphConnection(String s, int l) {
            toSha = s;
            toLane = l;
        }
    }

    // Map of SHA -> Node Data
    private final ObjectMap<String, GraphNode> graphNodes = new ObjectMap<>();

    private void calculateGraph() {
        graphNodes.clear();
        if (commits.isEmpty())
            return;

        // "Open Lanes" tracks which SHA is expected at the tip of each lane
        // Lane Index -> SHA
        Seq<String> openLanes = new Seq<>();

        // Iterate new to old
        for (CommitInfo c : commits) {
            GraphNode node = new GraphNode();

            // 1. Assign Lane
            int myLane = openLanes.indexOf(c.sha, false);

            if (myLane == -1) {
                // Not expecting this commit (new tip or branch start)
                // Find empty lane or create new
                myLane = openLanes.indexOf(null, false);
                if (myLane == -1) {
                    myLane = openLanes.size;
                    openLanes.add(c.sha);
                } else {
                    openLanes.set(myLane, c.sha);
                }
            }
            // Mark lane as occupied by me
            // (We don't remove yet, we wait to replace with parents)

            node.lane = myLane;
            node.colorIdx = myLane % GRAPH_COLORS.length;
            graphNodes.put(c.sha, node);

            // 2. Process Parents -> Connections
            // Replace my slot in openLanes with my FIRST parent
            // Additional parents get their own lanes

            if (c.parents != null && c.parents.length > 0) {
                // Primary parent continues this lane
                String p1 = c.parents[0];
                openLanes.set(myLane, p1);
                node.connections.add(new GraphConnection(p1, myLane));

                // Secondary parents (Merge)
                for (int i = 1; i < c.parents.length; i++) {
                    String pNext = c.parents[i];
                    // Does this parent already have a lane? (Merge from existing)
                    int pLane = openLanes.indexOf(pNext, false);
                    if (pLane == -1) {
                        // Find new lane for this parent
                        pLane = openLanes.indexOf(null, false);
                        if (pLane == -1) {
                            pLane = openLanes.size;
                            openLanes.add(pNext);
                        } else {
                            openLanes.set(pLane, pNext);
                        }
                    }
                    node.connections.add(new GraphConnection(pNext, pLane));
                }
            } else {
                // No parents (Initial commit), close this lane
                openLanes.set(myLane, null);
            }
        }
    }
}
