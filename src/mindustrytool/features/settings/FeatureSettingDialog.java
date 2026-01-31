package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import lombok.AllArgsConstructor;
import lombok.Data;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.ui.ChangelogDialog;

public class FeatureSettingDialog extends BaseDialog {

    private static final Seq<WebFeature> webFeatures = Seq.with(
            new WebFeature(
                    "@content-patches",
                    "@content-patches.description",
                    "https://mindustry-tool.com/vi/content-patches?size=100"),
            new WebFeature(
                    "@logic-editor",
                    "@logic-editor.description",
                    "https://mindustry-tool.com/vi/tools/logic"),
            new WebFeature(
                    "@logic-display-generator",
                    "@logic-display-generator.description",
                    "https://mindustry-tool.com/vi/tools/logic-display-generator"),
            new WebFeature(
                    "@sorter-image-generator",
                    "@sorter-image-generator.description",
                    "https://mindustry-tool.com/vi/tools/sorter-generator"),
            new WebFeature(
                    "@canvas-image-generator",
                    "@canvas-image-generator.description",
                    "https://mindustry-tool.com/vi/tools/canvas-generator"),
            new WebFeature(
                    "@wiki",
                    "@wiki.description",
                    "https://mindustry-tool.com/vi/wiki"),
            new WebFeature(
                    "@post",
                    "@post.description",
                    "https://mindustry-tool.com/vi/posts"),
            new WebFeature(
                    "@free-mindustry-server",
                    "@free-mindustry-server.description",
                    "https://mindustry-tool.com/vi/@me/servers")

    //
    );

    private boolean showWebFeature = true;
    private String filter = "";
    private Table paneTable;

    private String t(String text) {
        if (text == null) {
            return "";
        }

        if (text.startsWith("@")) {
            String key = text.substring(1);
            try {
                return Core.bundle.get(key);
            } catch (Exception e) {
                return text;
            }
        }
        return text;
    }

    public Dialog show(boolean showWebFeature) {
        this.showWebFeature = showWebFeature;
        return super.show();
    }

    public FeatureSettingDialog() {
        super("Feature");

        addCloseButton();

        buttons.button("@feature.changelog", Icon.book, () -> {
            new ChangelogDialog().show();
        });

        buttons.button("@feature.report-bug", Icon.infoCircle, () -> {
            if (!Core.app.openURI(Config.DISCORD_INVITE_URL)) {
                Core.app.setClipboardText(Config.DISCORD_INVITE_URL);
                Vars.ui.showInfoFade("@copied");
            }
        });

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom).padRight(8);
            s.field(filter, f -> {
                filter = f;
                rebuildPane();
            }).growX();
        }).growX().pad(10).row();

        cont.pane(table -> {
            this.paneTable = table;
            rebuildPane();
        }).scrollX(false).grow();
    }

    private void rebuildPane() {
        if (paneTable == null) {
            return;
        }

        paneTable.clear();
        paneTable.top().left();

        int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.93f / 340f));

        float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.93f) / cols;

        int i = 0;

        if (showWebFeature) {
            paneTable.add("@feature").padLeft(10).top().left().row();

            var featureWithDialog = FeatureManager.getInstance().getEnableds()
                    .select(f -> f.dialog().isPresent() && (filter.isEmpty()
                            || t(f.getMetadata().name()).toLowerCase().contains(filter.toLowerCase())));

            if (featureWithDialog.size > 0) {
                for (Feature feature : featureWithDialog) {
                    buildFeatureButton(paneTable, feature, cardWidth);
                    if (++i % cols == 0) {
                        paneTable.row();
                    }
                }
            }

            for (WebFeature webFeature : webFeatures) {
                if (!filter.isEmpty() && !t(webFeature.getName()).toLowerCase().contains(filter.toLowerCase())) {
                    continue;
                }
                buildFeatureButton(paneTable, webFeature, cardWidth);
                if (++i % cols == 0) {
                    paneTable.row();
                }
            }

            buildIconDialog(paneTable, cardWidth);
            if (++i % cols == 0) {
                paneTable.row();
            }

            paneTable.row();
            paneTable.button("@reeanable", () -> {
                FeatureManager.getInstance().reEnable();
            }).width(250).top().left().pad(10).tooltip("Used after a crash");

            paneTable.table().growX().row();

            paneTable.row();
            paneTable.image().color(Color.gray).growX().height(4f)
                    .colspan(cols)
                    .pad(10)
                    .row();

        }
        paneTable.add("@settings").padLeft(10).top().left().row();

        i = 0;

        for (Feature feature : FeatureManager.getInstance().getFeatures()) {
            if (!filter.isEmpty() && !t(feature.getMetadata().name()).toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }
            paneTable.table(parent -> buildFeatureCard(parent, feature, cardWidth))
                    .growX();

            if (++i % cols == 0) {
                paneTable.row();
            }
        }
    }

    private void buildFeatureButton(Table parent, Feature feature, float cardWidth) {
        var metadata = feature.getMetadata();
        parent.table(Tex.button, card -> {
            card.top().left();

            card.table(c -> {
                c.top().left().margin(12);

                c.table(header -> {
                    header.left();
                    header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

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

                }).growX().row();

                c.add(metadata.description())
                        .color(Color.lightGray)
                        .fontScale(0.9f)
                        .wrap()
                        .growX()
                        .padTop(10)
                        .row();

                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    feature.dialog().get().show();
                });

            }).grow();

        })
                .growX()
                .width(cardWidth)
                .height(180f).pad(10f).get().clicked(() -> {
                    feature.dialog().get().show();
                });
    }

    private void buildFeatureButton(Table parent, WebFeature feature, float cardWidth) {
        parent.table(Tex.button, card -> {
            card.top().left();

            card.table(c -> {
                c.top().left().margin(12);

                c.table(header -> {
                    header.left();
                    header.add(feature.getName()).style(Styles.defaultLabel).color(Color.white).growX().left();
                }).growX().row();

                c.add(feature.getDescription())
                        .color(Color.lightGray)
                        .fontScale(0.9f)
                        .wrap()
                        .growX()
                        .padTop(10)
                        .row();

                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    if (!Core.app.openURI(feature.getUrl())) {
                        Core.app.setClipboardText(feature.getUrl());
                        Vars.ui.showInfoFade("@copied");
                    }
                });

            }).grow();

        })
                .growX()
                .width(cardWidth)
                .height(180f).pad(10f);
    }

    private void buildFeatureCard(Table parent, Feature feature, float cardWidth) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        var metadata = feature.getMetadata();

        var card = parent.button(Styles.black8, () -> {
        })
                .growX()
                .width(cardWidth)
                .height(180f)
                .pad(10f)
                .grow()
                .color(enabled ? Color.green : Color.red)
                .get();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.stopped) {
                    return;
                }

                try {
                    FeatureManager.getInstance().setEnabled(feature, !enabled);
                    parent.clear();
                    buildFeatureCard(parent, feature, cardWidth);
                } catch (Exception e) {
                    Log.err(e);
                }
            }
        });

        card.top().left();
        card.table(c -> {
            c.top().left().margin(12);

            c.table(header -> {
                header.left();
                header.image(metadata.icon()).scaling(Scaling.fill).size(24).padRight(8);
                header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

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

                header.image(enabled ? Icon.eyeSmall : Icon.eyeOffSmall).size(24).padLeft(4)
                        .color(enabled ? Color.white : Color.gray);
            }).growX().row();

            c.add(metadata.description())
                    .color(Color.lightGray)
                    .fontScale(0.9f)
                    .wrap()
                    .growX()
                    .padTop(10)
                    .row();

            c.add().growY().row();

            c.add(enabled ? "@enabled" : "@disabled")
                    .color(enabled ? Color.green : Color.red)
                    .left();
        }).top().left().grow();
    }

    private void buildIconDialog(Table parent, float cardWidth) {
        parent.table(Tex.button, card -> {
            card.top().left();

            card.table(c -> {
                c.top().left().margin(12);

                c.table(header -> {
                    header.left();
                    header.add("Icon").style(Styles.defaultLabel).color(Color.white).growX().left();
                }).growX().row();

                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    var dialog = new BaseDialog("Icon");

                    dialog.addCloseButton();
                    dialog.closeOnBack();

                    var containers = new Table();

                    int width = 400;
                    int cols = Math.max((int) (Core.graphics.getWidth() * 0.9 / (width + 20)), 1);
                    String[] filter = { "" };

                    Runnable build = () -> {
                        containers.clear();

                        var declaredFields = Iconc.class.getDeclaredFields();

                        int col = 0;

                        for (var field : declaredFields) {
                            try {
                                field.setAccessible(true);

                                var icon = field.get(null);

                                if (icon == Iconc.all) {
                                    continue;
                                }

                                if (icon instanceof String || icon instanceof Character) {
                                    if (!field.getName().toLowerCase().contains(filter[0]
                                            .toLowerCase())) {
                                        continue;
                                    }

                                    containers.button(String.valueOf(icon) + " " + field.getName(), () -> {
                                        Core.app.setClipboardText(String.valueOf(icon));
                                    })
                                            .width(width)
                                            .scaling(Scaling.fill)
                                            .growX()
                                            .padRight(8)
                                            .padBottom(8)
                                            .labelAlign(Align.left)
                                            .top()
                                            .left()
                                            .get();

                                    if (++col % cols == 0) {
                                        containers.row();
                                    }
                                }
                            } catch (Exception e) {
                                Log.err(e);
                            }
                        }
                    };

                    dialog.cont.field(filter[0], Styles.defaultField, (t) -> {
                        filter[0] = t;
                        build.run();
                    }).width(cols * (width + 20) - 20).growX().row();

                    build.run();
                    dialog.cont.pane(containers).scrollX(false).top();
                    dialog.cont.top();

                    dialog.show();
                });

            }).grow();

        })
                .growX()
                .width(cardWidth)
                .height(180f).pad(10f);
    }

    @Data
    @AllArgsConstructor
    private static class WebFeature {
        private String name, description, url;
    }
}
