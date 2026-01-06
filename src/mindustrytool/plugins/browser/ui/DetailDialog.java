package mindustrytool.plugins.browser.ui;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;

import mindustrytool.plugins.browser.*;

public class DetailDialog extends BaseBrowserDialog {
    private final Table heroTable = new Table();
    private final Table actionTable = new Table();
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

            // 3. Content (Comments)
            t.add(contentTable).grow().row();
        }).grow().scrollX(false);

        addCloseButton();
    }

    private float mobileWidth() {
        return Math.min(Core.graphics.getWidth() - 40f, 400f);
    }

    private void addComment(Table table, mindustrytool.plugins.browser.CommentData c) {
        Table row = new Table();
        row.background(mindustry.gen.Tex.pane).margin(5f);

        // Avatar/User
        if (c.user() != null && c.user().imageUrl() != null && !c.user().imageUrl().isEmpty()) {
            row.add(new NetworkImage(c.user().imageUrl())).size(24f).padRight(10f).top();
        } else {
            row.add(new arc.scene.ui.Image(mindustry.gen.Icon.players)).size(24f).color(mindustry.graphics.Pal.accent)
                    .padRight(10f).top();
        }

        Table content = new Table();
        content.left().top();
        content.add(c.user() != null ? c.user().name() + "[]" : "Unknown").color(mindustry.graphics.Pal.accent)
                .fontScale(0.9f)
                .left().row();
        content.add(c.content() + "[]").color(mindustry.graphics.Pal.lightishGray).wrap().growX().left();

        row.add(content).growX().top();
        table.add(row).growX().padBottom(5f).row();
    }

    public void show(SchematicDetailData data) {
        buildHero(data);
        buildActionBar(data, data.id());
        showComments(data, data.id());
        show();
    }

    public void show(MapDetailData data) {
        buildHero(data);
        buildActionBar(data, data.id());
        showComments(data, data.id());
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

        boolean mobile = Core.graphics.isPortrait() || Core.graphics.getWidth() < 800;

        Table previewTable = new Table();
        mindustrytool.plugins.browser.ImageHandler.ImageType imgType = (data instanceof MapDetailData)
                ? mindustrytool.plugins.browser.ImageHandler.ImageType.MAP
                : mindustrytool.plugins.browser.ImageHandler.ImageType.SCHEMATIC;

        previewTable.add(new mindustrytool.plugins.browser.ImageHandler(id, imgType))
                .size(mobile ? mobileWidth() : 400).pad(10f);

        Table infoTable = new Table();
        infoTable.top().left().margin(10f);
        Table descTable = new Table();

        // --- COMMON COMPONENTS ---

        // 1. Header (Name + Actions)
        Table headerTable = new Table();
        headerTable.left();
        headerTable.add(name + "[]").fontScale(1.3f).color(mindustry.graphics.Pal.accent).left();

        headerTable.button(mindustry.gen.Icon.copy, mindustry.ui.Styles.clearNonei, () -> {
            Core.app.setClipboardText(id);
            mindustry.Vars.ui.showInfoFade("ID Copied");
        }).size(32).padLeft(10).tooltip("Copy ID");

        // Open Web Button
        headerTable
                .button(mindustry.gen.Icon.link, mindustry.ui.Styles.clearNonei,
                        () -> Core.app.openURI(mindustrytool.plugins.browser.Config.WEB_URL +
                                (data instanceof MapDetailData ? "/maps/" : "/schematics/") + id))
                .size(32).padLeft(5).tooltip("@open");

        // 2. Author
        Table authorTable = createAuthorTable(authorId);

        // 3. Tags
        Table tagTable = new Table();
        tagTable.left().defaults().padRight(4);
        arc.scene.ui.ScrollPane tagPane = new arc.scene.ui.ScrollPane(tagTable);
        tagPane.setScrollingDisabled(false, true);
        tagPane.setFadeScrollBars(false);
        tagPane.setOverscroll(true, false);

        if (tags != null && !tags.isEmpty()) {
            ensureTagsLoaded(data, () -> {
                tagTable.clear();
                TagRenderer.sortTags(tags);
                for (TagData t : tags) {
                    TagRenderer.render(tagTable, t, 1f, () -> {
                    });
                }
            });
        }

        // 4. Requirements & Power
        final Table reqContainer = new Table();
        final Table powerContainer = new Table();
        reqContainer.left();
        powerContainer.left();

        // Populate Reqs/Power Logic (Common)
        if (data instanceof SchematicDetailData) {
            SchematicDetailData s = (SchematicDetailData) data;
            if (s.meta() != null) {
                if (s.meta().requirements() != null)
                    populateRequirements(reqContainer, s.meta().requirements());
                if (s.meta().powerProduction() != null || s.meta().powerConsumption() != null)
                    populatePower(powerContainer, s.meta().powerProduction(), s.meta().powerConsumption());
            }
            mindustrytool.plugins.browser.Api.downloadSchematic(id, bytes -> {
                try {
                    if (bytes == null || bytes.length == 0)
                        return;
                    String schemString = new String(arc.util.serialization.Base64Coder.encode(bytes));
                    mindustry.game.Schematic sc = mindustrytool.plugins.browser.SchematicUtils
                            .readSchematic(schemString);
                    if (sc != null) {
                        Core.app.post(() -> {
                            populatePower(powerContainer, sc);
                            populateRequirements(reqContainer, sc.requirements());
                        });
                    }
                } catch (Exception e) {
                }
            });
        }

        // 5. Footer (Status + Verifier)
        Table footerTable = new Table();
        footerTable.left();
        if (status != null) {
            Table statusBadge = new Table();
            statusBadge.background(mindustry.ui.Styles.flatBordert.up);
            statusBadge.add(status.toUpperCase()).color(statusColor(status)).pad(2, 6, 2, 6).fontScale(0.8f);
            footerTable.add(statusBadge).padRight(10);
        }
        if (verifiedBy != null) {
            footerTable.add("Verified by: ").color(mindustry.graphics.Pal.gray).fontScale(0.9f);
            Table vAvatarContainer = new Table();
            footerTable.add(vAvatarContainer).size(16).padRight(4).padLeft(4);
            vAvatarContainer.add(new arc.scene.ui.Image(mindustry.gen.Icon.players)).size(16);

            arc.scene.ui.Label vLabel = new arc.scene.ui.Label("...");
            vLabel.setColor(mindustry.graphics.Pal.lightishGray);
            vLabel.setFontScale(0.9f);
            footerTable.add(vLabel).left();
            loadUser(verifiedBy, vAvatarContainer, vLabel);
        }

        // --- LAYOUT CONSTRUCTION ---

        if (mobile) {
            // MOBILE LAYOUT (Vertical Stack)

            // 1. Title
            infoTable.add(headerTable).growX().left().row();

            // 2. Subtitle: "designed by [Author]"
            Table subTitle = new Table();
            subTitle.left();
            subTitle.add("designed by ").color(mindustry.graphics.Pal.gray).fontScale(0.85f);
            subTitle.add(authorTable);
            infoTable.add(subTitle).left().padBottom(10f).row();

            // 3. Size
            infoTable.add("Size").color(mindustry.graphics.Pal.gray).fontScale(1f).left().padBottom(2f).row();
            infoTable.add(width + "x" + height).color(mindustry.graphics.Pal.lightishGray).left().padBottom(10f).row();

            // 4. Author (Detailed? No, already in subtitle. Skip or keep brief if needed.
            // Screenshot showed "Author" header)
            // Screenshot showed "Author" header then avatar+name.
            // Let's follow screenshot: "designed by Oct" (subtitle) AND "Author" section?
            // Actually screenshot says:
            // "steam gen but not more use"
            // "designed by Oct"
            // "Size" -> 10x12
            // "Author" -> [Avatar] the_oct.
            // So Author IS repeated. I will replicate this.

            infoTable.add("Author").color(mindustry.graphics.Pal.gray).fontScale(1f).left().padBottom(2f).row();
            infoTable.add(createAuthorTable(authorId)).left().padBottom(10f).row();

            // Description (Collapsible)
            infoTable.add(descTable).growX().left().padBottom(10f).row();
            buildDescription(descTable, description);

            // 5. Tags
            infoTable.add("Tags").color(mindustry.graphics.Pal.gray).fontScale(1f).left().padBottom(2f).row();
            infoTable.add(tagPane).growX().left().padBottom(10f).row();

            // 6. Requirements
            infoTable.add("Requirements").color(mindustry.graphics.Pal.gray).fontScale(1f).left().padBottom(2f).row();
            infoTable.add(reqContainer).growX().left().padBottom(5f).row();
            infoTable.add(powerContainer).growX().left().padBottom(10f).row();

            // Footer
            infoTable.add(footerTable).left().padTop(10f).row();

        } else {
            // DESKTOP LAYOUT (Compact Grid)

            // Row 1: Header
            infoTable.add(headerTable).growX().padBottom(5f).row();

            // Row 2: Metadata (Author • Date • Size)
            Table metaTable = new Table();
            metaTable.left();
            metaTable.add(authorTable).padRight(10); // Reusing authorTable instance
            // Date
            String timeStr = mindustrytool.plugins.browser.SchematicUtils.parseRelativeTime(createdAt);
            metaTable.add(new arc.scene.ui.Image(mindustry.gen.Icon.info)).size(16).padRight(4)
                    .color(mindustry.graphics.Pal.gray);
            metaTable.add(timeStr).color(mindustry.graphics.Pal.gray).padRight(10);
            // Size
            metaTable.add(width + "x" + height).color(mindustry.graphics.Pal.gray);
            infoTable.add(metaTable).left().padBottom(10f).row();

            // Row 3: Description
            infoTable.add(descTable).growX().left().padBottom(10f).row();
            buildDescription(descTable, description);

            // Row 4: Tags
            infoTable.add(tagPane).left().growX().padBottom(10f).row();

            // Row 5: Reqs + Power
            infoTable.add(powerContainer).growX().padBottom(5f).row();
            infoTable.add(reqContainer).growX().padBottom(10f).row();

            // Row 6: Footer
            infoTable.add(footerTable).left().padTop(10f).row();
        }

        // Fetch Full Data Listener (Just logical update, UI containers already bound)
        if (data instanceof SchematicDetailData) {
            mindustrytool.plugins.browser.Api.findSchematicById(id, fullData -> {
                if (fullData != null) {
                    Core.app.post(() -> {
                        if (fullData.meta() != null) {
                            if (fullData.meta().requirements() != null)
                                populateRequirements(reqContainer, fullData.meta().requirements());
                            if (fullData.meta().powerProduction() != null || fullData.meta().powerConsumption() != null)
                                populatePower(powerContainer, fullData.meta().powerProduction(),
                                        fullData.meta().powerConsumption());
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

    private void loadUser(String userId, Table avatarContainer, arc.scene.ui.Label nameLabel) {
        if (userId != null) {
            // First try standard fetch
            mindustrytool.plugins.browser.Api.findUserById(userId, user -> {
                if (user != null) {
                    Core.app.post(() -> {
                        if (user.name() != null && !user.name().isEmpty()) {
                            nameLabel.setText(user.name() + "[]");
                        } else {
                            // Name missing in standard model, likely different JSON key.
                            // Trigger fallback fetch logic below...
                            nameLabel.setText("Finding name...");
                        }
                        if (user.imageUrl() != null && !user.imageUrl().isEmpty()) {
                            avatarContainer.clearChildren();
                            avatarContainer.add(new NetworkImage(user.imageUrl())).size(16);
                        }
                    });
                } else {
                    Core.app.post(() -> nameLabel.setText("Unknown"));
                }
            });

            // Parallel JSON fetch to find the name key
            mindustrytool.plugins.browser.ApiRequest.getJval(
                    mindustrytool.plugins.browser.Config.API_URL + "users/" + userId,
                    json -> {
                        Core.app.post(() -> {
                            // CHECK FOR NULL (API Error)
                            if (json == null) {
                                nameLabel.setText(nameLabel.getText() + " (API Error)");
                                return;
                            }

                            // Heuristic to find name
                            String bestName = null;
                            if (json.isObject()) {
                                for (arc.struct.ObjectMap.Entry<String, arc.util.serialization.Jval> e : json
                                        .asObject()) {
                                    String key = e.key.toLowerCase();
                                    if (key.equals("name") || key.equals("username") || key.equals("login")
                                            || key.equals("nickname")) {
                                        arc.util.serialization.Jval val = e.value;
                                        if (val.isString()) {
                                            bestName = val.asString();
                                            break; // Found a likely candidate
                                        }
                                    }
                                }
                            }

                            if (bestName != null && !bestName.isEmpty()) {
                                nameLabel.setText(bestName);
                            } else {
                                // If still not found, show keys for manual debugging
                                StringBuilder sb = new StringBuilder("ID: " + userId + "\nKeys: ");
                                if (json.isObject()) {
                                    for (arc.struct.ObjectMap.Entry<String, arc.util.serialization.Jval> e : json
                                            .asObject()) {
                                        sb.append(e.key).append(", ");
                                    }
                                }
                                nameLabel.setText(sb.toString());
                            }
                        });
                    });

        } else {
            nameLabel.setText("Unknown (No ID)");
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
        container.add(Core.bundle.get("requirements", "Requirements")).color(mindustry.graphics.Pal.accent).left()
                .padBottom(5f).row();

        StringBuilder sb = new StringBuilder();

        for (Object o : reqs) {
            int amount = 0;
            String name = "?";
            String color = "ffffff";
            mindustry.type.Item item = null;

            if (o instanceof mindustry.type.ItemStack) {
                mindustry.type.ItemStack stack = (mindustry.type.ItemStack) o;
                amount = stack.amount;
                item = stack.item;
            } else if (o instanceof mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement) {
                mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement r = (mindustrytool.plugins.browser.SchematicDetailData.SchematicRequirement) o;
                amount = r.amount();
                name = r.name();
                color = r.color() != null ? r.color() : "ffffff";

                item = mindustry.Vars.content.items().find(it -> it.name.equalsIgnoreCase(r.name()));
                if (item == null)
                    item = mindustry.Vars.content.items().find(it -> it.localizedName.equalsIgnoreCase(r.name()));
            }

            if (item != null) {
                sb.append(item.emoji()).append("[lightgray]").append(amount).append("[]  ");
            } else {
                // Fallback for missing items: Colored Name
                sb.append("[#").append(color).append("]").append(name).append("[] [lightgray]").append(amount)
                        .append("[]  ");
            }
        }

        arc.scene.ui.Label label = new arc.scene.ui.Label(sb.toString());
        label.setWrap(true);
        label.setAlignment(arc.util.Align.left);
        label.setColor(arc.graphics.Color.white);

        container.add(label).growX().padRight(20f).left();
    }

    private void populatePower(Table container, mindustry.game.Schematic sc) {
        populatePower(container, sc.powerProduction(), sc.powerConsumption());
    }

    private void populatePower(Table container, Float producedVal, Float consumedVal) {
        container.clear();
        float produced = producedVal != null ? producedVal : 0f;
        float consumed = consumedVal != null ? consumedVal : 0f;

        // If no power involved, show nothing or explicit "None"?
        // Usually schematics without power don't need this section,
        // but we want to show it if metadata is present.

        Table powerTable = new Table();
        powerTable.left();
        powerTable.add(new arc.scene.ui.Image(mindustry.gen.Icon.power)).color(mindustry.graphics.Pal.power)
                .padRight(5f);

        boolean hasPower = false;
        if (produced > 0.001f) {
            powerTable.add("+" + (int) (produced * 60)).color(mindustry.graphics.Pal.heal).padRight(10f);
            hasPower = true;
        }
        if (consumed > 0.001f) {
            powerTable.add("-" + (int) (consumed * 60)).color(mindustry.graphics.Pal.remove).padRight(10f);
            hasPower = true;
        }

        if (!hasPower) {
            powerTable.add("0").color(mindustry.graphics.Pal.gray);
        }

        container.add("Power Stats").color(mindustry.graphics.Pal.accent).left().padBottom(5f).row();
        container.add(powerTable).left();
    }

    private void buildActionBar(Object data, String id) {
        actionTable.clear();
        actionTable.right().defaults().size(48).padLeft(8f);

        // Vote Up
        actionTable.button(mindustry.gen.Icon.upOpen, mindustry.ui.Styles.defaulti,
                () -> {
                    if (mindustrytool.plugins.browser.BrowserAuthService.getAccessToken() == null) {
                        mindustry.Vars.ui.showInfo("Please login first.");
                        return;
                    }
                    mindustrytool.plugins.browser.Api.vote(
                            data instanceof MapDetailData ? "map" : "schematic", id, error -> {
                                if (error == null) {
                                    mindustry.Vars.ui.showInfoFade("Voted!");
                                } else {
                                    mindustry.Vars.ui.showInfo("Vote failed.\n" + error);
                                }
                            });
                })
                .tooltip("Vote Up");

        // Schematics: Copy -> Download
        // Maps: Download only
        if (data instanceof MapDetailData) {
            actionTable.button(mindustry.gen.Icon.download, mindustry.ui.Styles.defaulti,
                    () -> {
                        mindustrytool.plugins.browser.ContentHandler.downloadMap(
                                new mindustrytool.plugins.browser.ContentData().id(((MapDetailData) data).id()));
                    })
                    .tooltip("@download");
        } else {
            // Copy Base64
            actionTable.button(mindustry.gen.Icon.paste, mindustry.ui.Styles.defaulti,
                    () -> {
                        mindustrytool.plugins.browser.ContentHandler.copySchematic(
                                new mindustrytool.plugins.browser.ContentData().id(((SchematicDetailData) data).id()));
                    })
                    .tooltip("@schematic.copy");

            // Download / Import
            actionTable.button(mindustry.gen.Icon.download, mindustry.ui.Styles.defaulti,
                    () -> {
                        mindustrytool.plugins.browser.ContentHandler.downloadSchematic(
                                new mindustrytool.plugins.browser.ContentData().id(((SchematicDetailData) data).id()));
                    })
                    .tooltip("@schematic.saved");
        }

        // Details Button
        actionTable.button(mindustry.gen.Icon.info, mindustry.ui.Styles.defaulti, () -> showDetailsPopup(data))
                .tooltip("Details");
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
                    error -> {
                        loading(false);
                        if (error == null) {
                            area.setText("");
                            mindustry.Vars.ui.showInfoFade("Comment posted!");
                            // Refresh
                            showComments(data, id);
                        } else {
                            mindustry.Vars.ui.showInfo("Failed to post comment.\n" + error);
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

    private void showDetailsPopup(Object data) {
        if (data instanceof SchematicDetailData) {
            mindustry.Vars.ui.showInfo("No additional details for schematics.");
            return;
        }

        String id = ((MapDetailData) data).id();

        BaseBrowserDialog dialog = new BaseBrowserDialog("Map Details");
        dialog.addCloseButton();

        Table contentTable = dialog.cont;
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

        dialog.show();
    }

    private Table createAuthorTable(String authorId) {
        Table t = new Table();
        t.left();
        Table avatarContainer = new Table();
        avatarContainer.add(new arc.scene.ui.Image(mindustry.gen.Icon.players)).size(16);
        arc.scene.ui.Label label = new arc.scene.ui.Label("...");
        label.setColor(mindustry.graphics.Pal.lightishGray);
        t.add(avatarContainer).size(16).padRight(4);
        t.add(label);
        loadUser(authorId, avatarContainer, label);
        return t;
    }

    private void buildDescription(Table container, String description) {
        container.clear();
        container.left();
        if (description == null || description.isEmpty())
            return;

        int limit = 150;
        if (description.length() <= limit) {
            container.add(description).wrap().color(mindustry.graphics.Pal.lightishGray).growX().left();
            return;
        }

        String shortDesc = description.substring(0, limit) + "...";
        showCollapsed(container, description, shortDesc);
    }

    private void showCollapsed(Table t, String full, String shortText) {
        t.clear();
        t.add(shortText).wrap().color(mindustry.graphics.Pal.lightishGray).growX().left().row();
        t.button("Read More", mindustry.gen.Icon.downOpen, () -> showExpanded(t, full, shortText))
                .left().padTop(4f).with(b -> {
                    b.setStyle(mindustry.ui.Styles.flatBordert);
                    b.getLabel().setFontScale(0.85f);
                    b.getLabel().setWrap(false);
                    b.getCells().first().padRight(8f);
                    b.margin(12f);
                    b.left();
                });
    }

    private void showExpanded(Table t, String full, String shortText) {
        t.clear();
        t.add(full).wrap().color(mindustry.graphics.Pal.lightishGray).growX().left().row();
        t.button("Show Less", mindustry.gen.Icon.upOpen, () -> showCollapsed(t, full, shortText))
                .left().padTop(4f).with(b -> {
                    b.setStyle(mindustry.ui.Styles.flatBordert);
                    b.getLabel().setFontScale(0.85f);
                    b.getLabel().setWrap(false);
                    b.getCells().first().padRight(8f);
                    b.margin(12f);
                    b.left();
                });
    }

    public void addCloseButton() {
        buttons.clear();
        buttons.button("@back", mindustry.gen.Icon.left, this::hide).size(210f, 64f);
    }

    private void ensureTagsLoaded(Object data, Runnable onDone) {
        mindustrytool.plugins.browser.TagService service = new mindustrytool.plugins.browser.TagService();
        mindustrytool.plugins.browser.TagService.TagCategoryEnum type = (data instanceof MapDetailData)
                ? mindustrytool.plugins.browser.TagService.TagCategoryEnum.maps
                : mindustrytool.plugins.browser.TagService.TagCategoryEnum.schematics;

        service.getTag(type, cats -> Core.app.post(onDone));
    }
}
