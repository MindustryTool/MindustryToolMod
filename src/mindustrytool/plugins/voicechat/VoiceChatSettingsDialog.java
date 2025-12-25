package mindustrytool.plugins.voicechat;

import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Voice chat settings dialog with per-player controls.
 * Uses IntMap with player entity ID to ensure proper key uniqueness.
 */
public class VoiceChatSettingsDialog extends BaseDialog {

    private final VoiceChatManager manager;

    // Per-player settings using entity ID (int) as key for reliable lookup
    private final IntMap<Boolean> playerMuted = new IntMap<>();
    private final IntMap<Float> playerVolume = new IntMap<>();

    public VoiceChatSettingsDialog(VoiceChatManager manager) {
        super("Voice Chat Settings");
        this.manager = manager;

        addCloseButton();

        // Add Reset to defaults button
        buttons.button("Reset to defaults", Icon.refresh, () -> {
            // Reset global settings
            manager.setEnabled(true);
            manager.setMuted(false);
            manager.setSpeakerMode(VoiceChatManager.VoiceMode.ALL);
            manager.setMicMode(VoiceChatManager.VoiceMode.ALL);

            // Reset per-player settings
            playerMuted.clear();
            playerVolume.clear();

            setup();
        }).size(250f, 64f);

        setup();
    }

    private void setup() {
        cont.clear();
        cont.defaults().pad(5f);

        // Create a single scrollable content table
        Table scrollContent = new Table();
        scrollContent.defaults().pad(5f);
        scrollContent.left().top();

        // Debug Controls & Status Indicator hidden/removed

        // === GLOBAL SETTINGS ===
        scrollContent.add("[accent]Device Controls").left().padBottom(10f).row();

        // Speaker Controls
        scrollContent.table(t -> {
            t.left().marginBottom(5f);
            t.add("Volume: ").color(Color.lightGray);
            t.add().growX();

            t.button(manager.isEnabled() ? "[green]ON" : "[red]OFF", Styles.flatToggleMenut, () -> {
                manager.setEnabled(!manager.isEnabled());
                setup();
            }).size(70f, 36f).checked(manager.isEnabled());

            boolean isTeamSpeaker = manager.getSpeakerMode() == VoiceChatManager.VoiceMode.TEAM;
            String speakerMode = isTeamSpeaker ? "[accent]Team" : "All";
            t.button(speakerMode, Styles.flatBordert, () -> {
                VoiceChatManager.VoiceMode newMode = manager.getSpeakerMode() == VoiceChatManager.VoiceMode.TEAM
                        ? VoiceChatManager.VoiceMode.ALL
                        : VoiceChatManager.VoiceMode.TEAM;
                manager.setSpeakerMode(newMode);
                setup();
            }).size(90f, 36f);
        }).growX().row();

        // Microphone Controls
        scrollContent.table(t -> {
            t.left().marginBottom(5f);
            t.add("Microphone: ").color(Color.lightGray);
            t.add().growX();

            t.button(!manager.isMuted() ? "[green]ON" : "[red]OFF", Styles.flatToggleMenut, () -> {
                manager.setMuted(!manager.isMuted());
                setup();
            }).size(70f, 36f).checked(!manager.isMuted());

            boolean isTeamMic = manager.getMicMode() == VoiceChatManager.VoiceMode.TEAM;
            String micMode = isTeamMic ? "[accent]Team" : "All";
            t.button(micMode, Styles.flatBordert, () -> {
                VoiceChatManager.VoiceMode newMode = manager.getMicMode() == VoiceChatManager.VoiceMode.TEAM
                        ? VoiceChatManager.VoiceMode.ALL
                        : VoiceChatManager.VoiceMode.TEAM;
                manager.setMicMode(newMode);
                setup();
            }).size(90f, 36f);
        }).growX().row();

        // Separator
        scrollContent.image().color(Color.darkGray).fillX().height(1f).pad(10f).row();

        // === PER-PLAYER SETTINGS ===
        scrollContent.add("[accent]Player Controls").left().padBottom(10f).row();

        // List all connected players
        for (Player p : Groups.player) {
            if (p == null)
                continue;

            // Use entity ID (integer) - unique and consistent
            int entityId = p.id;
            String displayName = Strings.stripColors(p.name);
            String uuid = p.uuid(); // Still need UUID for manager calls

            // Initialize defaults only if not already set
            if (!playerMuted.containsKey(entityId))
                playerMuted.put(entityId, false);
            if (!playerVolume.containsKey(entityId))
                playerVolume.put(entityId, 100f);

            // Read current values for this player
            boolean isMuted = playerMuted.get(entityId, false);
            float volume = playerVolume.get(entityId, 100f);

            // Build row for this player
            scrollContent.table(row -> {
                row.left().defaults().pad(3f);

                // ROW 1: Avatar | Name | Toggle
                row.table(header -> {
                    header.left();
                    header.image(Icon.players).size(32f).padRight(10f);
                    header.add(displayName).left().growX();

                    // ON/OFF button - use captured values
                    header.button(!isMuted ? "[green]ON" : "[red]OFF", Styles.flatToggleMenut, () -> {
                        boolean current = playerMuted.get(entityId, false);
                        playerMuted.put(entityId, !current);
                        manager.setPlayerMuted(uuid, !current);
                        setup();
                    }).size(60f, 30f).checked(!isMuted);
                }).growX().row();

                // ROW 2: Volume Slider
                row.table(controls -> {
                    Slider slider = new Slider(0f, 100f, 5f, false);
                    slider.setValue(volume);

                    arc.scene.ui.Label volumeLabel = new arc.scene.ui.Label(
                            (int) volume + "%", Styles.outlineLabel);

                    slider.changed(() -> {
                        float v = slider.getValue();
                        playerVolume.put(entityId, v);
                        manager.setPlayerVolume(uuid, v / 100f);
                        volumeLabel.setText((int) v + "%");
                    });

                    Table labelContent = new Table();
                    labelContent.touchable = arc.scene.event.Touchable.disabled;
                    labelContent.margin(3f, 33f, 3f, 33f);
                    labelContent.add("Volume", Styles.outlineLabel).left().growX();
                    labelContent.add(volumeLabel).padLeft(10f).right();

                    controls.stack(slider, labelContent).height(40f).growX();
                }).growX();

            }).growX().padBottom(5f).row();

            scrollContent.image().color(Color.darkGray).fillX().height(1f).pad(5f).row();
        }

        if (Groups.player.isEmpty()) {
            scrollContent.add("[gray]No players connected").pad(20f).row();
        }

        // Wrap everything in a single ScrollPane
        ScrollPane scroll = new ScrollPane(scrollContent, Styles.smallPane);
        scroll.setScrollingDisabled(true, false);
        cont.add(scroll).width(450f).growY().row();
    }

    public IntMap<Boolean> getPlayerMutedMap() {
        return playerMuted;
    }

    public IntMap<Float> getPlayerVolumeMap() {
        return playerVolume;
    }
}
