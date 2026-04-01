package mindustrytool.features.godmode;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.StatusEffect;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GodModeEffectDialog extends BaseDialog {
    public GodModeEffectDialog(BiConsumer<StatusEffect, Float> onApply, Consumer<StatusEffect> onClear) {
        super("Effects");
        addCloseButton();

        cont.table(t -> {
            float[] duration = { 600f };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.add("Duration (ticks): ");
                ctrl.field(String.valueOf(duration[0]), s -> {
                    try {
                        duration[0] = Float.parseFloat(s);
                    } catch (NumberFormatException ignored) {
                    }
                }).width(100);
            }).maxWidth(800).row();

            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (StatusEffect effect : Vars.content.statusEffects()) {
                    p.table(e -> {
                        e.background(Tex.underline);
                        e.image(effect.uiIcon).size(32).padRight(5);
                        e.add(effect.localizedName).width(120).left().ellipsis(true);

                        e.button(Icon.add, Styles.clearNonei, () -> {
                            onApply.accept(effect, duration[0]);
                        }).size(40);

                        e.button(Icon.cancel, Styles.clearNonei, () -> {
                            onClear.accept(effect);
                        }).size(40);
                    }).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow();
        }).growX().maxWidth(800).grow();
    }
}
