package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.type.StatusEffect;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

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

        Seq<StatusEffect> allEffects = Vars.content.statusEffects();
        Readable<Seq<StatusEffect>> filteredEffects = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allEffects;
            }
            String lower = q.trim().toLowerCase();
            return allEffects.select(e -> e.localizedName.toLowerCase().contains(lower));
        });

        children(() -> {
            column().gap(unit(2)).padding(unit(2)).children(() -> {
                row().growX().gap(unit(1)).center().children(() -> {
                    icon(Icon.zoom).size(unit(4));
                    textField(searchQuery)
                            .growX()
                            .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                });

                text(Core.bundle.get("feature.god-mode.effects.select-effect")).color(Color.lightGray);

                scroll().size(420f, 140f).children(() -> {
                    dynamic(filteredEffects, effects -> grid(6).gap(unit(1)).children(() -> {
                        if (effects != null) {
                            for (StatusEffect ef : effects) {
                                button()
                                        .style(Styles.clearTogglei)
                                        .checked(selectedEffect.map(sel -> sel == ef))
                                        .onClick(() -> selectedEffect.set(ef))
                                        .size(unit(9), unit(9))
                                        .tooltip(ef.localizedName)
                                        .children(() -> image(ef.uiIcon).size(unit(6)));
                            }
                        }
                    }));
                });

                row().growX().gap(unit(1)).center().children(() -> {
                    text(Core.bundle.get("feature.god-mode.effects.duration") + ": ").color(Color.lightGray);
                    textField(durationString)
                            .width(unit(16))
                            .onTextChange(s -> {
                                try {
                                    float val = Float.parseFloat(s.trim());
                                    durationSeconds.set(Math.max(1f, val));
                                } catch (NumberFormatException ignored) {
                                }
                            });

                    button("10s", () -> setDuration(10f)).style(Styles.defaultt).size(unit(10), unit(6));
                    button("60s", () -> setDuration(60f)).style(Styles.defaultt).size(unit(10), unit(6));
                    button("5m", () -> setDuration(300f)).style(Styles.defaultt).size(unit(10), unit(6));
                    button(Core.bundle.get("feature.god-mode.effects.infinite"), () -> setDuration(999999f))
                            .style(Styles.defaultt)
                            .size(unit(16), unit(6));
                });

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("feature.god-mode.effects.apply"))
                            .style(Styles.defaultt)
                            .growX()
                            .onClick(() -> {
                                StatusEffect ef = selectedEffect.get();
                                float sec = durationSeconds.get() != null ? durationSeconds.get() : 60f;
                                if (ef != null) {
                                    provider.applyEffect(ef, sec * 60f);
                                }
                                hide();
                            });

                    button(Core.bundle.get("feature.god-mode.effects.clear"))
                            .style(Styles.defaultt)
                            .growX()
                            .onClick(() -> {
                                StatusEffect ef = selectedEffect.get();
                                if (ef != null) {
                                    provider.clearEffect(ef);
                                }
                                hide();
                            });
                });
            });
        });
    }

    private void setDuration(float seconds) {
        durationSeconds.set(seconds);
        durationString.set(seconds == (long) seconds ? String.valueOf((long) seconds) : String.valueOf(seconds));
    }
}
