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
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import java.util.Optional;

public class WavePreviewFeature extends Table implements Feature {
    private final ObjectIntMap<UnitType> currentWaveCounts = new ObjectIntMap<>();
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

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> rebuild()));

        visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame() && Vars.state.rules.waves);

        update(() -> {
            if (!visible) {
                return;
            }

            if (interval.get(30)) {
                Core.app.post(() -> {
                    updateCounts();
                    updateUI();
                });
            }
        });

        Core.app.post(() -> rebuild());
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

            Core.app.post(() -> rebuild());
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
        updateCounts();
        updateUI();
    }

    private void updateCounts() {
        if (!Vars.state.isGame()) {
            return;
        }

        currentWaveCounts.clear();
        Team enemyTeam = Vars.state.rules.waveTeam;

        // Count current enemies
        Groups.unit.each(u -> u.team == enemyTeam, u -> {
            currentWaveCounts.put(u.type, currentWaveCounts.get(u.type, 0) + 1);
        });

        nextWaveCounts.clear();
        int nextWave = Vars.state.wave;
        for (SpawnGroup group : Vars.state.rules.spawns) {
            int amount = group.getSpawned(nextWave - 1);

            if (Vars.state.isCampaign()) {
                amount = Math.max(1, group.effect == StatusEffects.boss
                        ? (int) (amount * Vars.state.getPlanet().campaignRules.difficulty.enemySpawnMultiplier)
                        : Mathf.round(amount * Vars.state.getPlanet().campaignRules.difficulty.enemySpawnMultiplier));
            }

            int spawnedf = amount;

            if (spawnedf > 0) {
                nextWaveCounts.put(group.type, nextWaveCounts.get(group.type, 0) + spawnedf);
            }
        }
    }

    private void updateUI() {
        clear();

        top().left();
        background(Tex.pane);
        setColor(1f, 1f, 1f, WavePreviewConfig.opacity());

        float scale = WavePreviewConfig.scale();

        Label title = add("@wave-preview.title").top().left().align(Align.left).style(Styles.outlineLabel).pad(4)
                .color(mindustry.graphics.Pal.accent).get();
        title.setFontScale(scale);
        row();

        Label waveLabel = add(new Label(() -> "" + Vars.state.wave)).style(Styles.outlineLabel).left().padLeft(4).get();
        waveLabel.setFontScale(scale);
        row();

        Table currentWaveTable = new Table();
        add(currentWaveTable).growX().pad(4).row();
        buildWaveTable(currentWaveTable, currentWaveCounts);

        if (!nextWaveCounts.isEmpty()) {
            image().color(mindustry.graphics.Pal.gray).height(2).growX().pad(4).row();

            Label nextWaveLabel = add(new Label(() -> "" + (Vars.state.wave + 1))).style(Styles.outlineLabel).left()
                    .padLeft(4).get();
            nextWaveLabel.setFontScale(scale);
            row();

            Table nextWaveTable = new Table();
            add(nextWaveTable).growX().pad(4).row();
            buildWaveTable(nextWaveTable, nextWaveCounts);
        }

        pack();
    }

    private void buildWaveTable(Table table, ObjectIntMap<UnitType> counts) {
        table.clear();
        float scale = WavePreviewConfig.scale();

        if (counts.isEmpty()) {
            Label l = table.add("-").style(Styles.outlineLabel).color(mindustry.graphics.Pal.gray).get();
            l.setFontScale(scale);
            return;
        }

        int i = 0;

        var keys = Seq.with(counts.keys()).sort(u -> u.health);

        for (UnitType type : keys) {
            int amount = counts.get(type);
            if (amount <= 0) {
                continue;
            }

            table.image(type.uiIcon).size(16 * 1.5f * scale).padRight(4 * scale);
            Label l = table.add(String.valueOf(amount)).style(Styles.outlineLabel).padRight(8 * scale).get();
            l.setFontScale(scale);

            if (++i % 3 == 0) {
                table.row();
            }
        }
    }
}
