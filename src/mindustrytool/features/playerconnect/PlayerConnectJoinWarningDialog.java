package mindustrytool.features.playerconnect;

import arc.Core;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;

public class PlayerConnectJoinWarningDialog extends BaseDialog {
    public PlayerConnectJoinWarningDialog(PlayerConnectRoom room, Seq<String> unneeded, Seq<String> missing) {
        super("@warning");
        name = "playerConnectJoinWarningDialog";

        if (!missing.isEmpty()) {
            cont.add("Missing mods detected. Join anyway?").row();
            cont.label(() -> missing.toString(", ")).color(Pal.lightishGray).width(400f).wrap().row();
        }

        if (!unneeded.isEmpty()) {
            cont.add("Unneeded mods detected. Disable them?").row();
            cont.label(() -> unneeded.toString(", ")).color(Pal.lightishGray).width(400f).wrap().row();
        }

        buttons.button("@cancel", this::hide).size(100, 50);

        buttons.button("Disable & Join", () -> {
            unneeded
                    .map(mod -> mod.substring(0, mod.indexOf(":")))
                    .each(name -> {
                        var mod = Vars.mods.getMod(name);
                        if (mod != null) {
                            Vars.mods.setEnabled(mod, false);
                        }
                    });
            hide();
            PlayerConnectRenderer.proceedToJoin(room);
        }).size(200, 50);

        buttons.button("Ignore", () -> {
            hide();
            PlayerConnectRenderer.proceedToJoin(room);
        }).size(100, 50);

        Core.app.post(this::show);
    }
}
