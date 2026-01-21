package mindustrytool.features.display.wavepreview;

import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.util.Align;
import arc.util.Interval;
import arc.util.Log;
import mindustry.Vars;
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
import arc.scene.ui.Dialog;

public class WavePreviewFeature extends Table implements Feature {
    private final ObjectIntMap<UnitType> currentWaveCounts = new ObjectIntMap<>();
    private final ObjectIntMap<UnitType> nextWaveCounts = new ObjectIntMap<>();

    private final Interval interval = new Interval();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("Wave Preview")
                .description("Displays current and next wave composition")
                .icon(Icon.units)
                .order(1)
                .quickAccess(true)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        name = "wave-preview";

        Events.run(WorldLoadEvent.class, this::rebuild);

        visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

        update(() -> {
            if (!visible) {
                return;
            }

            if (interval.get(30)) {
                updateCounts();
                updateUI();
            }
        });

        rebuild();
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

            rebuild();
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
        if (!Vars.state.isGame() || !Vars.state.rules.waves) {
            return;
        }

        currentWaveCounts.clear();
        Team enemyTeam = Vars.state.rules.waveTeam;

        // Count current enemies
        Groups.unit.each(u -> u.team == enemyTeam, u -> {
            currentWaveCounts.put(u.type, currentWaveCounts.get(u.type, 0) + 1);
        });

        nextWaveCounts.clear();
        int nextWave = Vars.state.wave + 1;

        for (SpawnGroup group : Vars.state.rules.spawns) {
            int amount = group.getSpawned(nextWave - 1);
            if (amount > 0) {
                nextWaveCounts.put(group.type, nextWaveCounts.get(group.type, 0) + amount);
            }
        }
    }

    private void updateUI() {
        clear();
        top().left();
        background(Tex.paneRight);

        add("Waves").top().left().align(Align.left).style(Styles.outlineLabel).pad(4)
                .color(mindustry.graphics.Pal.accent).row();

        add(new Label(() -> "" + Vars.state.wave)).style(Styles.outlineLabel).left().padLeft(4).row();

        Table currentWaveTable = new Table();
        add(currentWaveTable).growX().pad(4).row();
        buildWaveTable(currentWaveTable, currentWaveCounts);

        if (!nextWaveCounts.isEmpty()) {
            image().color(mindustry.graphics.Pal.gray).height(2).growX().pad(4).row();

            add(new Label(() -> "" + (Vars.state.wave + 1))).style(Styles.outlineLabel).left().padLeft(4).row();

            Table nextWaveTable = new Table();
            add(nextWaveTable).growX().pad(4).row();
            buildWaveTable(nextWaveTable, nextWaveCounts);
        }

        pack();
    }

    private void buildWaveTable(Table table, ObjectIntMap<UnitType> counts) {
        table.clear();

        if (counts.isEmpty()) {
            table.add("-").style(Styles.outlineLabel).color(mindustry.graphics.Pal.gray);
            return;
        }

        int i = 0;
        for (UnitType type : counts.keys()) {
            int amount = counts.get(type);
            if (amount <= 0)
                continue;

            table.image(type.uiIcon).size(16 * 1.5f).padRight(4);
            table.add(String.valueOf(amount)).style(Styles.outlineLabel).padRight(8);

            if (++i % 3 == 0)
                table.row();
        }
    }
}
