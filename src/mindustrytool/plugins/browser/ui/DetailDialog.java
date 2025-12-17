package mindustrytool.plugins.browser.ui;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.plugins.browser.MapDetailData;
import mindustrytool.plugins.browser.SchematicDetailData;

public class DetailDialog extends BaseDialog {
    private final Table heroTable = new Table();
    private final Table actionTable = new Table();
    private final Table tabsTable = new Table();
    private final Table contentTable = new Table();

    public DetailDialog() {
        super("");
        addCloseListener();
        setFillParent(true);
        setupUI();
    }

    private void setupUI() {
        cont.clear();
        // Main Container
        cont.pane(t -> {
            t.top();
            // 1. Hero Section (Preview + Quick Stats)
            t.add(heroTable).growX().row();

            // 2. Action Bar
            t.add(actionTable).growX().pad(10f).row();

            // 3. Tabs (Details | Comments)
            t.add(tabsTable).growX().row();

            // 4. Tab Content
            t.add(contentTable).grow().row();
        }).grow();

        addCloseButton();
    }

    public void show(MapDetailData data) {
        buildHero(data);
        buildActionBar(data, data.id());
        buildTabs(data, data.id());
        show();
    }

    public void show(SchematicDetailData data) {
        buildHero(data);
        buildActionBar(data, data.id());
        buildTabs(data, data.id());
        show();
    }

    private void buildHero(Object data) {
        heroTable.clear();
        heroTable.top();

        // Extract common data
        String id;
        final String authorId; // Assuming createdBy is ID
        Long likes, downloads;

        if (data instanceof MapDetailData) {
            MapDetailData m = (MapDetailData) data;
            authorId = m.createdBy();
            id = m.id();
            likes = m.likes();
            downloads = m.downloads();
        } else {
            SchematicDetailData s = (SchematicDetailData) data;
            authorId = s.createdBy();
            id = s.id();
            likes = s.likes();
            downloads = s.downloads();
        }

        boolean mobile = Core.graphics.isPortrait();

        Table previewTable = new Table();
        mindustrytool.plugins.browser.ImageHandler.ImageType imgType = (data instanceof MapDetailData)
                ? mindustrytool.plugins.browser.ImageHandler.ImageType.MAP
                : mindustrytool.plugins.browser.ImageHandler.ImageType.SCHEMATIC;

        previewTable.add(new mindustrytool.plugins.browser.ImageHandler(id, imgType))
                .size(mobile ? mobileWidth() : 400).pad(10f);

        Table infoTable = new Table();
        infoTable.top().left().margin(10f);

        // Name (Title)
        String contentName = (data instanceof MapDetailData) ? ((MapDetailData) data).name()
                : ((SchematicDetailData) data).name();
        infoTable.add(contentName).fontScale(1.2f).color(mindustry.graphics.Pal.accent).left().row();

        // Author Area (Async Load)
        Table authorTable = new Table();
        authorTable.left();
        // Placeholder Avatar
        arc.scene.ui.Image avatarImage = new arc.scene.ui.Image(mindustry.gen.Icon.players);
        authorTable.add(avatarImage).size(24).padRight(5);

        // Placeholder Name (ID)
        arc.scene.ui.Label authorLabel = new arc.scene.ui.Label("Loading...");
        authorLabel.setColor(mindustry.graphics.Pal.lightishGray);
        authorTable.add(authorLabel).left();

        infoTable.add(authorTable).left().padBottom(10f).row();

        // Fetch User Data
        mindustrytool.plugins.browser.Api.findUserById(authorId, user -> {
            if (user != null) {
                Core.app.post(() -> {
                    authorLabel.setText(user.name());
                    if (user.imageUrl() != null && !user.imageUrl().isEmpty()) {
                        loadAvatar(user.imageUrl(), avatarImage);
                    }
                });
            } else {
                Core.app.post(() -> authorLabel.setText(authorId)); // Fallback to ID
            }
        });

        // App/ID Info
        infoTable.add("ID: " + id).color(mindustry.graphics.Pal.darkishGray).fontScale(0.8f).left().row();

        // Stats
        Table statsGrid = new Table();
        statsGrid.left().defaults().padRight(20f).left();
        statsGrid.add(longIcon(mindustry.gen.Icon.download, downloads));
        statsGrid.add(longIcon(mindustry.gen.Icon.upOpen, likes)); // Changed icon
        infoTable.add(statsGrid).left().padTop(10f).row();

        if (mobile) {
            heroTable.add(previewTable).row();
            heroTable.add(infoTable).growX();
        } else {
            heroTable.add(previewTable).top();
            heroTable.add(infoTable).growX().top();
        }
    }

    private void buildActionBar(Object data, String id) {
        actionTable.clear();
        actionTable.left().defaults().size(48).padRight(8f);

        // Open Web
        actionTable.button(mindustry.gen.Icon.link, mindustry.ui.Styles.defaulti,
                () -> Core.app.openURI(mindustrytool.plugins.browser.Config.WEB_URL +
                        (data instanceof MapDetailData ? "/maps/" : "/schematics/") + id))
                .tooltip("@open");

        // Copy ID
        actionTable.button(mindustry.gen.Icon.copy, mindustry.ui.Styles.defaulti,
                () -> {
                    Core.app.setClipboardText(id);
                    mindustry.Vars.ui.showInfoFade("@copied");
                }).tooltip("@copy");

        // Download
        actionTable.button(mindustry.gen.Icon.download, mindustry.ui.Styles.defaulti,
                () -> {
                    if (data instanceof MapDetailData) {
                        mindustrytool.plugins.browser.ContentHandler.downloadMap(
                                new mindustrytool.plugins.browser.ContentData().id(id));
                    } else {
                        mindustrytool.plugins.browser.ContentHandler.downloadSchematic(
                                new mindustrytool.plugins.browser.ContentData().id(id));
                    }
                }).tooltip("@download");

        // Like/Vote
        actionTable.button(mindustry.gen.Icon.upOpen, mindustry.ui.Styles.defaulti,
                () -> {
                    if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() == null) {
                        mindustry.Vars.ui.showInfo("Login required to vote.");
                        return;
                    }
                    mindustrytool.plugins.browser.Api.vote(
                            data instanceof MapDetailData ? "map" : "schematic", id, success -> {
                                if (success) {
                                    mindustry.Vars.ui.showInfoFade("Voted!");
                                } else {
                                    mindustry.Vars.ui.showInfoFade("Vote failed.");
                                }
                            });
                })
                .tooltip("Vote Up");
    }

    private void buildTabs(Object data, String id) {
        tabsTable.clear();
        tabsTable.left().defaults().height(40f).padRight(0f);
        tabsTable.background(mindustry.ui.Styles.black6); // Add background to tab bar

        // Tab Buttons
        mindustry.ui.Styles.flatTogglet.fontColor = mindustry.graphics.Pal.accent;

        // Create a ButtonGroup for mutual exclusivity (visual)
        arc.scene.ui.ButtonGroup<arc.scene.ui.Button> group = new arc.scene.ui.ButtonGroup<>();

        tabsTable.button("@details", mindustry.ui.Styles.flatTogglet, () -> showDetails(data, id))
                .group(group).checked(true).growX().height(50f); // Taller tabs

        tabsTable.button("@comments", mindustry.ui.Styles.flatTogglet, () -> showComments(data, id))
                .group(group).growX().height(50f);

        // Initial show
        showDetails(data, id);
    }

    private void showDetails(Object data, String id) {
        contentTable.clear();
        contentTable.top().left().margin(10f);

        if (data instanceof SchematicDetailData) {
            SchematicDetailData s = (SchematicDetailData) data;

            // Show metadata requirements immediately if available
            if (s.meta() != null && s.meta().requirements() != null) {
                // Wrap in panel
                Table reqTable = new Table();
                reqTable.background(mindustry.ui.Styles.black6).margin(10f);

                reqTable.add("Requirements (Preview)").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
                reqTable.table(t -> {
                    t.left();
                    int i = 0;
                    for (mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement req : s.meta()
                            .requirements()) {
                        mindustry.type.Item item = mindustry.Vars.content.items()
                                .find(it -> it.name.equalsIgnoreCase(req.name()));
                        if (item == null)
                            item = mindustry.Vars.content.items()
                                    .find(it -> it.localizedName.equalsIgnoreCase(req.name()));

                        if (item != null) {
                            t.image(item.uiIcon).size(24f).padRight(4f);
                        } else {
                            t.image().color(arc.graphics.Color.valueOf(req.color() != null ? req.color() : "ffffff"))
                                    .size(10f);
                        }
                        t.add(String.valueOf(req.amount())).color(mindustry.graphics.Pal.lightishGray).padRight(10f);
                        if (++i % 4 == 0)
                            t.row();
                    }
                }).left().growX();
                contentTable.add(reqTable).growX().padBottom(10f).row();
            }

            contentTable.add("Downloading full details...").color(mindustry.graphics.Pal.accent).left().row();
            mindustrytool.plugins.browser.Api.downloadSchematic(id, bytes -> {
                try {
                    // Try to read as standard schematic first
                    mindustry.game.Schematic sc = null;
                    try {
                        sc = mindustrytool.plugins.browser.SchematicUtils.read(
                                new java.io.ByteArrayInputStream(bytes));
                    } catch (Exception e) {
                        // If missing header, try Base64 decode
                        if (e.getMessage() != null && e.getMessage().contains("missing header")) {
                            try {
                                String base64 = new String(bytes).trim();
                                byte[] decoded = java.util.Base64.getDecoder().decode(base64);
                                sc = mindustrytool.plugins.browser.SchematicUtils.read(
                                        new java.io.ByteArrayInputStream(decoded));
                            } catch (Exception ex) {
                                // Fallback failed, throw original
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    }

                    if (sc != null) {
                        mindustry.game.Schematic finalSc = sc;
                        Core.app.post(() -> buildSchematicDetails(finalSc));
                    }
                } catch (Exception e) {
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < Math.min(bytes.length, 20); i++) {
                        hex.append(String.format("%02X ", bytes[i]));
                    }
                    String headerPreview = hex.toString();

                    arc.util.Log.err("Schematic Load Error", e);
                    arc.util.Log.info("Schematic Data Hex: " + headerPreview);

                    boolean hasMetadata = (data instanceof SchematicDetailData &&
                            ((SchematicDetailData) data).meta() != null);

                    Core.app.post(() -> {
                        if (!hasMetadata) {
                            contentTable.clear();
                            String message = "Failed: " + e.getMessage() +
                                    "\nLength: " + bytes.length +
                                    "\nHex: " + headerPreview;
                            contentTable.add(message)
                                    .color(mindustry.graphics.Pal.remove)
                                    .wrap().growX();
                        } else {
                            // Soft fail - keep preview, just notify via log
                            // mindustry.Vars.ui.showInfoFade("Full details unavailable");

                            // Try to remove the "Downloading..." label if found at the bottom
                            if (contentTable.getChildren().size > 0 &&
                                    contentTable.getChildren().peek() instanceof arc.scene.ui.Label) {
                                arc.scene.Element el = contentTable.getChildren().peek();
                                if (((arc.scene.ui.Label) el).getText().toString().contains("Downloading")) {
                                    el.remove();
                                }
                            }
                        }
                    });
                }
            });
            // Note: Since Api.download might not call back on error, the "Downloading..."
            // might persist.
            // This is acceptable for now vs "Length: 0".
        } else {
            // Map details
            // Map details
            MapDetailData m = (MapDetailData) data;

            // Description Panel
            contentTable.table(d -> {
                d.background(mindustry.ui.Styles.black6).margin(10f);
                d.add("@description").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
                d.add(m.description()).wrap().color(mindustry.graphics.Pal.lightishGray).growX();
            }).growX().padBottom(10f).row();

            // Tags Panel
            contentTable.table(tg -> {
                tg.background(mindustry.ui.Styles.black6).margin(10f);
                tg.add("@tags").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
                tg.table(t -> {
                    t.left();
                    if (m.tags() != null) {
                        for (mindustrytool.plugins.browser.TagData tag : m.tags()) {
                            t.button(tag.name(), mindustry.ui.Styles.flatBordert, () -> {
                            })
                                    .padRight(5).padBottom(5).height(30f); // Adjust chip height
                        }
                    }
                }).left().growX();
            }).growX().padBottom(10f).row();

            // Rules Panel
            Table rulesContainer = new Table();
            rulesContainer.background(mindustry.ui.Styles.black6).margin(10f);
            rulesContainer.add("Waves & Rules").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();

            Table rulesTable = new Table();
            rulesTable.left();
            rulesTable.add("Analyzing map data...").color(mindustry.graphics.Pal.accent);
            rulesContainer.add(rulesTable).growX();

            contentTable.add(rulesContainer).growX().padBottom(10f).row();

            // Download and parse map
            mindustrytool.plugins.browser.Api.downloadMap(id, bytes -> {
                try {
                    mindustry.game.Rules rules = mindustrytool.plugins.browser.MapUtils.readRules(bytes);
                    Core.app.post(() -> {
                        rulesTable.clear();
                        if (rules == null) {
                            rulesTable.add("Analysis failed.").color(mindustry.graphics.Pal.remove);
                        } else {
                            // Win Condition
                            String mode = rules.attackMode ? "Attack" : (rules.pvp ? "PvP" : "Survival");
                            rulesTable.add("Mode: " + mode).color(mindustry.graphics.Pal.lightishGray).left()
                                    .padBottom(5f).row();

                            // Spawns
                            if (rules.spawns.isEmpty()) {
                                rulesTable.add("No waves defined.").color(mindustry.graphics.Pal.gray).left();
                            } else {
                                rulesTable.add("Waves:").color(mindustry.graphics.Pal.accent).left().padBottom(5f)
                                        .row();
                                Table waveList = new Table();
                                waveList.left();
                                int i = 0;
                                for (mindustry.game.SpawnGroup group : rules.spawns) {
                                    if (group.type == null)
                                        continue;

                                    waveList.image(group.type.uiIcon).size(24f).padRight(5f);
                                    waveList.add("x" + group.unitAmount).padRight(5f)
                                            .color(mindustry.graphics.Pal.lightishGray);

                                    String range = (group.end > 9999) ? (group.begin + "+")
                                            : (group.begin + "-" + group.end);

                                    waveList.add("[gray]W:[] " + range).padRight(15f)
                                            .color(mindustry.graphics.Pal.lightishGray);

                                    if (++i % 2 == 0)
                                        waveList.row();
                                }
                                rulesTable.add(waveList).left();
                            }
                        }
                    });
                } catch (Exception e) {
                    Core.app.post(() -> {
                        rulesTable.clear();
                        rulesTable.add("Error: " + e.getMessage()).color(mindustry.graphics.Pal.remove);
                    });
                }
            });
        }

    }

    private void buildSchematicDetails(mindustry.game.Schematic sc) {
        contentTable.clear();
        contentTable.top().left();

        // Power Stats
        float produced = sc.powerProduction();
        float consumed = sc.powerConsumption();
        float net = produced - consumed;

        // Power Stats Panel
        Table powerContainer = new Table();
        powerContainer.background(mindustry.ui.Styles.black6).margin(10f);

        Table powerTable = new Table();
        powerTable.left();
        powerTable.add(new arc.scene.ui.Image(mindustry.gen.Icon.power)).color(mindustry.graphics.Pal.power)
                .padRight(5f);

        if (Math.abs(produced) < 0.01 && Math.abs(consumed) < 0.01) {
            powerTable.add("No Power Usage").color(mindustry.graphics.Pal.gray);
        } else {
            powerTable.add("+" + (int) (produced * 60)).color(mindustry.graphics.Pal.heal).padRight(10f);
            powerTable.add("-" + (int) (consumed * 60)).color(mindustry.graphics.Pal.remove).padRight(10f);
            powerTable.add((net >= 0 ? "+" : "") + (int) (net * 60))
                    .color(net >= 0 ? mindustry.graphics.Pal.heal : mindustry.graphics.Pal.remove);
        }
        powerContainer.add("Power Analysis").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
        powerContainer.add(powerTable).left();
        contentTable.add(powerContainer).growX().padBottom(10f).row();

        // Requirements Panel
        Table reqContainer = new Table();
        reqContainer.background(mindustry.ui.Styles.black6).margin(10f);
        reqContainer.add("@requirements").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();

        reqContainer.table(t -> {
            t.left();
            int i = 0;
            for (mindustry.type.ItemStack stack : sc.requirements()) {
                t.image(stack.item.uiIcon).size(24f).padRight(4f);
                t.add(String.valueOf(stack.amount)).color(mindustry.graphics.Pal.lightishGray).padRight(10f);

                if (++i % 4 == 0)
                    t.row();
            }
        }).left().growX();
        contentTable.add(reqContainer).growX().padBottom(10f).row();
    }

    private void showComments(Object data, String id) {
        contentTable.clear();
        contentTable.top().left().margin(10f);

        mindustrytool.plugins.browser.PagingRequest<mindustrytool.plugins.browser.CommentData> request = mindustrytool.plugins.browser.Api
                .getCommentsRequest(
                        data instanceof MapDetailData ? "map" : "schematic", id);

        Table commentsList = new Table();
        commentsList.top().left();

        arc.scene.ui.ScrollPane pane = new arc.scene.ui.ScrollPane(commentsList);
        // Fade scroll bars, basic setup
        pane.setFadeScrollBars(false);

        contentTable.add(pane).grow().row();

        // Input Area
        Table inputTable = new Table();
        if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() != null) {
            arc.scene.ui.TextArea area = new arc.scene.ui.TextArea("");
            area.setMessageText("Write a comment...");

            inputTable.add(area).growX().height(60f).padRight(10f);
            inputTable.button(mindustry.gen.Icon.upload, mindustry.ui.Styles.defaulti, () -> {
                String text = area.getText();
                if (text == null || text.trim().isEmpty())
                    return;

                loading(true);
                mindustrytool.plugins.browser.Api.postComment(
                        data instanceof MapDetailData ? "map" : "schematic", id, text, success -> {
                            loading(false);
                            if (success) {
                                area.setText("");
                                // Refresh
                                showComments(data, id);
                            } else {
                                mindustry.Vars.ui.showInfo("Failed to post comment.");
                            }
                        });
            }).size(48f);
        } else {
            inputTable.add("Log in to post comments.").color(mindustry.graphics.Pal.gray);
        }
        contentTable.add(inputTable).growX().padTop(5f);

        // Load Comments
        request.getPage(items -> {
            Core.app.post(() -> {
                commentsList.clear();
                if (items == null || items.isEmpty()) {
                    commentsList.add("No comments yet.").color(mindustry.graphics.Pal.gray).pad(10f);
                } else {
                    for (mindustrytool.plugins.browser.CommentData c : items) {
                        addComment(commentsList, c);
                    }
                }
            });
        });
    }

    private void addComment(Table table, mindustrytool.plugins.browser.CommentData c) {
        Table row = new Table();
        row.background(mindustry.gen.Tex.pane).margin(5f);

        // Avatar/User
        row.add(new arc.scene.ui.Image(mindustry.gen.Icon.players)).size(24f).color(mindustry.graphics.Pal.accent)
                .padRight(10f).top();

        Table content = new Table();
        content.left().top();
        content.add(c.user() != null ? c.user().name() : "Unknown").color(mindustry.graphics.Pal.accent).fontScale(0.9f)
                .left().row();
        content.add(c.content()).color(mindustry.graphics.Pal.lightishGray).wrap().growX().left();

        row.add(content).growX().top();
        table.add(row).growX().padBottom(5f).row();
    }

    private void loading(boolean load) {
        if (load)
            mindustry.Vars.ui.loadfrag.show("Posting...");
        else
            mindustry.Vars.ui.loadfrag.hide();
    }

    private void loadAvatar(String url, arc.scene.ui.Image image) {
        if (url == null || url.isEmpty())
            return;
        arc.util.Http.get(url, res -> {
            byte[] bytes = res.getResult();
            if (bytes != null && bytes.length > 0) {
                Core.app.post(() -> {
                    try {
                        arc.graphics.Pixmap p = new arc.graphics.Pixmap(bytes);
                        image.setDrawable(new arc.scene.style.TextureRegionDrawable(
                                new arc.graphics.g2d.TextureRegion(new arc.graphics.Texture(p))));
                    } catch (Exception e) {
                        // ignore invalid images
                    }
                });
            }
        }, e -> {
        });
    }

    private float mobileWidth() {
        return Math.min(Core.graphics.getWidth() - 40f, 400f);
    }

    private Table longIcon(arc.scene.style.TextureRegionDrawable icon, Long value) {
        Table t = new Table();
        t.add(new arc.scene.ui.Image(icon)).size(20f).padRight(4f).color(mindustry.graphics.Pal.lightishGray);
        t.add(String.valueOf(value != null ? value : 0)).color(mindustry.graphics.Pal.lightishGray);
        return t;
    }
}
