package mindustrytool.features.godmode;

import arc.Events;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.PlayEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

public class GodModeFeature extends Table implements Feature {

    GodModeProvider internal = new InternalGodModeProvider();
    JSGodModeProvider js = new JSGodModeProvider();

    GodModeProvider provider = null;
    boolean useJS = false;

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
        Timer.schedule(this::switchProvider, 60, 60);
    }

    private void switchProvider() {
        if (provider != null && provider.isAvailable()) {
            return;
        }

        if (js.isAvailable()) {
            provider = js;
        } else if (internal.isAvailable()) {
            provider = internal;
        } else {
            provider = null;
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
        return Optional.of(new GodModeSettingsDialog(this));
    }

    void rebuild() {
        clear();
        background(Tex.pane);

        if (provider != null) {
            provider.build(this);
        } else {
            add("Unavailable").growX().labelAlign(Align.center);
        }
    }
}
