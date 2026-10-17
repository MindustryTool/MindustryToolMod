package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

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

        Computed<String> posLabel = Signal.computed(() ->
                Core.bundle.format("feature.god-mode.core.position",
                        Math.round(posX.get() != null ? posX.get() : 0f),
                        Math.round(posY.get() != null ? posY.get() : 0f)));

        children(() -> {
            column().props(padding(unit(3)), gap(unit(2.5f))).children(() -> {
                text(Core.bundle.get("feature.god-mode.core.select-core")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 110f), prefWidth(440f), prefHeight(110f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Block b : coreBlocks) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedCore.map(sel -> sel == b))
                                    .onClick(() -> selectedCore.set(b))
                                    .props(size(unit(11), unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), padding(unit(1)), tooltip(b.localizedName))
                                    .children(() -> image(new TextureRegionDrawable(b.uiIcon)).props(size(unit(7.5f)), prefWidth(unit(7.5f)), prefHeight(unit(7.5f))));
                        }
                    });
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 65f), prefWidth(440f), prefHeight(65f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .props(padding(unit(1.5f)))
                                    .children(() -> {
                                        row().props(gap(unit(1.5f)), center()).children(() -> {
                                            image(Tex.whiteui).props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3))).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                row().props(growX(), gap(unit(2)), center()).children(() -> {
                    text(posLabel).color(WebStyles.Colors.GHOST_FG);
                    button()
                            .style(WebStyles.secondary())
                            .props(padding(unit(1.5f)))
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }))
                            .children(() -> text(Core.bundle.get("feature.god-mode.core.select-position")));
                });

                button()
                        .style(WebStyles.primary())
                        .props(growX(), padding(unit(2)))
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
                        })
                        .children(() -> text(Core.bundle.get("feature.god-mode.core.place")));
            });
        });
    }
}
