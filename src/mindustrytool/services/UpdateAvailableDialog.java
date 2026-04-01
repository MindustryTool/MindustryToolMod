package mindustrytool.services;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;

public class UpdateAvailableDialog extends BaseDialog {
    public UpdateAvailableDialog(String currentVer, String latestVer, String changelog, Runnable done) {
        super("Update Available");
        name = "updateAvailableDialog";

        Table table = new Table();
        table.defaults().left();

        table.add(Core.bundle.format("message.new-version", "[#" + Color.crimson.toString() + "]" + currentVer,
                "[#" + Color.green.toString() + "]" + latestVer))
                .wrap()
                .width(500f)
                .padBottom(20)
                .row();

        table.add("Discord: " + Config.DISCORD_INVITE_URL).color(Color.royal).padTop(5f).row();

        table.image().height(4f).color(Color.gray).fillX().pad(10f).row();

        Table changelogTable = new Table();
        changelogTable.top().left();
        changelogTable.add(changelog).growX().wrap().width(480f).left();

        ScrollPane pane = new ScrollPane(changelogTable);
        table.add(pane).size(500f, 400f)
                .scrollX(false)
                .row();

        cont.add(table);

        buttons.button("Cancel", () -> {
            remove();
            done.run();
        }).size(100f, 50f);
        buttons.button("Update", () -> {
            try {
                remove();
                Vars.ui.mods.show();
                Vars.ui.mods.githubImportMod(Config.REPO_URL, true);
                Vars.ui.mods.toFront();
                Timer.schedule(() -> Vars.ui.loadfrag.toFront(), 0.2f);
            } catch (Exception e) {
                Log.err(e);
                Vars.ui.showException(e);
            }
        }).size(100f, 50f);
    }
}
