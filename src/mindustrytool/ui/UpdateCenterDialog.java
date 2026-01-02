package mindustrytool.ui;

import arc.Core;
import arc.files.Fi;
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

        // Cleanup legacy settings to prevent crashes
        if (Core.settings.has("mindustrytool-releases-json"))
            Core.settings.remove("mindustrytool-releases-json");
        if (Core.settings.has("mindustrytool-tags-json"))
            Core.settings.remove("mindustrytool-tags-json");
        if (Core.settings.has("mindustrytool-branches-json"))
            Core.settings.remove("mindustrytool-branches-json");
        if (Core.settings.has("mindustrytool-commits-json"))
            Core.settings.remove("mindustrytool-commits-json");

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
                            saveStringCacheToFile("tags", json);

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
        String json = loadStringCacheFromFile("tags");
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
            if (!loading)
                Core.app.post(this::rebuildUI);
            return;
        }

        // Fetch branches first, then commits from all key branches
        if (activeBranches.isEmpty()) {
            Http.get(BRANCHES_API)
                    .header("User-Agent", "MindustryToolMod")
                    .error(e -> {
                        handleError(e);
                        // Fallback to fetch main only if branches fail
                        fetchBranchCommits("main");
                    })
                    .submit(res -> {
                        try {
                            Jval val = Jval.read(res.getResultAsString());
                            if (val.isArray()) {
                                for (Jval v : val.asArray())
                                    activeBranches.add(v.getString("name", ""));
                            }
                            fetchMultiBranchCommits();
                        } catch (Exception e) {
                            Log.err(e);
                            fetchBranchCommits("main");
                        }
                    });
        } else {
            fetchMultiBranchCommits();
        }
    }

    private void fetchMultiBranchCommits() {
        // Fetch commits from multiple active branches to expose graph topology
        ObjectSet<String> branchesToFetch = new ObjectSet<>();
        branchesToFetch.add("main"); // Always fetch main

        // Add other active branches (limit to 5 to avoid API span)
        int count = 0;
        for (String b : activeBranches) {
            if (!b.equals("main") && count < 4) {
                branchesToFetch.add(b);
                count++;
            }
        }

        // Log.info("[GitGraph] Fetching commits from branches: @", branchesToFetch);

        // Parallel Fetch
        Seq<CommitInfo> allCommits = new Seq<>();
        ObjectSet<String> seenShas = new ObjectSet<>();
        int[] pending = { branchesToFetch.size };

        for (String branch : branchesToFetch) {
            Http.get(COMMITS_API + "?sha=" + branch + "&per_page=100") // Increased to 100
                    .header("User-Agent", "MindustryToolMod")
                    .error(e -> {
                        Log.err("Failed to fetch commits for branch: " + branch, e);
                        synchronized (pending) {
                            pending[0]--;
                            if (pending[0] <= 0)
                                finalizeCommits(allCommits);
                        }
                    })
                    .submit(res -> {
                        try {
                            Jval array = Jval.read(res.getResultAsString());
                            if (array.isArray()) {
                                synchronized (allCommits) {
                                    for (Jval item : array.asArray()) {
                                        String sha = item.getString("sha", "");
                                        if (seenShas.add(sha)) {
                                            allCommits.add(new CommitInfo(item));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.err(e);
                        }

                        synchronized (pending) {
                            pending[0]--;
                            if (pending[0] <= 0)
                                finalizeCommits(allCommits);
                        }
                    });
        }
    }

    private void fetchBranchCommits(String branch) {
        // Fallback for single branch fetch
        Http.get(COMMITS_API + "?sha=" + branch)
                .header("User-Agent", "MindustryToolMod")
                .error(e -> {
                    if (commits.isEmpty())
                        loadCommitsFromDisk();
                    handleError(e);
                })
                .submit(res -> {
                    try {
                        Jval array = Jval.read(res.getResultAsString());
                        if (array.isArray()) {
                            Seq<CommitInfo> fetched = new Seq<>();
                            for (Jval item : array.asArray())
                                fetched.add(new CommitInfo(item));
                            finalizeCommits(fetched);
                        }
                    } catch (Exception e) {
                        Log.err(e);
                    }
                });
    }

    private void finalizeCommits(Seq<CommitInfo> result) {
        // Sort descenting by date
        result.sort(c -> -c.date.compareTo(c.date));

        cachedCommits = result;
        lastCommitFetch = System.currentTimeMillis();

        Core.app.post(() -> {
            commits.clear();
            commits.addAll(result);

            // Save to file instead of settings to avoid UTF limit crash
            saveCacheToFile("commits", result);

            if (!loading)
                rebuildUI();
        });
    }

    private void saveCacheToFile(String name, Seq<CommitInfo> data) {
        try {
            Jval array = Jval.newArray();
            for (CommitInfo c : data) {
                Jval obj = Jval.newObject();
                obj.put("sha", c.sha);
                obj.put("html_url", c.htmlUrl);

                Jval commitObj = Jval.newObject();
                commitObj.put("message", c.message);
                Jval authorObj = Jval.newObject();
                authorObj.put("name", c.authorName);
                authorObj.put("date", c.date);
                commitObj.put("author", authorObj);
                obj.put("commit", commitObj);

                // Parents
                if (c.parents != null && c.parents.length > 0) {
                    Jval pArray = Jval.newArray();
                    for (String p : c.parents) {
                        Jval pObj = Jval.newObject();
                        pObj.put("sha", p);
                        pArray.add(pObj);
                    }
                    obj.put("parents", pArray);
                }

                if (c.avatarUrl != null && !c.avatarUrl.isEmpty()) {
                    Jval uObj = Jval.newObject();
                    uObj.put("avatar_url", c.avatarUrl);
                    obj.put("author", uObj);
                }

                array.add(obj);
            }
            saveStringCacheToFile(name, array.toString());
        } catch (Exception e) {
            Log.err("Failed to save cache file " + name, e);
        }
    }

    private void saveStringCacheToFile(String name, String data) {
        try {
            Core.settings.getDataDirectory().child("mindustrytool-" + name + ".json").writeString(data);
        } catch (Exception e) {
            Log.err("Failed to save cache file " + name, e);
        }
    }

    private String loadStringCacheFromFile(String name) {
        try {
            Fi file = Core.settings.getDataDirectory().child("mindustrytool-" + name + ".json");
            if (file.exists()) {
                return file.readString();
            }
        } catch (Exception e) {
            Log.err("Failed to load cache file " + name, e);
        }
        return null;
    }

    private void loadCommitsFromDisk() {
        try {
            String json = loadStringCacheFromFile("commits");
            if (json != null) {
                Jval val = Jval.read(json);
                if (val.isArray()) {
                    for (Jval item : val.asArray())
                        commits.add(new CommitInfo(item));
                }
            }
        } catch (Exception e) {
            Log.err("Failed to load commits from disk", e);
        }
    }

    private void fetchBranches() {
        if (cachedBranches != null && System.currentTimeMillis() - lastBranchFetch < CACHE_DURATION) {
            activeBranches.clear();
            activeBranches.addAll(cachedBranches);
            return;
        }

        Http.get(BRANCHES_API)
                .header("User-Agent", "MindustryToolMod")
                .error(this::handleError)
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
                    } catch (Exception e) {
                        Log.err("Failed to parse branches", e);
                    }
                    if (!loading)
                        Core.app.post(this::rebuildUI);
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
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30 minutes

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

                            saveStringCacheToFile("releases", json); // Persist to file
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
        String json = loadStringCacheFromFile("releases");
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
        buttons.clearChildren(); // Ensure clean state
        buttons.bottom();
        if (Core.graphics.isPortrait()) {
            // Mobile: Centered fixed size to prevent huge separation on wide screens
            buttons.defaults().size(150f, 64f).pad(4f);
        } else {
            // Desktop: Fixed width, centered
            buttons.defaults().size(210f, 64f).pad(8f);
        }

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
        cont.defaults().pad(0f); // Reset defaults
        cont.margin(20f); // Add margin to prevent screen edge touching

        // Wrapper to constrain width on PC but fill on Mobile
        // IMPORTANT: We use grow() here so this table takes all available space in
        // 'cont'.
        // 'cont' itself grows in BaseDialog.
        cont.table(main -> {
            main.top();
            main.defaults().growX().maxWidth(600f).padBottom(5f);

            // 1. Header (Removed)

            // 2. Filters (Horizontally scrollable)
            Table filterRow = new Table();
            filterRow.left();

            // Reload button
            filterRow.button(Icon.refresh, Styles.clearNonei, this::refresh).size(40f).padRight(10f);

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
            // Using growY() allows it to take available space.
            Table items = new Table();
            ScrollPane scroll = new ScrollPane(items, Styles.smallPane);
            scroll.setFadeScrollBars(false);
            // Add uniformY() to force equal height distribution with other uniformY cells
            // (the timeline)
            main.add(scroll).growY().uniformY().minHeight(100f).row();

            buildReleaseList(items);

            // 4. Timeline
            main.add("Recent Activity").color(Color.lightGray).left().padTop(10f).row();
            main.image().height(2f).color(Color.darkGray).fillX().padBottom(5f).row();

            Table timeline = new Table();
            ScrollPane timeScroll = new ScrollPane(timeline, Styles.smallPane);
            timeScroll.setFadeScrollBars(false);
            // growY() + uniformY() here too ensures it matches the list above
            main.add(timeScroll).growY().uniformY().minHeight(100f).row();

            buildTimeline(timeline);
        }).grow().maxWidth(600f);
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
        // Responsive width: On mobile portrait, use full width with padding
        float dialogWidth = Core.graphics.isPortrait()
                ? Core.graphics.getWidth() - 30f // Reduced margin for mobile
                : Math.min(600f, Core.graphics.getWidth() * 0.7f); // Increased PC max width slightly
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
        // Use percentage width for mobile (almost 50% each)
        if (Core.graphics.isPortrait()) {
            dialog.buttons.defaults().width(contentWidth / 2f - 10f).height(64f).pad(5f);
        } else {
            dialog.buttons.defaults().size(210f, 64f).pad(8f);
        }

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
                        Lines.stroke(2.5f);
                        for (GraphConnection conn : fNode.connections) {
                            float tx = x + 10f + conn.toLane * laneSpacing;
                            float ty = y; // Bottom of this cell

                            Draw.color(GRAPH_COLORS[conn.toLane % GRAPH_COLORS.length]);

                            if (fNode.lane != conn.toLane) {
                                // Bezier curve for lane changes
                                drawBezierCurve(cx, cy, tx, ty, 16);
                            } else {
                                // Straight down
                                Lines.line(cx, cy, cx, ty);
                            }
                        }

                        // Draw vertical line from top to center (connect to child)
                        Draw.color(GRAPH_COLORS[fNode.colorIdx]);
                        Lines.line(cx, y + height, cx, cy);

                        Draw.reset();

                        // Draw Node with glow effect
                        if (isStableTag) {
                            // Star for stable releases
                            Draw.color(Pal.accent, 0.3f);
                            Fill.circle(cx, cy, 12f); // Glow
                            Draw.color(Pal.accent);
                            Icon.star.draw(cx - 8f, cy - 8f, 16f, 16f);
                        } else if (isTag) {
                            // Larger dot for tags (beta/dev)
                            Draw.color(GRAPH_COLORS[fNode.colorIdx], 0.3f);
                            Fill.circle(cx, cy, 8f); // Glow
                            Draw.color(GRAPH_COLORS[fNode.colorIdx]);
                            Fill.circle(cx, cy, 5f);
                        } else {
                            // Small dot for regular commits
                            Draw.color(GRAPH_COLORS[fNode.colorIdx]);
                            Fill.circle(cx, cy, 3f);
                        }
                        Draw.reset();
                    }

                    // Draw smooth Bezier curve
                    private void drawBezierCurve(float x1, float y1, float x2, float y2, int segments) {
                        // Control points for S-curve
                        float midY = (y1 + y2) / 2f;
                        float cx1 = x1;
                        float cy1 = midY;
                        float cx2 = x2;
                        float cy2 = midY;

                        float prevX = x1, prevY = y1;
                        for (int i = 1; i <= segments; i++) {
                            float t = (float) i / segments;
                            float u = 1 - t;

                            // Cubic Bezier formula
                            float bx = u * u * u * x1 + 3 * u * u * t * cx1 + 3 * u * t * t * cx2 + t * t * t * x2;
                            float by = u * u * u * y1 + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y2;

                            Lines.line(prevX, prevY, bx, by);
                            prevX = bx;
                            prevY = by;
                        }
                    }
                }).width(graphWidth).growY().padRight(10f);

                // Content
                row.table(content -> {
                    content.left().defaults().left();

                    if (isStableTag) {
                        content.table(badge -> {
                            badge.background(Styles.black3);
                            badge.image(Icon.github).size(12f).color(Pal.accent).padRight(4f);
                            badge.add(tagName).color(Pal.accent);
                        }).padBottom(4f).padTop(4f).row();
                    }

                    // Message
                    // Responsive width for message to prevent horizontal overflow on mobile
                    float availableWidth;
                    if (Core.graphics.isPortrait()) {
                        // Screen width - Graph width - Margins/Padding (approx 70f)
                        availableWidth = Core.graphics.getWidth() - graphWidth - 70f;
                    } else {
                        availableWidth = 500f;
                    }
                    content.add(c.message).color(Color.white).wrap().width(availableWidth).padBottom(4f).row();

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
     * Uses cache to avoid excessive API calls.
     */
    public static void checkSilent() {
        mindustry.mod.Mods.LoadedMod mod = Vars.mods.getMod(mindustrytool.Main.class);
        if (mod == null)
            return;

        mindustrytool.utils.Version current = new mindustrytool.utils.Version(mod.meta.version);

        // Check cache first to avoid unnecessary API calls
        if (cachedReleases != null && System.currentTimeMillis() - lastReleaseFetch < CACHE_DURATION) {
            Log.info("Silent update check: Using cached releases (saves API quota)");
            checkReleasesForUpdate(cachedReleases, current);
            return;
        }

        // Also check if we already checked recently (even without full cache)
        long lastCheck = Core.settings.getLong("mindustrytool-last-update-check", 0);
        if (System.currentTimeMillis() - lastCheck < CACHE_DURATION) {
            Log.info("Silent update check: Skipped (checked recently)");
            return;
        }

        Http.get(RELEASES_API, response -> {
            try {
                Core.settings.put("mindustrytool-last-update-check", System.currentTimeMillis());

                Jval array = Jval.read(response.getResultAsString());
                if (array.isArray() && array.asArray().size > 0) {
                    // Build cache while checking
                    Seq<ReleaseInfo> releases = new Seq<>();
                    for (Jval val : array.asArray()) {
                        ReleaseInfo info = new ReleaseInfo(val);
                        if (!info.draft) {
                            releases.add(info);
                        }
                    }

                    // Update cache
                    cachedReleases = releases;
                    lastReleaseFetch = System.currentTimeMillis();

                    checkReleasesForUpdate(releases, current);
                }
            } catch (Exception e) {
                Log.err("Silent update check failed", e);
            }
        }, error -> {
            Log.err("Silent update check failed", error);
        });
    }

    /** Helper to check releases for updates (used by checkSilent) */
    private static void checkReleasesForUpdate(Seq<ReleaseInfo> releases, mindustrytool.utils.Version current) {
        // Find the latest STABLE release
        ReleaseInfo latestStable = null;
        for (ReleaseInfo info : releases) {
            if (info.getVersion().type == mindustrytool.utils.Version.SuffixType.STABLE) {
                latestStable = info;
                break;
            }
        }

        if (latestStable != null && latestStable.getVersion().isNewerThan(current)) {
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

        // Log.info("[GitGraph] Starting calculation for @ commits", commits.size);

        // "Open Lanes" tracks which SHA is expected at the tip of each lane
        // Lane Index -> SHA
        Seq<String> openLanes = new Seq<>();

        // Iterate new to old
        for (CommitInfo c : commits) {
            // Log.info("[GitGraph] Commit @: @ parents", c.sha.substring(0, 7), c.parents
            // != null ? c.parents.length : 0);
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
