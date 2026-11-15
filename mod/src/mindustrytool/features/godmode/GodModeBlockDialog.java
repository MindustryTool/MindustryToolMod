package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class GodModeBlockDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<Category> selectedCategory = Signal.of(null);
    private final Signal<Block> selectedBlock;
    private final Signal<Integer> rotation = Signal.of(0);
    private final Signal<Team> selectedTeam = Signal.of(Vars.player != null ? Vars.player.team() : Team.sharded);
    private final Signal<Float> posX = Signal.of(Vars.player != null ? Vars.player.x : 0f);
    private final Signal<Float> posY = Signal.of(Vars.player != null ? Vars.player.y : 0f);

    public GodModeBlockDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.block.title"));

        name("godModeBlockDialog");
        addCloseButton();
        closeOnBack();

        Seq<Block> allBlocks = Vars.content != null && Vars.content.blocks() != null
                ? Vars.content.blocks().select(GodModeBlockDialog::isValidBlock)
                : new Seq<>();

        selectedBlock = Signal.of(allBlocks.size > 0 ? allBlocks.first() : null);

        Readable<Seq<Block>> filteredBlocks = Signal.computed(() -> {
            String q = searchQuery.get();
            Category cat = selectedCategory.get();
            String lower = q != null ? q.trim().toLowerCase() : "";
            return allBlocks.select(b -> {
                boolean matchesCategory = cat == null || b.category == cat;
                boolean matchesQuery = lower.isEmpty()
                        || (b.localizedName != null && b.localizedName.toLowerCase().contains(lower));
                return matchesCategory && matchesQuery;
            });
        });

        Computed<String> posLabel = Signal.computed(() -> Core.bundle.format("feature.god-mode.block.position",
                Math.round(posX.get() != null ? posX.get() : 0f),
                Math.round(posY.get() != null ? posY.get() : 0f)));

        children(() -> {
            column().props(padding(unit(3)), prefWidth(440f), gap(unit(2.5f))).children(() -> {
                row().props(growX(), padding(unit(1.5f)), rounded(unit(2), WebStyles.Colors.SECONDARY_BG),
                        border(1.5f, WebStyles.Colors.BORDER_INPUT), gap(unit(1.5f)), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)),
                                    color(WebStyles.Colors.GHOST_FG));
                            textField(searchQuery)
                                    .props(style(WebStyles.clearInput()), growX(), height(unit(8)), prefHeight(unit(8)),
                                            placeholder(Core.bundle.get("feature.god-mode.common.search")));
                        });

                text(Core.bundle.get("feature.god-mode.block.select-block")).props(color(WebStyles.Colors.GHOST_FG));

                wrap().props(left(), gap(unit(1.5f))).children(() -> {
                    button(Core.bundle.get("feature.god-mode.block.category.all"), () -> selectedCategory.set(null))
                            .props(style(WebStyles.filterChipText()), checked(selectedCategory.map(c -> c == null)),
                                    padding(unit(1.5f)));

                    for (Category cat : Category.all) {
                        String catName = Core.bundle.get("category." + cat.name() + ".name",
                                Strings.capitalize(cat.name()));
                        button(catName, () -> selectedCategory.set(cat))
                                .props(style(WebStyles.filterChipText()), checked(selectedCategory.map(c -> c == cat)),
                                        padding(unit(1.5f)));
                    }
                });

                scroll().props(size(440f, 130f), prefHeight(130f)).children(() -> {
                    dynamic(filteredBlocks, blocks -> wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        if (blocks != null) {
                            for (Block b : blocks) {
                                button()
                                        .onClick(() -> selectedBlock.set(b))
                                        .props(style(WebStyles.filterChip()),
                                                checked(selectedBlock.map(sel -> sel == b)), size(unit(10), unit(10)),
                                                prefWidth(unit(10)), prefHeight(unit(10)), padding(unit(1)),
                                                tooltip(b.localizedName))
                                        .children(() -> image(new TextureRegionDrawable(b.uiIcon)).props(
                                                size(unit(6.5f)), prefWidth(unit(6.5f)), prefHeight(unit(6.5f))));
                            }
                        }
                    }));
                });

                when(selectedBlock.map(b -> b != null && b.rotate)).thenDo(() -> {
                    column().props(growX(), gap(unit(1))).children(() -> {
                        text(Core.bundle.get("feature.god-mode.block.rotation"))
                                .props(color(WebStyles.Colors.GHOST_FG));
                        row().props(gap(unit(1.5f)), left()).children(() -> {
                            int[] rots = { 0, 1, 2, 3 };
                            String[] rotLabels = {
                                    Core.bundle.get("feature.god-mode.block.rotation.deg0"),
                                    Core.bundle.get("feature.god-mode.block.rotation.deg90"),
                                    Core.bundle.get("feature.god-mode.block.rotation.deg180"),
                                    Core.bundle.get("feature.god-mode.block.rotation.deg270")
                            };
                            for (int i = 0; i < rots.length; i++) {
                                int r = rots[i];
                                String lbl = rotLabels[i];
                                button(lbl, () -> rotation.set(r))
                                        .props(style(WebStyles.filterChipText()),
                                                checked(rotation.map(curr -> curr != null && curr == r)),
                                                padding(unit(1.5f)));
                            }
                        });
                    });
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).props(color(WebStyles.Colors.GHOST_FG));

                scroll().props(size(440f, 65f), prefHeight(65f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .onClick(() -> selectedTeam.set(t))
                                    .props(style(WebStyles.filterChip()), checked(selectedTeam.map(sel -> sel == t)),
                                            padding(unit(1.5f)))
                                    .children(() -> {
                                        row().props(gap(unit(1.5f)), center()).children(() -> {
                                            image(Tex.whiteui).props(size(unit(3)), prefWidth(unit(3)),
                                                    prefHeight(unit(3)), color(t.color));
                                            text(t.localized()).props(color(t.color));
                                        });
                                    });
                        }
                    });
                });

                row().props(growX(), gap(unit(2)), center()).children(() -> {
                    text(posLabel).props(color(WebStyles.Colors.GHOST_FG));
                    button()
                            .props(style(WebStyles.secondary()), padding(unit(1.5f)))
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }))
                            .children(() -> text(Core.bundle.get("feature.god-mode.block.select-position")));
                });

                button()
                        .props(style(WebStyles.primary()), growX(), padding(unit(2)))
                        .onClick(() -> {
                            Block b = selectedBlock.get();
                            Team tm = selectedTeam.get();
                            int rot = (b != null && b.rotate && rotation.get() != null) ? rotation.get() : 0;
                            float x = posX.get() != null ? posX.get() : 0f;
                            float y = posY.get() != null ? posY.get() : 0f;
                            if (b != null && tm != null) {
                                boolean placed = provider.placeBlock(b, tm, x, y, rot);
                                if (!placed && Vars.ui != null && Vars.ui.hudfrag != null) {
                                    Vars.ui.hudfrag.showToast(Core.bundle.get("feature.god-mode.block.invalid-tile"));
                                }
                            }
                            hide();
                        })
                        .children(() -> text(Core.bundle.get("feature.god-mode.block.place")));
            });
        });
    }

    private static boolean isValidBlock(@Nullable Block b) {
        if (b == null || b.isAir() || !b.synthetic() || !b.isPlaceable()) {
            return false;
        }
        if (b.buildVisibility == BuildVisibility.hidden) {
            return false;
        }
        if (b.uiIcon == null) {
            return false;
        }
        if (Core.atlas != null && (!Core.atlas.isFound(b.uiIcon) || b.uiIcon == Core.atlas.find("error"))) {
            return false;
        }
        return true;
    }
}
