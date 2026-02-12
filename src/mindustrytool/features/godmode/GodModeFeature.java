package mindustrytool.features.godmode;

import arc.Events;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.PlayEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

public class GodModeFeature extends Table implements Feature {

    private GodModeProvider internal = new InternalGodModeProvider();
    private JSGodModeProvider js = new JSGodModeProvider();

    private GodModeProvider provider = null;
    private boolean useJS = false;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.god-mode.name")
                .description("@feature.god-mode.description")
                .enabledByDefault(false)
                .icon(Icon.defense)
                .build();
    }

    @Override
    public void init() {
        rebuild();

        Events.run(PlayEvent.class, this::switchProvider);
        Events.run(StateChangeEvent.class, this::switchProvider);
    }

    private void switchProvider() {
        if (internal.isAvailable()) {
            provider = internal;
        } else if (js.isAvailable()) {
            provider = js;
        }

        rebuild();
    }

    @Override
    public void onEnable() {
        if (Vars.ui.hudGroup != null) {
            Stack parent = Vars.ui.hudGroup.find("waves/editor");

            if (parent == null) {
                Log.err("GodModeFeature: waves/editor not found");
                return;
            }

            Table waves = parent.find("waves");

            if (waves == null) {
                Log.err("GodModeFeature: waves not found");
                return;
            }

            waves.row();
            waves.add(this).growX().padTop(10f);
        }
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        BaseDialog dialog = new BaseDialog("@feature.god-mode.settings");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.add("Provider: ").padRight(10);

            ButtonGroup<TextButton> group = new ButtonGroup<>();

            t.button("Internal", Styles.togglet, () -> {
                useJS = false;
                provider = internal;
                rebuild();
            }).group(group).checked(!useJS).disabled(b -> !internal.isAvailable()).size(120, 50);

            t.button("JS", Styles.togglet, () -> {
                useJS = true;
                provider = js;
                rebuild();
            }).group(group).checked(useJS).disabled(b -> !js.isAvailable()).size(120, 50);

        }).row();

        return Optional.of(dialog);
    }

    private void rebuild() {
        clear();
        background(Tex.pane);

        if (provider != null) {
            provider.build(this);
        } else {
            add("Unavailable");
        }
    }
}
