package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public class GodModeCoreDialog extends SolimDialog {

    private final Signal<Block> selectedCore;
    private final Signal<Team> selectedTeam = Signal.of(Vars.player != null ? Vars.player.team() : Team.sharded);
    private final Signal<Float> posX = Signal.of(Vars.player != null ? Vars.player.x : 0f);
    private final Signal<Float> posY = Signal.of(Vars.player != null ? Vars.player.y : 0f);

    public GodModeCoreDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.core.title"));

        name("godModeCoreDialog");
        addCloseButton();
        closeOnBack();

        Seq<Block> coreBlocks = Vars.content.blocks().select(b -> b instanceof CoreBlock);
        selectedCore = Signal.of(coreBlocks.size > 0 ? coreBlocks.first() : null);

        Readable<String> posLabel = posX.combine(posY, (x, y) ->
                Core.bundle.format("feature.god-mode.core.position",
                        Math.round(x != null ? x : 0f),
                        Math.round(y != null ? y : 0f)));

        children(() -> {
            column().gap(unit(2)).padding(unit(2)).children(() -> {
                text(Core.bundle.get("feature.god-mode.core.select-core")).color(Color.lightGray);

                scroll().size(420f, 100f).children(() -> {
                    grid(6).gap(unit(1)).children(() -> {
                        for (Block b : coreBlocks) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(selectedCore.map(sel -> sel == b))
                                    .onClick(() -> selectedCore.set(b))
                                    .size(unit(10), unit(10))
                                    .tooltip(b.localizedName)
                                    .children(() -> image(b.uiIcon).size(unit(7)));
                        }
                    });
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(Color.lightGray);

                scroll().size(420f, 50f).children(() -> {
                    row().gap(unit(1)).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .padding(unit(1))
                                    .children(() -> {
                                        row().gap(unit(1)).children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                row().growX().gap(unit(2)).center().children(() -> {
                    text(posLabel).color(Color.lightGray);
                    button(Core.bundle.get("feature.god-mode.core.select-position"))
                            .style(Styles.defaultt)
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }));
                });

                button(Core.bundle.get("feature.god-mode.core.place"))
                        .style(Styles.defaultt)
                        .growX()
                        .onClick(() -> {
                            Block b = selectedCore.get();
                            Team tm = selectedTeam.get();
                            float x = posX.get() != null ? posX.get() : 0f;
                            float y = posY.get() != null ? posY.get() : 0f;
                            if (b != null && tm != null) {
                                boolean placed = provider.placeCore(b, tm, x, y);
                                if (!placed && Vars.ui != null && Vars.ui.hudfrag != null) {
                                    Vars.ui.hudfrag.showToast(Core.bundle.get("feature.god-mode.core.invalid-tile"));
                                }
                            }
                            hide();
                        });
            });
        });
    }
}
