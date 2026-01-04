package mindustrytool.features.social.multiplayer;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import mindustry.Vars;
import mindustry.maps.Map;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.gen.Icon;
import arc.Core;

/** Injects Player Connect button into the paused menu. */
public class PausedMenuInjector {

    public static void inject() {
        Vars.ui.paused.shown(() -> {
            if (!PlayerConnectFeature.getRoomsDialog().isEnabled())
                return;

            Table root = Vars.ui.paused.cont;

            // Debug logging
            // arc.util.Log.info("[PlayerConnect] Scanning pause menu... isGame=" +
            // Vars.state.isGame() + ", isCampaign=" + Vars.state.isCampaign());

            // Search for the cell containing the host button
            boolean hostReplaced = false;
            boolean planetReplaced = false;
            String hostText = Core.bundle.get("server.host");
            String planetText = Core.bundle.get("planet.button", "Planet Map"); // Planet Map button text

            for (arc.scene.ui.layout.Cell<?> cell : root.getCells()) {
                if (cell.get() instanceof Button b) {
                    boolean isHost = false;
                    boolean isPlanet = false;

                    // Check TextButton (Text OR Icon)
                    if (b instanceof TextButton tb) {
                        String text = tb.getText().toString();
                        if (text.contains(hostText) || text.contains("Host")) {
                            isHost = true;
                        } else if (text.contains(planetText) || text.contains("Planet")) {
                            isPlanet = true;
                        } else {
                            // Check by icon
                            for (arc.scene.Element child : tb.getChildren()) {
                                if (child instanceof arc.scene.ui.Image img) {
                                    if (img.getDrawable() == Icon.host) {
                                        isHost = true;
                                        break;
                                    } else if (img.getDrawable() == Icon.planet) {
                                        isPlanet = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // Check ImageButton (Icon)
                    else if (b instanceof ImageButton ib) {
                        if (ib.getStyle().imageUp == Icon.host) {
                            isHost = true;
                        } else if (ib.getStyle().imageUp == Icon.planet) {
                            isPlanet = true;
                        }
                    }

                    // Replace Host button
                    if (isHost && !hostReplaced) {
                        // arc.util.Log.info("[PlayerConnect] Host button found! Replacing with
                        // Multiplayer button.");

                        TextButton newBtn = new TextButton(
                                Core.bundle.get("mdt.message.manage-room.host-title", "Multiplayer"));
                        newBtn.add(new arc.scene.ui.Image(Icon.host)).padLeft(6f);
                        newBtn.getCells().reverse();
                        newBtn.changed(() -> {
                            try {
                                PlayerConnectFeature.getInstance().showCreateRoomDialog();
                            } catch (Exception e) {
                                Vars.ui.showException(e);
                            }
                        });

                        @SuppressWarnings("unchecked")
                        arc.scene.ui.layout.Cell<arc.scene.Element> typedCell = (arc.scene.ui.layout.Cell<arc.scene.Element>) cell;
                        typedCell.setElement(newBtn);
                        hostReplaced = true;
                    }

                    // Replace Planet Map button with Change Map ONLY when in custom game mode
                    // (Host/Local)
                    // If in Campaign, we want to Keep Planet Map and Add Change Map separately
                    if (isPlanet && !planetReplaced && Vars.state.isGame() && !Vars.state.isCampaign()) {
                        // arc.util.Log.info("[PlayerConnect] Planet Map button found in custom game!
                        // Replacing with Change Map.");

                        String btnText = Vars.net.client() ? "Play Custom" : "Change Map";
                        TextButton changeMapBtn = new TextButton(btnText);
                        changeMapBtn.add(new arc.scene.ui.Image(Icon.map)).padLeft(6f);
                        changeMapBtn.getCells().reverse();
                        changeMapBtn.changed(() -> {
                            try {
                                Vars.ui.paused.hide();
                                // Use reflection to access custom game dialog to avoid compilation issues
                                Object customDialog = arc.util.Reflect.get(Vars.ui, "custom");
                                if (customDialog instanceof mindustry.ui.dialogs.BaseDialog) {
                                    ((mindustry.ui.dialogs.BaseDialog) customDialog).show();
                                } else {
                                    Vars.ui.showInfo("Could not find Custom Game dialog.");
                                }
                            } catch (Exception e) {
                                Vars.ui.showException(e);
                            }
                        });

                        @SuppressWarnings("unchecked")
                        arc.scene.ui.layout.Cell<arc.scene.Element> typedCell = (arc.scene.ui.layout.Cell<arc.scene.Element>) cell;
                        typedCell.setElement(changeMapBtn);
                        planetReplaced = true;
                    }
                }
            }

            if (!hostReplaced) {
                // arc.util.Log.info("[PlayerConnect] Host button NOT found. Adding fallback
                // button.");

                @SuppressWarnings("rawtypes")
                arc.struct.Seq<arc.scene.ui.layout.Cell> cells = root.getCells();

                if (Vars.mobile) {
                    root.row().buttonRow("@mdt.message.manage-room.title", Icon.planet,
                            () -> PlayerConnectFeature.getInstance().showCreateRoomDialog()).row();
                } else if (!cells.isEmpty() && cells.size > 2
                        && arc.util.Reflect.<Integer>get(cells.get(cells.size - 2), "colspan") == 2) {
                    root.row()
                            .button("@mdt.message.manage-room.title", Icon.planet,
                                    () -> PlayerConnectFeature.getInstance().showCreateRoomDialog())
                            .colspan(2)
                            .width(450f).row();
                } else {
                    root.row().button("@mdt.message.manage-room.title", Icon.planet,
                            () -> PlayerConnectFeature.getInstance().showCreateRoomDialog()).row();
                }

                if (cells.size > 2) {
                    cells.swap(cells.size - 1, cells.size - 2);
                }
            }

            // Add Change Map button if it wasn't replaced (e.g. Campaign or button not
            // found)
            // Logic: Always show if in Game.
            if (!planetReplaced && Vars.state.isGame()) {
                // arc.util.Log.info("[PlayerConnect] Adding Change Map button
                // (Campaign/Fallback).");

                @SuppressWarnings("rawtypes")
                arc.struct.Seq<arc.scene.ui.layout.Cell> cells = root.getCells();

                Runnable showMap = () -> {
                    try {
                        Vars.ui.paused.hide();
                        // Use reflection to access custom game dialog to avoid compilation issues
                        Object customDialog = arc.util.Reflect.get(Vars.ui, "custom");
                        if (customDialog instanceof mindustry.ui.dialogs.BaseDialog) {
                            ((mindustry.ui.dialogs.BaseDialog) customDialog).show();
                        } else {
                            Vars.ui.showInfo("Could not find Custom Game dialog.");
                        }
                    } catch (Exception e) {
                        Vars.ui.showException(e);
                    }
                };

                String btnText = Vars.net.client() ? "Play Custom" : "Change Map";
                if (Vars.mobile) {
                    root.row().buttonRow(btnText, Icon.map, showMap).row();
                } else if (!cells.isEmpty() && cells.size > 2) {
                    root.row().button(btnText, Icon.map, showMap).colspan(2)
                            .width(450f).row();
                } else {
                    root.row().button(btnText, Icon.map, showMap).row();
                }

                // Swap with the last button (usually Save & Quit) so this appears above it
                if (cells.size > 1) {
                    cells.swap(cells.size - 1, cells.size - 2);
                    // arc.util.Log.info("[PlayerConnect] Swapped Change Map button with previous
                    // element.");
                }
            }
        });
    }
}
