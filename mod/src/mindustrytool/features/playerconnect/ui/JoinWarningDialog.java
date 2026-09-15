package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.List;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.PlayerConnectRoom;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;

public class JoinWarningDialog extends SolimDialog {

    public JoinWarningDialog(
            PlayerConnectRoom room,
            List<String> missingMods,
            List<String> unneededMods,
            String password) {
        super(Core.bundle.get("warning", "Warning"));

        name("joinWarningDialog");
        addCloseButton();
        closeOnBack();
        cont().center();

        children(() -> new JoinWarningView(this, room, missingMods, unneededMods, password));
    }

    private static class JoinWarningView extends BaseComponent {
        private final JoinWarningDialog dialog;
        private final PlayerConnectRoom room;
        private final List<String> missingMods;
        private final List<String> unneededMods;
        private final String password;

        public JoinWarningView(
                JoinWarningDialog dialog,
                PlayerConnectRoom room,
                List<String> missingMods,
                List<String> unneededMods,
                String password) {
            this.dialog = dialog;
            this.room = room;
            this.missingMods = missingMods;
            this.unneededMods = unneededMods;
            this.password = password;
        }

        @Override
        protected Element build() {
            return column()
                    .width(dvw(90f).map(w -> Math.min(w, 450f)))
                    .maxHeight(dvh(85f).map(h -> Math.min(h, 450f)))
                    .margin(unit(4))
                    .gap(unit(2))
                    .center()
                    .children(() -> {
                        if (!missingMods.isEmpty()) {
                            text(Core.bundle.get("feature.player-connect.missing-mods", "Missing mods (may cause crashes):"))
                                    .color(Color.scarlet);
                            text(String.join(", ", missingMods))
                                    .color(Color.lightGray);
                        }

                        if (!unneededMods.isEmpty()) {
                            text(Core.bundle.get("feature.player-connect.unneeded-mods", "Unneeded mods:"))
                                    .color(Color.orange);
                            text(String.join(", ", unneededMods))
                                    .color(Color.lightGray);
                        }

                        row().gap(unit(2)).center().children(() -> {
                            button(Core.bundle.get("cancel", "Cancel"), dialog::hide)
                                    .style(Styles.defaultb)
                                    .size(unit(26), unit(9));

                            if (!unneededMods.isEmpty()) {
                                button(Core.bundle.get("feature.player-connect.disable-and-join", "Disable & Join"), this::disableAndJoin)
                                        .style(Styles.defaultb)
                                        .size(unit(36), unit(9));
                            }

                            button(Core.bundle.get("feature.player-connect.join-anyway", "Join Anyway"), this::joinDirect)
                                    .style(Styles.defaultb)
                                    .color(Color.royal)
                                    .size(unit(30), unit(9));
                        });
                    }).element();
        }

        private void disableAndJoin() {
            for (String modStr : unneededMods) {
                int colonIdx = modStr.indexOf(':');
                String modName = colonIdx != -1 ? modStr.substring(0, colonIdx) : modStr;
                LoadedMod mod = Vars.mods.getMod(modName);
                if (mod != null) {
                    Vars.mods.setEnabled(mod, false);
                }
            }
            joinDirect();
        }

        private void joinDirect() {
            dialog.hide();
            try {
                PlayerConnectLink link = PlayerConnectLink.fromString(room.getLink());
                PlayerConnectClient.join(link, password, () -> {
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }
    }
}
