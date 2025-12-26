package mindustrytool.plugins.playerconnect;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.gen.Icon;
import arc.Core;

/** Injects Player Connect button into the paused menu. */
public class PausedMenuInjector {

    public static void inject(BaseDialog roomDialog) {
        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;

            // Debug logging
            arc.util.Log.info("[PlayerConnect] Scanning pause menu for 'Host' button...");

            // Search for the cell containing the host button
            boolean replaced = false;
            String hostText = Core.bundle.get("server.host");

            for (arc.scene.ui.layout.Cell<?> cell : root.getCells()) {
                if (cell.get() instanceof Button b) {
                    boolean isHost = false;

                    // Check TextButton (Text OR Icon)
                    if (b instanceof TextButton tb) {
                        String text = tb.getText().toString();
                        if (text.contains(hostText) || text.contains("Host")) {
                            isHost = true;
                        } else {
                            // Check if TextButton has the Host icon
                            for (arc.scene.Element child : tb.getChildren()) {
                                if (child instanceof arc.scene.ui.Image img && img.getDrawable() == Icon.host) {
                                    isHost = true;
                                    break;
                                }
                            }
                        }
                    }
                    // Check ImageButton (Icon)
                    else if (b instanceof ImageButton ib && ib.getStyle().imageUp == Icon.host) {
                        isHost = true;
                    }

                    if (isHost) {
                        arc.util.Log.info(
                                "[PlayerConnect] Host button found in "
                                        + (b instanceof TextButton ? "TextButton" : "ImageButton") + "! Replacing.");

                        TextButton newBtn = new TextButton(
                                Core.bundle.get("message.manage-room.host-title", "Multiplayer"));
                        newBtn.add(new arc.scene.ui.Image(Icon.host)).padLeft(6f);
                        newBtn.getCells().reverse();
                        newBtn.changed(() -> {
                            try {
                                roomDialog.show();
                            } catch (Exception e) {
                                Vars.ui.showException(e);
                            }
                        });

                        // Suppress raw type warning for setElement
                        @SuppressWarnings("unchecked")
                        arc.scene.ui.layout.Cell<arc.scene.Element> typedCell = (arc.scene.ui.layout.Cell<arc.scene.Element>) cell;
                        typedCell.setElement(newBtn);

                        replaced = true;
                        break;
                    }
                }
            }

            if (!replaced) {
                arc.util.Log.info("[PlayerConnect] Host button NOT found. Adding fallback 'Manage room' button.");

                // Fallback logic
                @SuppressWarnings("rawtypes")
                arc.struct.Seq<arc.scene.ui.layout.Cell> cells = root.getCells();

                if (Vars.mobile) {
                    root.row().buttonRow("@message.manage-room.title", Icon.planet, roomDialog::show).row();
                } else if (!cells.isEmpty() && cells.size > 2
                        && arc.util.Reflect.<Integer>get(cells.get(cells.size - 2), "colspan") == 2) {
                    root.row().button("@message.manage-room.title", Icon.planet, roomDialog::show).colspan(2)
                            .width(450f).row();
                } else {
                    root.row().button("@message.manage-room.title", Icon.planet, roomDialog::show).row();
                }

                if (cells.size > 2) {
                    cells.swap(cells.size - 1, cells.size - 2);
                }
            }
        });
    }
}
