package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.WebFeature;
import mindustrytool.ui.ChangelogDialog;

public class FeatureSettingDialog extends BaseDialog {

    private String filter = "";
    private Table paneTable;

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
        resized(this::rebuild);
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

        int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.85f / 340f));
        float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.85f) / cols;

        paneTable.row();
        paneTable.button("@reeanable", () -> {
            FeatureManager.getInstance().reEnable();
            rebuildPane();
        }).width(250).top().left().pad(10).tooltip("Used after a crash");

        paneTable.row();

        int i = 0;
        // Toggleable Features
        for (Feature feature : FeatureManager.getInstance().getFeatures()) {
            if (!filter.isEmpty()
                    && !Utils.getString(feature.getMetadata().name()).toLowerCase().contains(filter.toLowerCase()))
                continue;

            FeatureCard.buildToggle(paneTable, feature, this::rebuildPane);

            if (++i % cols == 0) {
                paneTable.row();
            }
        }

        paneTable.row();
        paneTable.image().color(Color.gray).growX().height(4f).colspan(cols).pad(10).row();

        paneTable.add("@feature").padLeft(10).top().left().row();

        i = 0;

        // Features with Dialogs
        for (Feature feature : FeatureManager.getInstance().getEnableds()) {
            if (!feature.dialog().isPresent())
                continue;
            if (!filter.isEmpty()
                    && !Utils.getString(feature.getMetadata().name()).toLowerCase().contains(filter.toLowerCase()))
                continue;

            FeatureCard.buildLink(paneTable, feature);
            if (++i % cols == 0)
                paneTable.row();
        }

        // Web Features
        for (WebFeature webFeature : WebFeature.defaults) {
            if (!filter.isEmpty() && !Utils.getString(webFeature.name()).toLowerCase().contains(filter.toLowerCase()))
                continue;

            FeatureCard.buildLink(paneTable, webFeature);
            if (++i % cols == 0)
                paneTable.row();
        }

        // Icon Dialog
        buildIconDialogButton(paneTable, cardWidth);
        if (++i % cols == 0)
            paneTable.row();

        paneTable.table().growX().row();
    }

    private void buildIconDialogButton(Table parent, float cardWidth) {
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

                c.button(Icon.linkSmall, () -> new IconBrowserDialog().show());
            }).grow();
        }).growX().width(cardWidth).height(180f).pad(10f);
    }
}
