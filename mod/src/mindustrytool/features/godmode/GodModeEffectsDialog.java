package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.type.StatusEffect;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class GodModeEffectsDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<StatusEffect> selectedEffect = Signal.of(Vars.content.statusEffects().size > 0 ? Vars.content.statusEffects().first() : null);
    private final Signal<Float> durationSeconds = Signal.of(60f);
    private final Signal<String> durationString = Signal.of("60");

    public GodModeEffectsDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.effects.title"));

        name("godModeEffectsDialog");
        addCloseButton();
        closeOnBack();

        durationString.subscribe(s -> {
            if (s != null) {
                try {
                    float val = Float.parseFloat(s.trim());
                    durationSeconds.set(Math.max(1f, val));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        Seq<StatusEffect> allEffects = Vars.content.statusEffects();
        Readable<Seq<StatusEffect>> filteredEffects = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allEffects;
            }
            String lower = q.trim().toLowerCase();
            return allEffects.select(e -> e.localizedName.toLowerCase().contains(lower));
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

                text(Core.bundle.get("feature.god-mode.effects.select-effect")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 140f).children(() -> {
                    dynamic(filteredEffects, effects -> wrap().left().gap(unit(1.5f)).children(() -> {
                        if (effects != null) {
                            for (StatusEffect ef : effects) {
                                button()
                                        .style(WebStyles.filterChip())
                                        .checked(selectedEffect.map(sel -> sel == ef))
                                        .onClick(() -> selectedEffect.set(ef))
                                        .size(unit(10), unit(10))
                                        .padding(unit(1))
                                        .tooltip(ef.localizedName)
                                        .children(() -> image(new TextureRegionDrawable(ef.uiIcon)).size(unit(6.5f)));
                            }
                        }
                    }));
                });

                wrap().gap(unit(2)).center().children(() -> {
                    row().gap(unit(1.5f)).center().children(() -> {
                        text(Core.bundle.get("feature.god-mode.effects.duration") + ": ").color(WebStyles.Colors.GHOST_FG);

                        row()
                                .padding(unit(1))
                                .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                                .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                                .children(() -> {
                                    textField(durationString)
                                            .width(unit(18))
                                            .height(unit(8))
                                            .style(WebStyles.clearInput());
                                });
                    });

                    wrap().gap(unit(1.5f)).center().children(() -> {
                        button(() -> setDuration(10f)).style(WebStyles.outline()).padding(unit(1)).size(unit(11), unit(7)).children(() -> text("10s"));
                        button(() -> setDuration(60f)).style(WebStyles.outline()).padding(unit(1)).size(unit(11), unit(7)).children(() -> text("60s"));
                        button(() -> setDuration(300f)).style(WebStyles.outline()).padding(unit(1)).size(unit(11), unit(7)).children(() -> text("5m"));
                        button(() -> setDuration(999999f))
                                .style(WebStyles.secondary())
                                .padding(unit(1))
                                .size(unit(16), unit(7))
                                .children(() -> text(Core.bundle.get("feature.god-mode.effects.infinite")));
                    });
                });

                row().growX().gap(unit(2)).children(() -> {
                    button()
                            .style(WebStyles.primary())
                            .growX()
                            .padding(unit(2))
                            .onClick(() -> {
                                StatusEffect ef = selectedEffect.get();
                                float sec = durationSeconds.get() != null ? durationSeconds.get() : 60f;
                                if (ef != null) {
                                    provider.applyEffect(ef, sec * 60f);
                                }
                                hide();
                            })
                            .children(() -> text(Core.bundle.get("feature.god-mode.effects.apply")));

                    button()
                            .style(WebStyles.danger())
                            .growX()
                            .padding(unit(2))
                            .onClick(() -> {
                                StatusEffect ef = selectedEffect.get();
                                if (ef != null) {
                                    provider.clearEffect(ef);
                                }
                                hide();
                            })
                            .children(() -> text(Core.bundle.get("feature.god-mode.effects.clear")));
                });
            });
        });
    }

    private void setDuration(float seconds) {
        durationSeconds.set(seconds);
        durationString.set(seconds == (long) seconds ? String.valueOf((long) seconds) : String.valueOf(seconds));
    }
}
