package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
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

        Seq<Item> allItems = Vars.content.items();
        Readable<Seq<Item>> filteredItems = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allItems;
            }
            String lower = q.trim().toLowerCase();
            return allItems.select(item -> item.localizedName.toLowerCase().contains(lower));
        });

        children(() -> {
            column().gap(unit(2)).padding(unit(2)).children(() -> {
                row().growX().gap(unit(1)).center().children(() -> {
                    icon(Icon.zoom).size(unit(4));
                    textField(searchQuery)
                            .growX()
                            .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                });

                text(Core.bundle.get("feature.god-mode.items.select-item")).color(Color.lightGray);

                scroll().size(420f, 130f).children(() -> {
                    dynamic(filteredItems, items -> grid(6).gap(unit(1)).children(() -> {
                        if (items != null) {
                            for (Item it : items) {
                                button()
                                        .style(Styles.clearTogglei)
                                        .checked(selectedItem.map(sel -> sel == it))
                                        .onClick(() -> selectedItem.set(it))
                                        .size(unit(9), unit(9))
                                        .tooltip(it.localizedName)
                                        .children(() -> image(it.uiIcon).size(unit(6)));
                            }
                        }
                    }));
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(Color.lightGray);

                scroll().size(420f, 60f).children(() -> {
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

                row().growX().gap(unit(1)).center().children(() -> {
                    text(Core.bundle.get("feature.god-mode.items.amount") + ": ").color(Color.lightGray);
                    textField(amountString)
                            .width(unit(20))
                            .onTextChange(s -> {
                                try {
                                    int val = Integer.parseInt(s.trim());
                                    amount.set(Math.max(1, val));
                                } catch (NumberFormatException ignored) {
                                }
                            });
                });

                row().gap(unit(1)).center().children(() -> {
                    button("+10", () -> addDelta(10)).style(Styles.defaultt).size(unit(12), unit(6));
                    button("+100", () -> addDelta(100)).style(Styles.defaultt).size(unit(12), unit(6));
                    button("+1000", () -> addDelta(1000)).style(Styles.defaultt).size(unit(14), unit(6));
                    button("+10k", () -> addDelta(10000)).style(Styles.defaultt).size(unit(14), unit(6));
                    button("Max", this::setMaxAmount).style(Styles.defaultt).size(unit(12), unit(6));
                });

                button(Core.bundle.get("feature.god-mode.items.add"))
                        .style(Styles.defaultt)
                        .growX()
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
                        });
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
