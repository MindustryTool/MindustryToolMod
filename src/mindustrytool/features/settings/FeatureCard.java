package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.WebFeature;

public class FeatureCard {

    public static void buildToggle(Table parent, Feature feature, Runnable rebuild) {
        boolean enabled = feature.isEnabled();
        var metadata = feature.getMetadata();

        var card = parent.button(Styles.black8, () -> {

        })
                .growX()
                .height(180f)
                .pad(5f)
                .color(enabled ? Color.green : Color.red).get();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.stopped)
                    return;

                try {
                    FeatureManager.getInstance().toggle(feature);
                    parent.clear();
                    rebuild.run();
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
                header.image(metadata.icon())
                        .scaling(Scaling.fit)
                        .size(24)
                        .padRight(8);

                header.add(Utils.getString(metadata.name()))
                        .style(Styles.defaultLabel)
                        .color(Color.white)
                        .growX()
                        .ellipsis(true)
                        .left();

                header.image(Utils.scalable(enabled ? Icon.eye : Icon.eyeOff))
                        .height(24)
                        .width(32)
                        .padRight(8)
                        .scaling(Scaling.fit)
                        .color(enabled ? Color.white : Color.gray);

                if (feature.setting().isPresent()) {
                    header.button(Utils.scalable(Icon.settings), Styles.clearNonei,
                            () -> feature.setting().ifPresent(dialog -> Core.app.post(dialog::show)))
                            .size(32)
                            .scaling(Scaling.fit)
                            .get()
                            .addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    event.stop();
                                }
                            });
                }

            }).growX().row();

            c.add(Utils.getString(metadata.description()))
                    .color(Color.lightGray)
                    .fontScale(0.9f)
                    .wrap()
                    .growX()
                    .padTop(10)
                    .ellipsis(true)
                    .row();

            c.add().growY().row();
            c.add(enabled ? "@enabled" : "@disabled").color(enabled ? Color.green : Color.red).left();
        }).grow().top().left();
    }

    public static void buildLink(Table parent, Feature feature) {
        var metadata = feature.getMetadata();

        var card = parent.button(Styles.black8, () -> {

        })
                .growX()
                .height(180f)
                .pad(5f)
                .color(Pal.accent)
                .get();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.stopped)
                    return;

                try {
                    feature.dialog().ifPresent(d -> d.show());
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
                header.add(Utils.getString(metadata.name()))
                        .style(Styles.defaultLabel)
                        .color(Color.white)
                        .growX()
                        .ellipsis(true)
                        .left();
            }).growX().row();

            c.add(Utils.getString(metadata.description()))
                    .color(Color.lightGray)
                    .fontScale(0.9f)
                    .wrap()
                    .growX()
                    .padTop(10)
                    .ellipsis(true)
                    .row();

            c.add().grow().row();
            c.add().growX();

            c.table(Tex.button, l -> l.image(Icon.linkSmall));
        }).grow();
    }

    public static void buildLink(Table parent, WebFeature feature) {
        var card = parent.button(Styles.black8, () -> {

        })
                .growX()
                .height(180f)
                .pad(5f)
                .get();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.stopped)
                    return;

                try {
                    if (!Core.app.openURI(feature.url())) {
                        Core.app.setClipboardText(feature.url());
                        mindustry.Vars.ui.showInfoFade("@copied");
                    }
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
                header.add(Utils.getString(feature.name())).style(Styles.defaultLabel).color(Color.white).growX()
                        .ellipsis(true).left();
            }).growX().row();

            c.add(Utils.getString(feature.description())).color(Color.lightGray).fontScale(0.9f).wrap().growX()
                    .padTop(10)
                    .ellipsis(true).row();
            c.add().growY().row();
            c.add().growX();

            c.table(Tex.button, l -> l.image(Icon.linkSmall));
        }).grow();
    }
}
