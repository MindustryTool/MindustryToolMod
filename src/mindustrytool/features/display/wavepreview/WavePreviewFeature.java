package mindustrytool.features.display.wavepreview;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Interval;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.SpawnGroup;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import java.util.Optional;

public class WavePreviewFeature extends Table implements Feature {
    private final ObjectIntMap<UnitType> nextWaveCounts = new ObjectIntMap<>();

    private final Interval interval = new Interval();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.wave-preview.name")
                .description("@feature.wave-preview.description")
                .icon(Icon.units)
                .order(1)
                .quickAccess(true)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        name = "wave-preview";

        Events.run(WorldLoadEvent.class, () -> Core.app.post(this::rebuild));

        visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame() && Vars.state.rules.waves);

        update(() -> {
            if (!visible) {
                return;
            }

            if (interval.get(60)) {
                rebuild();
            }
        });

        Core.app.post(this::rebuild);
    }

    @Override
    public void onEnable() {
        if (Vars.ui.hudGroup != null) {
            Stack parent = Vars.ui.hudGroup.find("waves/editor");

            if (parent == null) {
                Log.err("WavePreviewFeature: waves/editor not found");
                return;
            }

            Table waves = parent.find("waves");

            if (waves == null) {
                Log.err("WavePreviewFeature: waves not found");
                return;
            }

            waves.row();
            waves.add(this).growX().padTop(10f);

            Core.app.post(this::rebuild);
        }
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.empty();
    }

    private void rebuild() {
        if (!Vars.state.isGame()) {
            return;
        }

        nextWaveCounts.clear();

        for (SpawnGroup group : Vars.state.rules.spawns) {
            if (group.type == null) {
                continue;
            }

            int amount = group.getSpawned(Vars.state.wave - 1);

            if (amount == 0) {
                continue;
            }

            if (Vars.state.isCampaign()) {
                amount = Math.max(1, group.effect == StatusEffects.boss
                        ? (int) (amount * Vars.state.getPlanet().campaignRules.difficulty.enemySpawnMultiplier)
                        : Mathf.round(amount * Vars.state.getPlanet().campaignRules.difficulty.enemySpawnMultiplier));
            }

            if (amount > 0) {
                nextWaveCounts.put(group.type, nextWaveCounts.get(group.type, 0) + amount);
            }
        }

        clear();
        top().left();
        background(Tex.pane);
        setColor(1f, 1f, 1f, WavePreviewConfig.opacity());

        float scale = WavePreviewConfig.scale();

        Label title = add("@wave-preview.title").top().left().align(Align.left).style(Styles.outlineLabel).pad(4)
                .color(mindustry.graphics.Pal.accent).get();
        title.setFontScale(scale);
        row();

        if (!nextWaveCounts.isEmpty()) {
            Label nextWaveLabel = add(new Label(() -> "" + Vars.state.wave)).style(Styles.outlineLabel).left()
                    .padLeft(4).get();
            nextWaveLabel.setFontScale(scale);
            row();

            Table nextWaveTable = new Table();
            add(nextWaveTable).growX().pad(4).row();

            int i = 0;
            var keys = Seq.with(nextWaveCounts.keys()).sort(u -> u.health);

            for (UnitType type : keys) {
                int amount = nextWaveCounts.get(type);

                nextWaveTable.image(type.uiIcon).size(16 * 1.5f * scale).padRight(4 * scale);
                Label l = nextWaveTable.add(String.valueOf(amount)).style(Styles.outlineLabel).padRight(8 * scale)
                        .get();
                l.setFontScale(scale);

                if (++i % 3 == 0) {
                    nextWaveTable.row();
                }
            }
        }

        pack();
    }
}
