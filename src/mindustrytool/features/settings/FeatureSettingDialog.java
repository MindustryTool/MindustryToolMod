package mindustrytool.features.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;

public class FeatureSettingDialog extends BaseDialog {

    public FeatureSettingDialog() {
        super("Feature");
        addCloseButton();

        buttons.button("@feature.report-bug", Icon.infoCircle, () -> {
            Core.app.openURI(Config.DISCORD_INVITE_URL);
        });

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

        shown(this::rebuild);
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

    private void rebuild() {
        cont.clear();
        cont.pane(table -> {
            table.top().left();

            table.add("@feature").padLeft(10).top().left().row();

            int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.9f / 340f));
            float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.9f) / cols;

            int i = 0;

            for (Feature feature : FeatureManager.getInstance().getEnableds().select(f -> f.dialog().isPresent())) {
                buildFeatureButton(table, feature, cardWidth);
                if (++i % cols == 0) {
                    table.row();
                }
            }

            table.row();
            table.image().color(Color.gray).growX().height(4f)
                    .colspan(cols)
                    .pad(10)
                    .row();

            table.add("@settings").padLeft(10).top().left().row();

            i = 0;

            for (Feature feature : FeatureManager.getInstance().getFeatures()) {
                buildFeatureCard(table, feature, cardWidth);
                if (++i % cols == 0) {
                    table.row();
                }
            }
        }).grow();
    }

    private void buildFeatureButton(Table parent, Feature feature, float cardWidth) {
        var metadata = feature.getMetadata();

        parent.table(Tex.button, card -> {
            card.top().left();

            card.table(c -> {
                c.top().left().margin(12);

                // Header
                c.table(header -> {
                    header.left();
                    header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

                    // Settings button
                    if (feature.setting().isPresent()) {
                        header.button(Icon.settings, Styles.clearNonei,
                                () -> feature.setting().ifPresent(dialog -> Core.app.post(() -> dialog.show())))
                                .size(32)
                                .padLeft(8)
                                .get().addListener(new ClickListener() {
                                    @Override
                                    public void clicked(InputEvent event, float x, float y) {
                                        event.stop();
                                    }
                                });
                    }

                    // Status icon (visual only)
                }).growX().row();

                // Description
                c.add(metadata.description())
                        .color(Color.lightGray)
                        .fontScale(0.9f)
                        .wrap()
                        .growX()
                        .padTop(10)
                        .row();

                // Spacer to push status to bottom
                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    feature.dialog().get().show();
                });

            }).grow();

        })
                .growX()
                .minWidth(cardWidth)
                .height(180f).pad(10f).get().clicked(() -> {
                    feature.dialog().get().show();
                });
    }

    private void buildFeatureCard(Table parent, Feature feature, float cardWidth) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        var metadata = feature.getMetadata();

        parent.table(Styles.black6, card -> {
            card.top().left();

            // Status border
            card.image().color(enabled ? Color.green : Color.red).growX().height(4f).row();

            card.table(c -> {
                c.top().left().margin(12);

                // Header
                c.table(header -> {
                    header.left();
                    header.image(metadata.icon()).scaling(Scaling.fill).size(24).padRight(8);
                    header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

                    // Settings button
                    if (feature.setting().isPresent()) {
                        header.button(Icon.settings, Styles.clearNonei,
                                () -> feature.setting().ifPresent(dialog -> Core.app.post(() -> dialog.show())))
                                .size(32)
                                .padLeft(8)
                                .get().addListener(new ClickListener() {
                                    @Override
                                    public void clicked(InputEvent event, float x, float y) {
                                        event.stop();
                                    }
                                });
                    }

                    // Status icon (visual only)
                    header.image(enabled ? Icon.eyeSmall : Icon.eyeOffSmall).size(24).padLeft(4)
                            .color(enabled ? Color.white : Color.gray);
                }).growX().row();

                // Description
                c.add(metadata.description())
                        .color(Color.lightGray)
                        .fontScale(0.9f)
                        .wrap()
                        .growX()
                        .padTop(10)
                        .row();

                // Spacer to push status to bottom
                c.add().growY().row();

                // Status Footer
                c.add(enabled ? "@enabled" : "@disabled")
                        .color(enabled ? Color.green : Color.red)
                        .left();
            }).grow();

        })
                .growX()
                .minWidth(cardWidth)
                .height(180f).pad(10f).get().addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (event.stopped) {
                            return;
                        }

                        FeatureManager.getInstance().setEnabled(feature, !enabled);
                        rebuild();
                    }
                });
    }
}
