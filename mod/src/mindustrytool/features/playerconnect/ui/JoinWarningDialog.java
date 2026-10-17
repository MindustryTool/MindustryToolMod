package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.paddingY;
import static solim.modifier.Values.prefHeight;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.List;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.PlayerConnectRoom;
import solim.modifier.Values;
import solim.core.Units;
import solim.core.Component;
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

    private static class JoinWarningView extends Component {
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
                    .props(growX(), Values.maxWidth(Units.dvw.map(w -> Math.min(w * 0.9f, 900f))), maxHeight(Units.dvh.map(h -> Math.min(h * 0.85f, 900f))), margin(unit(5)), gap(unit(3.5f)), center())
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

                        row().props(gap(unit(3)), center()).children(() -> {
                            button(Core.bundle.get("cancel", "Cancel"), dialog::hide)
                                    .style(WebStyles.outline())
                                    .props(height(unit(10)), prefHeight(unit(10)), paddingX(unit(5)), paddingY(unit(2)), minWidth(unit(26)));

                            if (!unneededMods.isEmpty()) {
                                button(Core.bundle.get("feature.player-connect.disable-and-join", "Disable & Join"), this::disableAndJoin)
                                        .style(WebStyles.secondary())
                                        .props(height(unit(10)), prefHeight(unit(10)), paddingX(unit(5)), paddingY(unit(2)), minWidth(unit(36)));
                            }

                            button(Core.bundle.get("feature.player-connect.join-anyway"), this::joinDirect)
                                    .style(WebStyles.danger())
                                    .props(height(unit(10)), prefHeight(unit(10)), paddingX(unit(5)), paddingY(unit(2)), minWidth(unit(30)));
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