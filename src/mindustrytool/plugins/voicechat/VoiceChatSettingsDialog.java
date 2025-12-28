package mindustrytool.plugins.voicechat;

import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.plugins.auth.AuthService;
import mindustrytool.plugins.auth.AuthPlugin;

/**
 * Voice chat settings dialog with per-player controls.
 * Uses IntMap with player entity ID to ensure proper key uniqueness.
 */
public class VoiceChatSettingsDialog extends BaseDialog {

    private final VoiceChatManager manager;

    // Per-player settings removed - we now read directly from manager

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
            Groups.player.each(p -> {
                manager.setPlayerMuted(String.valueOf(p.id), false);
                manager.setPlayerVolume(String.valueOf(p.id), 1.0f);
            });

            setup();
        }).size(250f, 64f);

        setup();
    }

    private void setup() {
        cont.clear();
        cont.defaults().pad(5f);

        // Login Check
        if (!AuthService.isLoggedIn()) {
            cont.add("[accent]Login Required").pad(20f).row();
            cont.add("Voice Chat requires you to be logged in.").color(Color.lightGray).pad(10f).row();
            cont.button("Login", Icon.play, () -> {
                AuthPlugin.loginDialog.show();
                hide(); // Close this dialog, user will reopen after login
            }).size(200f, 50f).pad(10f);
            return;
        }

        // Create a single scrollable content table
        Table scrollContent = new Table();
        scrollContent.defaults().pad(5f);
        scrollContent.left().top();

        // Debug Controls & Status Indicator hidden/removed

        // === GLOBAL SETTINGS ===

        // Aggressive Wake-up: Force sync whenever UI is rebuilt/shown
        manager.syncStatus();

        // Status Label (Visual Truth)
        scrollContent.table(t -> {
            t.left();
            t.add("Status: ").color(Color.lightGray);
            t.add(manager.getStatusText()).padLeft(5f);
        }).padBottom(10f).row();

        scrollContent.add("[accent]Device Controls").left().padBottom(10f).row();

        // Speaker Controls
        scrollContent.table(t -> {
            t.left().marginBottom(5f);
            t.add("Volume: ").color(Color.lightGray);
            t.add().growX();

            // 1. 3D Audio Toggle (First)
            t.button(manager.isSpatialAudioEnabled() ? "3D: [green]ON" : "3D: [red]OFF", Styles.flatToggleMenut, () -> {
                manager.setSpatialAudioEnabled(!manager.isSpatialAudioEnabled());
                setup();
            }).size(80f, 36f).checked(manager.isSpatialAudioEnabled());

            // 2. Main Enable Toggle
            t.button(manager.isEnabled() ? "[green]ON" : "[red]OFF", Styles.flatToggleMenut, () -> {
                manager.setEnabled(!manager.isEnabled());
                setup();
            }).size(70f, 36f).checked(manager.isEnabled());

            // 3. Team/All Mode
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

        // Master Volume Control
        // Master Volume Control (Styled)
        scrollContent.table(t -> {
            t.left().marginBottom(5f);

            // Container for Slider Stack
            t.table(controls -> {
                Slider slider = new Slider(0f, 200f, 5f, false);
                slider.setValue(manager.getMasterVolume() * 100f);

                // Label for percentage
                arc.scene.ui.Label volumeLabel = new arc.scene.ui.Label(
                        (int) (manager.getMasterVolume() * 100f) + "%", Styles.outlineLabel);

                slider.changed(() -> {
                    float v = slider.getValue();
                    manager.setMasterVolume(v / 100f);
                    volumeLabel.setText((int) v + "%");
                });

                // Overlay Text (Total Volume | XX%)
                Table labelContent = new Table();
                labelContent.touchable = arc.scene.event.Touchable.disabled;
                labelContent.margin(3f, 33f, 3f, 33f); // margin left/right inside slider
                labelContent.add("Total Volume", Styles.outlineLabel).left().growX();
                labelContent.add(volumeLabel).padLeft(10f).right();

                controls.stack(slider, labelContent).height(40f).growX();
            }).growX();

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
            String playerId = String.valueOf(p.id); // Use ID to fix duplicate UUID issues
            String displayName = Strings.stripColors(p.name);

            // Read current values directly from Manager (Source of Truth)
            boolean isMuted = manager.isPlayerMuted(playerId);
            float volume = manager.getPlayerVolume(playerId) * 100f; // Convert 0-1.0 to 0-100

            // Build row for this player
            scrollContent.table(row -> {
                row.left().defaults().pad(3f);

                // ROW 1: Avatar | Name | Toggle
                row.table(header -> {
                    header.left();
                    header.image(Icon.players).size(32f).padRight(10f);
                    header.add(displayName).left().growX();

                    // ON/OFF button
                    header.button(!isMuted ? "[green]ON" : "[red]OFF", Styles.flatToggleMenut, () -> {
                        manager.setPlayerMuted(playerId, !isMuted);
                        setup(); // Rebuild UI to reflect state
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
                        manager.setPlayerVolume(playerId, v / 100f);
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
}
