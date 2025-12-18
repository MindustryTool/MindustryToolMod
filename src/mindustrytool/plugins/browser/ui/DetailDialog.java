package mindustrytool.plugins.browser.ui;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;

import mindustrytool.plugins.browser.*;

public class DetailDialog extends BaseBrowserDialog {
    private final Table heroTable = new Table();
    private final Table actionTable = new Table();
    private final Table tabsTable = new Table();
    private final Table contentTable = new Table();

    public DetailDialog() {
        super("");
        setupUI();
    }

    private void setupUI() {
        cont.clear();
        // Main Container
        cont.pane(t -> {
            t.top();
            // 1. Hero Section (Preview + Quick Stats + Desc + Tags)
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

    private float mobileWidth() {
        return Math.min(Core.graphics.getWidth() - 40f, 400f);
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

    public void show(SchematicDetailData data) {
        buildHero(data);
        buildActionBar(data, data.id());
        buildTabs(data, data.id());
        show();
    }

    public void show(MapDetailData data) {
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
        final String authorId;
        String name;
        String description;
        Seq<TagData> tags;
        int width, height;
        String createdAt, status, verifiedBy;

        if (data instanceof MapDetailData) {
            MapDetailData m = (MapDetailData) data;
            authorId = m.createdBy();
            id = m.id();
            name = m.name();
            description = m.description();
            tags = m.tags();
            width = m.width();
            height = m.height();
            createdAt = m.createdAt();
            status = m.status();
            verifiedBy = m.verifiedBy();
        } else {
            SchematicDetailData s = (SchematicDetailData) data;
            authorId = s.createdBy();
            id = s.id();
            name = s.name();
            description = s.description();
            tags = s.tags();
            width = s.width();
            height = s.height();
            createdAt = s.createdAt();
            status = s.status();
            verifiedBy = s.verifiedBy();
        }

        if (name == null)
            name = "Untitled";

        boolean mobile = Core.graphics.isPortrait();

        Table previewTable = new Table();
        mindustrytool.plugins.browser.ImageHandler.ImageType imgType = (data instanceof MapDetailData)
                ? mindustrytool.plugins.browser.ImageHandler.ImageType.MAP
                : mindustrytool.plugins.browser.ImageHandler.ImageType.SCHEMATIC;

        previewTable.add(new mindustrytool.plugins.browser.ImageHandler(id, imgType))
                .size(mobile ? mobileWidth() : 400).pad(10f);

        Table infoTable = new Table();
        infoTable.top().left().margin(10f);

        // 1. Name
        infoTable.add(name).fontScale(1.3f).color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();

        // 2. Description
        if (description != null && !description.isEmpty()) {
            infoTable.add(description).wrap().color(mindustry.graphics.Pal.lightishGray).growX().padBottom(10f).left()
                    .row();
        }

        // 3. Size
        infoTable.add("Size: " + width + "x" + height).color(mindustry.graphics.Pal.gray).left().padBottom(10f).row();

        // 4. Author
        Table authorTable = new Table();
        authorTable.left();
        arc.scene.ui.Image avatarImage = new arc.scene.ui.Image(mindustry.gen.Icon.players);
        authorTable.add(avatarImage).size(24).padRight(5);
        arc.scene.ui.Label authorLabel = new arc.scene.ui.Label("Loading...");
        authorLabel.setColor(mindustry.graphics.Pal.lightishGray);
        authorTable.add(authorLabel).left();
        infoTable.add(authorTable).left().padBottom(10f).row();
        loadUser(authorId, avatarImage, authorLabel);

        // 5. Tags
        if (tags != null && !tags.isEmpty()) {
            TagRenderer.sortTags(tags);
            Table tagTable = new Table();
            tagTable.left();
            for (TagData t : tags) {
                TagRenderer.render(tagTable, t, 1f, () -> {
                });
            }
            infoTable.add(tagTable).left().growX().padBottom(10f).row();
        }

        // 6 & 7. Requirements & Power (Schematics only)
        if (data instanceof SchematicDetailData) {
            SchematicDetailData s = (SchematicDetailData) data;

            // Requirements Container
            Table reqContainer = new Table();
            reqContainer.left();
            infoTable.add(reqContainer).growX().padBottom(10f).row();

            // Power Container
            Table powerContainer = new Table();
            powerContainer.left();
            infoTable.add(powerContainer).growX().padBottom(10f).row();

            // Initial Requirement Preview
            if (s.meta() != null && s.meta().requirements() != null) {
                populateRequirements(reqContainer, s.meta().requirements());
            }

            // Async Load Full Details
            mindustrytool.plugins.browser.Api.downloadSchematic(id, bytes -> {
                try {
                    String schemString = new String(arc.util.serialization.Base64Coder.encode(bytes));
                    mindustry.game.Schematic sc = mindustrytool.plugins.browser.SchematicUtils
                            .readSchematic(schemString);
                    if (sc != null) {
                        Core.app.post(() -> {
                            populatePower(powerContainer, sc);
                            // Also update requirements with exact data if needed, but meta is usually fine.
                            // If we want to be exact:
                            populateRequirements(reqContainer, sc.requirements());
                        });
                    }
                } catch (Exception e) {
                    // ignore or show error in power container
                }
            });
        }

        // 8. Created At
        arc.scene.ui.Label createdAtLabel = new arc.scene.ui.Label(createdAt != null ? "Created At: " + createdAt : "");
        createdAtLabel.setColor(mindustry.graphics.Pal.gray);
        if (createdAt != null)
            infoTable.add(createdAtLabel).left().padBottom(5f).row();
        else
            infoTable.add(createdAtLabel).left().padBottom(5f).row();

        // 9. Status
        arc.scene.ui.Label statusLabel = new arc.scene.ui.Label(status != null ? "Status: " + status : "");
        statusLabel.setColor(status != null ? statusColor(status) : mindustry.graphics.Pal.accent);
        if (status != null)
            infoTable.add(statusLabel).left().padBottom(5f).row();
        else
            infoTable.add(statusLabel).left().padBottom(5f).row();

        // 10. Verified By
        Table verifierTable = new Table();
        verifierTable.left();
        verifierTable.add("Verified By: ").color(mindustry.graphics.Pal.gray).padRight(5f);

        arc.scene.ui.Image vAvatar = new arc.scene.ui.Image(mindustry.gen.Icon.players);
        verifierTable.add(vAvatar).size(24).padRight(5);

        arc.scene.ui.Label vLabel = new arc.scene.ui.Label("-");
        vLabel.setColor(mindustry.graphics.Pal.lightishGray);
        verifierTable.add(vLabel).left();

        infoTable.add(verifierTable).left().padBottom(10f).row();
        if (verifiedBy != null) {
            loadUser(verifiedBy, vAvatar, vLabel);
        }

        // Fetch Full Data to populate missing fields
        if (data instanceof SchematicDetailData) {
            mindustrytool.plugins.browser.Api.findSchematicById(id, fullData -> {
                if (fullData != null) {
                    Core.app.post(() -> {
                        if (fullData.createdAt() != null)
                            createdAtLabel.setText("Created At: " + fullData.createdAt());
                        if (fullData.status() != null) {
                            statusLabel.setText("Status: " + fullData.status());
                            statusLabel.setColor(statusColor(fullData.status()));
                        }
                        if (fullData.verifiedBy() != null) {
                            loadUser(fullData.verifiedBy(), vAvatar, vLabel);
                        }
                    });
                }
            });
        }

        if (mobile) {
            heroTable.add(previewTable).row();
            heroTable.add(infoTable).growX();
        } else {
            heroTable.add(previewTable).top();
            heroTable.add(infoTable).growX().top();
        }
    }

    private void loadUser(String userId, arc.scene.ui.Image avatar, arc.scene.ui.Label nameLabel) {
        if (userId != null) {
            mindustrytool.plugins.browser.Api.findUserById(userId, user -> {
                if (user != null) {
                    Core.app.post(() -> {
                        nameLabel.setText(user.name());
                        if (user.imageUrl() != null && !user.imageUrl().isEmpty()) {
                            loadAvatar(user.imageUrl(), avatar);
                        }
                    });
                } else {
                    Core.app.post(() -> nameLabel.setText("Unknown"));
                }
            });
        } else {
            nameLabel.setText("Unknown");
        }
    }

    private arc.graphics.Color statusColor(String status) {
        if ("verified".equalsIgnoreCase(status))
            return mindustry.graphics.Pal.heal;
        if ("rejected".equalsIgnoreCase(status))
            return mindustry.graphics.Pal.remove;
        return mindustry.graphics.Pal.accent;
    }

    private void populateRequirements(Table container, Iterable<?> reqs) {
        container.clear();
        container.add("@requirements").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
        container.table(t -> {
            t.left();
            int i = 0;
            // Handle both SchematicRequirement and ItemStack (bit hacky or use overloads)
            // Just assume we can iterate and check type or use helpers
            for (Object o : reqs) {
                int amount = 0;
                arc.scene.ui.Image icon = null;

                if (o instanceof mindustry.type.ItemStack) {
                    mindustry.type.ItemStack stack = (mindustry.type.ItemStack) o;
                    amount = stack.amount;
                    icon = new arc.scene.ui.Image(stack.item.uiIcon);
                    // usage of name removed
                    mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement r = (mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement) o;
                    amount = r.amount();
                    mindustry.type.Item item = mindustry.Vars.content.items()
                            .find(it -> it.name.equalsIgnoreCase(r.name()));
                    if (item == null)
                        item = mindustry.Vars.content.items().find(it -> it.localizedName.equalsIgnoreCase(r.name()));

                    if (item != null)
                        icon = new arc.scene.ui.Image(item.uiIcon);
                    else {
                        icon = new arc.scene.ui.Image();
                        icon.setColor(arc.graphics.Color.valueOf(r.color() != null ? r.color() : "ffffff"));
                    }
                }

                if (icon != null) {
                    t.add(icon).size(24f).padRight(4f);
                    t.add(String.valueOf(amount)).color(mindustry.graphics.Pal.lightishGray).padRight(10f);
                    if (++i % 4 == 0)
                        t.row();
                }
            }
        }).left().growX();
    }

    private void populatePower(Table container, mindustry.game.Schematic sc) {
        container.clear();
        float produced = sc.powerProduction();
        float consumed = sc.powerConsumption();
        float net = produced - consumed;

        Table powerTable = new Table();
        powerTable.left();
        powerTable.add(new arc.scene.ui.Image(mindustry.gen.Icon.power)).color(mindustry.graphics.Pal.power)
                .padRight(5f);

        if (produced > 0)
            powerTable.add("+" + (int) (produced * 60)).color(mindustry.graphics.Pal.heal).padRight(10f);
        if (consumed > 0)
            powerTable.add("-" + (int) (consumed * 60)).color(mindustry.graphics.Pal.remove).padRight(10f);

        powerTable.add((net >= 0 ? "+" : "") + (int) (net * 60))
                .color(net >= 0 ? mindustry.graphics.Pal.heal : mindustry.graphics.Pal.remove);

        container.add("Power Consumption/Production").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
        container.add(powerTable).left();
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
                    mindustry.Vars.ui.showInfoFade("ID Copied");
                })
                .tooltip("Copy ID");

        // Download / Copy
        if (data instanceof MapDetailData) {
            actionTable.button(mindustry.gen.Icon.download, mindustry.ui.Styles.defaulti,
                    () -> {
                        mindustrytool.plugins.browser.ContentHandler.downloadMap(
                                new mindustrytool.plugins.browser.ContentData().id(((MapDetailData) data).id()));
                    })
                    .tooltip("@download");
        } else {
            actionTable.button(mindustry.gen.Icon.copy, mindustry.ui.Styles.defaulti,
                    () -> {
                        mindustrytool.plugins.browser.ContentHandler.copySchematic(
                                new mindustrytool.plugins.browser.ContentData().id(((SchematicDetailData) data).id()));
                    })
                    .tooltip("@schematic.copy");
        }

        // Vote Up
        actionTable.button(mindustry.gen.Icon.upOpen, mindustry.ui.Styles.defaulti,
                () -> {
                    if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() == null) {
                        mindustry.Vars.ui.showInfo("Please login first.");
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
        tabsTable.background(mindustry.ui.Styles.black6);

        // Tab Buttons
        mindustry.ui.Styles.flatTogglet.fontColor = mindustry.graphics.Pal.accent;
        arc.scene.ui.ButtonGroup<arc.scene.ui.Button> group = new arc.scene.ui.ButtonGroup<>();

        tabsTable.button("@details", mindustry.ui.Styles.flatTogglet, () -> showDetails(data, id))
                .group(group).checked(true).growX().height(50f);

        tabsTable.button("@comments", mindustry.ui.Styles.flatTogglet, () -> showComments(data, id))
                .group(group).growX().height(50f);

        // Initial show
        showDetails(data, id);
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
        pane.setFadeScrollBars(false);

        // Post Comment
        Table inputTable = new Table();
        arc.scene.ui.TextArea area = new arc.scene.ui.TextArea("");
        area.setMessageText("Write a comment...");
        inputTable.add(area).growX().height(60f).padRight(5f);
        inputTable.button(mindustry.gen.Icon.pencil, mindustry.ui.Styles.defaulti, () -> {
            String text = area.getText();
            if (text == null || text.trim().isEmpty())
                return;

            if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() == null) {
                mindustry.Vars.ui.showInfo("Login required.");
                return;
            }

            loading(true, "Posting...");
            mindustrytool.plugins.browser.Api.postComment(
                    data instanceof MapDetailData ? "map" : "schematic", id, text,
                    success -> {
                        loading(false);
                        if (success) {
                            area.setText("");
                            mindustry.Vars.ui.showInfoFade("Comment posted!");
                            // Refresh
                            showComments(data, id);
                        } else {
                            mindustry.Vars.ui.showInfo("Failed to post comment.");
                        }
                    });
        }).size(48f);

        if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() == null) {
            inputTable.touchable = arc.scene.event.Touchable.disabled;
            area.setMessageText("Login to comment");
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

        contentTable.row();
        contentTable.add(pane).grow().padTop(10f);
    }

    // Removed buildSchematicDetails as it is now integrated into buildHero helpers

    private void showDetails(Object data, String id) {
        // For Schematics, details are now in Hero.
        if (data instanceof SchematicDetailData) {
            // No extra content in details tab for currently.
            // Maybe show message
            contentTable.clear();
            contentTable.add("See info above.").color(mindustry.graphics.Pal.gray);
        } else {
            // Map details
            contentTable.clear();
            contentTable.top().left().margin(10f);

            // Map details (Rules) - Keep this as it is large
            // ... (rest of map logic)

            // Rules Panel
            Table rulesContainer = new Table();
            rulesContainer.background(mindustry.ui.Styles.black6).margin(10f);
            rulesContainer.add("Waves & Rules").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();

            Table rulesTable = new Table();
            rulesTable.left();
            rulesTable.add("Analyzing map data...").color(mindustry.graphics.Pal.accent);
            rulesContainer.add(rulesTable).growX();

            contentTable.add(rulesContainer).growX().padBottom(10f).row();

            mindustrytool.plugins.browser.Api.downloadMap(id, bytes -> {
                if (bytes == null || bytes.length == 0) {
                    Core.app.post(() -> {
                        rulesTable.clear();
                        rulesTable.add("Preview unavailable (No Data)").color(mindustry.graphics.Pal.gray);
                    });
                    return;
                }

                try {
                    mindustry.game.Rules rules = mindustrytool.plugins.browser.MapUtils.readRules(bytes);
                    Core.app.post(() -> {
                        rulesTable.clear();
                        if (rules == null) {
                            rulesTable.add("Analysis failed.").color(mindustry.graphics.Pal.remove);
                        } else {
                            String mode = rules.attackMode ? "Attack" : (rules.pvp ? "PvP" : "Survival");
                            rulesTable.add("Mode: " + mode).color(mindustry.graphics.Pal.lightishGray).left()
                                    .padBottom(5f).row();

                            if (rules.spawns.isEmpty()) {
                                rulesTable.add("No waves defined.").color(mindustry.graphics.Pal.gray).left();
                            } else {
                                rulesTable.add("Waves:").color(mindustry.graphics.Pal.accent).left().padBottom(5f)
                                        .row();
                                Table waveList = new Table();
                                waveList.left();
                                int i = 0;
                                int count = 0;
                                for (mindustry.game.SpawnGroup group : rules.spawns) {
                                    if (group.type == null)
                                        continue;

                                    if (count++ > 10) {
                                        if (count == 12) {
                                            waveList.add("...").color(mindustry.graphics.Pal.gray).colspan(3).left();
                                        }
                                        continue;
                                    }

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

    public void addCloseButton() {
        actionTable.button(mindustry.gen.Icon.left, mindustry.ui.Styles.defaulti, this::hide)
                .size(48f).right().padLeft(10f);
    }
}
