package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.width;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.type.StatusEffect;
import mindustrytool.components.WebStyles;
import solim.modifier.Values;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;
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

        Effect.of(durationString, s -> {
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
            column().props(padding(unit(3)), gap(unit(2.5f))).children(() -> {
                row().props(growX(), padding(unit(1.5f)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT), gap(unit(1.5f)), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)), color(WebStyles.Colors.GHOST_FG));
                            textField(searchQuery)
                                    .props(style(WebStyles.clearInput()), growX(), height(unit(8)), prefHeight(unit(8)), Values.placeholder(Core.bundle.get("feature.god-mode.common.search")));
                        });

                text(Core.bundle.get("feature.god-mode.effects.select-effect")).props(color(WebStyles.Colors.GHOST_FG));

                scroll().props(size(440f, 140f), prefWidth(440f), prefHeight(140f)).children(() -> {
                    dynamic(filteredEffects, effects -> wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        if (effects != null) {
                            for (StatusEffect ef : effects) {
                                button()
                                        .onClick(() -> selectedEffect.set(ef))
                                        .props(style(WebStyles.filterChip()), checked(selectedEffect.map(sel -> sel == ef)), size(unit(10), unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), padding(unit(1)), tooltip(ef.localizedName))
                                        .children(() -> image(new TextureRegionDrawable(ef.uiIcon)).props(size(unit(6.5f)), prefWidth(unit(6.5f)), prefHeight(unit(6.5f))));
                            }
                        }
                    }));
                });

                wrap().props(gap(unit(2)), center()).children(() -> {
                    row().props(gap(unit(1.5f)), center()).children(() -> {
                        text(Core.bundle.get("feature.god-mode.effects.duration") + ": ").props(color(WebStyles.Colors.GHOST_FG));

                        row()
                                .props(padding(unit(1)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT))
                                .children(() -> {
                                    textField(durationString)
                                             .props(style(WebStyles.clearInput()), width(unit(18)), prefWidth(unit(18)), height(unit(8)), prefHeight(unit(8)));
                                });
                    });

                    wrap().props(gap(unit(1.5f)), center()).children(() -> {
                        button(() -> setDuration(10f)).props(style(WebStyles.outline()), padding(unit(1)), size(unit(11), unit(7)), prefWidth(unit(11)), prefHeight(unit(7))).children(() -> text("10s"));
                        button(() -> setDuration(60f)).props(style(WebStyles.outline()), padding(unit(1)), size(unit(11), unit(7)), prefWidth(unit(11)), prefHeight(unit(7))).children(() -> text("60s"));
                        button(() -> setDuration(300f)).props(style(WebStyles.outline()), padding(unit(1)), size(unit(11), unit(7)), prefWidth(unit(11)), prefHeight(unit(7))).children(() -> text("5m"));
                        button(() -> setDuration(999999f))
                                .props(style(WebStyles.secondary()), padding(unit(1)), size(unit(16), unit(7)), prefWidth(unit(16)), prefHeight(unit(7)))
                                .children(() -> text(Core.bundle.get("feature.god-mode.effects.infinite")));
                    });
                });

                row().props(growX(), gap(unit(2))).children(() -> {
                    button()
                            .props(style(WebStyles.primary()), growX(), padding(unit(2)))
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
                            .props(style(WebStyles.danger()), growX(), padding(unit(2)))
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