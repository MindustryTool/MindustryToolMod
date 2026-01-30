package mindustrytool.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;

public class ChangelogDialog extends BaseDialog {
    private int page = 1;
    private final int perPage = 10;
    private Table listTable;
    private boolean isLoading = false;
    private boolean hasNextPage = true;

    public ChangelogDialog() {
        super("@changelog.title");

        addCloseButton();
        closeOnBack();

        cont.clear();
        listTable = new Table();
        listTable.top().left();

        ScrollPane pane = new ScrollPane(listTable);
        pane.setScrollingDisabled(true, false);
        cont.add(pane).grow().row();

        Table paginationTable = new Table();
        paginationTable.button(Icon.left, () -> {
            if (page > 1 && !isLoading) {
                page--;
                fetchReleases();
            }
        }).size(50f).disabled(t -> page <= 1 || isLoading);

        paginationTable.add("").width(20f);
        paginationTable.label(() -> String.valueOf(page)).fontScale(1.2f);
        paginationTable.add("").width(20f);

        paginationTable.button(Icon.right, () -> {
            if (hasNextPage && !isLoading) {
                page++;
                fetchReleases();
            }
        }).size(50f).disabled(t -> !hasNextPage || isLoading);

        cont.add(paginationTable).pad(10f);

        buttons.button("@feature.copy-debug-detail", Icon.export, () -> {
            try {
                HashMap<String, Object> json = new HashMap<>();

                String lastLog = Vars.dataDirectory.child("last_log.txt").readString();
                String type = Core.app.getType().name();
                float uiScale = Scl.scl();
                String locale = Core.bundle.getLocale().toLanguageTag();
                float windowWidth = Core.graphics.getWidth();
                float windowHeight = Core.graphics.getHeight();
                boolean fullscreen = Core.graphics.isFullscreen();
                boolean isPortrait = Core.graphics.isPortrait();

                String mods = Vars.mods.getModStrings().reduce("", (a, b) -> a + b + "\n");

                json.put("mods", mods);
                json.put("type", type);
                json.put("window_width", String.valueOf(windowWidth));
                json.put("window_height", String.valueOf(windowHeight));
                json.put("fullscreen", String.valueOf(fullscreen));
                json.put("is_portrait", String.valueOf(isPortrait));
                json.put("locale", locale);
                json.put("ui_scale", String.valueOf(uiScale));
                json.put("last_log", lastLog);
                var tree = getUiTree(Core.scene.root);
                json.put("ui_tree", tree);
                json.put("flatten_ui_tree", flattenUiTree(tree));

                Core.app.setClipboardText(Utils.toJsonPretty(json));
                Vars.ui.showInfoFade("@coppied");

            } catch (Exception err) {
                Vars.ui.showException(err);
            }
        });

        shown(this::fetchReleases);
    }

    private void fetchReleases() {
        if (isLoading)
            return;
        isLoading = true;

        listTable.clear();
        listTable.add("@loading").color(Color.lightGray).center().grow();

        String url = Config.GITHUB_API_URL + "?page=" + page + "&per_page=" + perPage;

        Http.get(url).error(e -> {
            Log.err("Failed to fetch releases", e);
            isLoading = false;
            Core.app.post(() -> {
                listTable.clear();
                listTable.add("@error.fetch-releases").color(Color.scarlet).center().grow();
            });
        }).submit(res -> {
            try {
                Jval json = Jval.read(res.getResultAsString());
                if (json.isArray()) {
                    Seq<Jval> releases = json.asArray();
                    hasNextPage = releases.size >= perPage;
                    Core.app.post(() -> {
                        isLoading = false;
                        rebuildList(releases);
                    });
                } else {
                    isLoading = false;
                    Core.app.post(() -> {
                        listTable.clear();
                        listTable.add("@error.invalid-response").color(Color.scarlet).center().grow();
                    });
                }
            } catch (Exception e) {
                Log.err("Failed to parse releases", e);
                isLoading = false;
                Core.app.post(() -> {
                    listTable.clear();
                    listTable.add("@error.parse-failed").color(Color.scarlet).center().grow();
                });
            }
        });
    }

    private void rebuildList(Seq<Jval> releases) {
        listTable.clear();
        listTable.top().left();

        if (releases.size == 0) {
            listTable.add("@changelog.empty").color(Color.lightGray).center().grow();
            return;
        }

        for (Jval release : releases) {
            String tagName = release.getString("tag_name", "Unknown");
            String body = release.getString("body", "");
            String name = release.getString("name", tagName);
            int downloadCount = 0;

            if (release.has("assets")) {
                for (Jval asset : release.get("assets").asArray()) {
                    downloadCount += asset.getInt("download_count", 0);
                }
            }

            final int finalDownloadCount = downloadCount;
            final String finalTagName = tagName;
            final String finalBody = body;
            final String finalName = name;

            listTable.table(Styles.black5, t -> {
                t.top().left().margin(10f);

                t.table(header -> {
                    header.left();
                    header.add("[accent]" + finalName + "[]").style(Styles.defaultLabel).growX().left();
                    header.add("[lightgray]" + finalTagName + "[]").padLeft(10f);
                }).growX().row();

                t.table(stats -> {
                    stats.left();
                    stats.image(Icon.download).size(16f).color(Color.gold);
                    stats.add("[gold] " + finalDownloadCount + "[]").padLeft(5f);
                }).padTop(5f).growX().row();

                t.image().color(Color.gray).height(2f).growX().padTop(5f).padBottom(5f).row();

                t.add(Utils.renderMarkdown(finalBody)).wrap().growX().left().padBottom(10f).row();

                t.button("@install", Icon.download, () -> {
                    try {
                        Vars.ui.mods.show();
                        Vars.ui.mods.githubImportMod(Config.REPO_URL, true, finalTagName);
                        Vars.ui.mods.toFront();
                        // Close dialogs to show the mod installation progress
                        this.hide();
                    } catch (Throwable e) {
                        Log.err(e);
                        Vars.ui.showException(e);
                    }
                }).size(150f, 45f).right();
            }).growX().pad(10f).row();
        }
    }

    private UiTree getUiTree(Element element) {
        var node = new UiTree(element.name, element.getClass().getSimpleName());

        if (element instanceof Group group) {
            node.children = group.getChildren().map(child -> getUiTree(child)).list();
        }

        return node;
    }

    private List<String> flattenUiTree(UiTree tree) {
        List<String> result = new ArrayList<>();
        flatten(tree, tree.name != null ? tree.type + "(" + tree.name + ")" : tree.type, result);
        result.sort(String::compareTo);
        return result;
    }

    private void flatten(UiTree node, String path, List<String> result) {
        if (node.children == null || node.children.isEmpty()) {
            result.add(path);
            return;
        }

        for (UiTree child : node.children) {
            flatten(child, path + "." + (child.name != null ? child.type + "(" + child.name + ")" : child.type),
                    result);
        }
    }

    private static class UiTree {
        public String name;
        public String type;
        public List<UiTree> children;

        public UiTree(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

}
