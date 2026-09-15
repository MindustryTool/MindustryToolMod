package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.signal.Readable;
import solim.signal.Signal;

public class GodModeItemsDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<Item> selectedItem = Signal.of(Vars.content.items().size > 0 ? Vars.content.items().first() : null);
    private final Signal<Team> selectedTeam = Signal.of(Vars.player != null ? Vars.player.team() : Team.sharded);
    private final Signal<Integer> amount = Signal.of(1000);
    private final Signal<String> amountString = Signal.of("1000");

    public GodModeItemsDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.items.title"));

        name("godModeItemsDialog");
        addCloseButton();
        closeOnBack();

        amountString.subscribe(s -> {
            if (s != null) {
                try {
                    int val = Integer.parseInt(s.trim());
                    amount.set(Math.max(1, val));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        Seq<Item> allItems = Vars.content.items();
        Readable<Seq<Item>> filteredItems = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allItems;
            }
            String lower = q.trim().toLowerCase();
            return allItems.select(item -> item.localizedName.toLowerCase().contains(lower));
        });

        children(() -> {
            column().gap(unit(2.5f)).padding(unit(3)).children(() -> {
                row().growX().gap(unit(1.5f))
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .center()
                        .children(() -> {
                            icon(Icon.zoom).size(unit(4.5f)).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .growX()
                                    .height(unit(8))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                        });

                text(Core.bundle.get("feature.god-mode.items.select-item")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 130f).children(() -> {
                    dynamic(filteredItems, items -> wrap().left().gap(unit(1.5f)).children(() -> {
                        if (items != null) {
                            for (Item it : items) {
                                button()
                                        .style(WebStyles.filterChip())
                                        .checked(selectedItem.map(sel -> sel == it))
                                        .onClick(() -> selectedItem.set(it))
                                        .size(unit(10), unit(10))
                                        .padding(unit(1))
                                        .tooltip(it.localizedName)
                                        .children(() -> image(new TextureRegionDrawable(it.uiIcon)).size(unit(6.5f)));
                            }
                        }
                    }));
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 65f).children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .padding(unit(1.5f))
                                    .children(() -> {
                                        row().gap(unit(1.5f)).center().children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                row().growX().gap(unit(2)).center().children(() -> {
                    text(Core.bundle.get("feature.god-mode.items.amount") + ": ").color(WebStyles.Colors.GHOST_FG);

                    row()
                            .padding(unit(1))
                            .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                            .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                            .children(() -> {
                                textField(amountString)
                                        .width(unit(22))
                                        .height(unit(8))
                                        .style(WebStyles.clearInput());
                            });
                });

                wrap().gap(unit(1.5f)).center().children(() -> {
                    button(() -> addDelta(10)).style(WebStyles.outline()).padding(unit(1)).size(unit(13), unit(7)).children(() -> text("+10"));
                    button(() -> addDelta(100)).style(WebStyles.outline()).padding(unit(1)).size(unit(13), unit(7)).children(() -> text("+100"));
                    button(() -> addDelta(1000)).style(WebStyles.outline()).padding(unit(1)).size(unit(15), unit(7)).children(() -> text("+1000"));
                    button(() -> addDelta(10000)).style(WebStyles.outline()).padding(unit(1)).size(unit(15), unit(7)).children(() -> text("+10k"));
                    button(this::setMaxAmount).style(WebStyles.secondary()).padding(unit(1)).size(unit(13), unit(7)).children(() -> text("Max"));
                });

                button()
                        .style(WebStyles.primary())
                        .growX()
                        .padding(unit(2))
                        .onClick(() -> {
                            Item it = selectedItem.get();
                            Team tm = selectedTeam.get();
                            int count = amount.get() != null ? amount.get() : 1000;
                            if (tm != null && tm.core() == null) {
                                if (Vars.ui != null && Vars.ui.hudfrag != null) {
                                    Vars.ui.hudfrag.showToast(Core.bundle.get("feature.god-mode.items.no-core"));
                                }
                            }
                            if (it != null && tm != null) {
                                provider.addItems(it, count, tm);
                            }
                            hide();
                        })
                        .children(() -> text(Core.bundle.get("feature.god-mode.items.add")));
            });
        });
    }

    private void addDelta(int delta) {
        int current = amount.get() != null ? amount.get() : 0;
        int next = Math.max(1, current + delta);
        amount.set(next);
        amountString.set(String.valueOf(next));
    }

    private void setMaxAmount() {
        Team tm = selectedTeam.get();
        int max = tm != null && tm.core() != null ? tm.core().storageCapacity : 20000;
        amount.set(max);
        amountString.set(String.valueOf(max));
    }
}
