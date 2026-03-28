package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.WebFeature;

public class FeatureCard {

    public static void buildToggle(Table parent, Feature feature, Runnable rebuild) {
        boolean enabled = feature.isEnabled();
        var metadata = feature.getMetadata();

        var card = parent.button(Styles.black8, () -> {
        }).growX().height(180f).pad(5f)
                .color(enabled ? Color.green : Color.red).get();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.stopped)
                    return;
                try {
                    feature.toggle();
                    parent.clear();
                    rebuild.run();
                } catch (Exception e) {
                    arc.util.Log.err(e);
                }
            }
        });

        card.top().left();
        card.table(c -> {
            c.top().left().margin(12);

            c.table(header -> {
                header.left();
                header.image(metadata.icon()).scaling(Scaling.fill).size(24).padRight(8);
                header.add(Utils.getString(metadata.name())).style(Styles.defaultLabel).color(Color.white).growX()
                        .ellipsis(true)
                        .left();

                if (feature.setting().isPresent()) {
                    header.button(Icon.settings, Styles.clearNonei,
                            () -> feature.setting().ifPresent(dialog -> Core.app.post(dialog::show)))
                            .size(32).padLeft(8).get().addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    event.stop();
                                }
                            });
                }

                header.image(enabled ? Icon.eyeSmall : Icon.eyeOffSmall).size(24).padLeft(4)
                        .color(enabled ? Color.white : Color.gray);
            }).growX().row();

            c.add(Utils.getString(metadata.description())).color(Color.lightGray).fontScale(0.9f).wrap().growX()
                    .padTop(10).ellipsis(true)
                    .row();
            c.add().growY().row();
            c.add(enabled ? "@enabled" : "@disabled").color(enabled ? Color.green : Color.red).left();
        }).grow().top().left();
    }

    public static void buildLink(Table parent, Feature feature) {
        var metadata = feature.getMetadata();
        parent.table(Tex.button, card -> {
            card.top().left();
            card.table(c -> {
                c.top().left().margin(12);
                c.table(header -> {
                    header.left();
                    header.add(Utils.getString(metadata.name())).style(Styles.defaultLabel).color(Color.white).growX()
                            .ellipsis(true)
                            .left();

                    if (feature.setting().isPresent()) {
                        header.button(Icon.settings, Styles.clearNonei,
                                () -> feature.setting().ifPresent(dialog -> Core.app.post(dialog::show)))
                                .size(32).padLeft(8).get().addListener(new ClickListener() {
                                    @Override
                                    public void clicked(InputEvent event, float x, float y) {
                                        event.stop();
                                    }
                                });
                    }
                }).growX().row();

                c.add(Utils.getString(metadata.description())).color(Color.lightGray).fontScale(0.9f).wrap().growX()
                        .padTop(10)
                        .ellipsis(true)
                        .row();
                c.add().grow().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> feature.dialog().ifPresent(d -> d.show()));
            }).grow();
        }).growX().height(180f).pad(5f).get().clicked(() -> feature.dialog().ifPresent(d -> d.show()));
    }

    public static void buildLink(Table parent, WebFeature feature) {
        parent.table(Tex.button, card -> {
            card.top().left();
            card.table(c -> {
                c.top().left().margin(12);
                c.table(header -> {
                    header.left();
                    header.add(Utils.getString(feature.name())).style(Styles.defaultLabel).color(Color.white).growX()
                            .ellipsis(true).left();
                }).growX().row();

                c.add(Utils.getString(feature.description())).color(Color.lightGray).fontScale(0.9f).wrap().growX()
                        .padTop(10)
                        .ellipsis(true).row();
                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    if (!Core.app.openURI(feature.url())) {
                        Core.app.setClipboardText(feature.url());
                        mindustry.Vars.ui.showInfoFade("@copied");
                    }
                });
            }).grow();
        }).growX().height(180f).pad(5f);
    }
}
