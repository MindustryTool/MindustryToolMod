package mindustrytool.features.tools.quickaccess;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.Feature;
import mindustrytool.features.content.browser.BrowserFeature;

public class QuickAccessFeature implements Feature {

    private FlipButton flipButton;
    private Element waveInfoTable;

    @Override
    public String getName() {
        return "Quick Access Tools";
    }

    @Override
    public void init() {
        Events.run(WorldLoadEvent.class, this::buildUI);
    }

    private void buildUI() {
        // Post to main thread to ensure HUD is initialized
        Core.app.post(() -> {
            if (Vars.ui.hudGroup == null)
                return;

            // Find the wave info table (statustable)
            waveInfoTable = Vars.ui.hudGroup.find("statustable");
            // Soft check: if statustable is missing (e.g. main menu), still allow build but
            // might be hidden

            // Remove old if exists (double check for safety)
            Element existing = Vars.ui.hudGroup.find("quick-access-tools");
            if (existing != null)
                existing.remove();

            flipButton = new FlipButton();

            Vars.ui.hudGroup.fill(cont -> {
                cont.name = "quick-access-tools";
                cont.top().left();
                cont.visible(() -> Vars.ui.hudfrag.shown && !Vars.ui.minimapfrag.shown()
                        && !Vars.state.isEditor() // Hide in map editor
                        && waveInfoTable.hasParent() && waveInfoTable.visible);

                cont.table(Tex.buttonEdge4, pad -> {
                    // Universal adjustments (User requested larger UI for desktop too)
                    float btnSize = 56f;
                    float btnPad = 1f;

                    // Row 1 (Always visible): FlipButton + first set of tools
                    pad.table(row1 -> {
                        row1.left();
                        row1.defaults().size(btnSize).pad(btnPad);
                        row1.add(flipButton);

                        // First tool always visible next to FlipButton
                        row1.button(Icon.settings, Styles.clearNonei, () -> {
                            // Refresh logic could be improved here, currently relying on static dialog in
                            // BrowserFeature
                            // But better to rebuild the list
                            // Use centralized dialog from BrowserFeature to ensure all components are
                            // listed
                            BrowserFeature.getComponentDialog().show();
                        }).tooltip(Core.bundle.get("mdt.message.lazy-components.title", "Components"));

                        row1.button(Icon.paste, Styles.clearNonei, () -> {
                            mindustry.ui.dialogs.BaseDialog dialog = BrowserFeature.getSchematicDialog().getIfEnabled();
                            if (dialog != null) {
                                dialog.show();
                            }
                        }).tooltip("Schematic Browser")
                                .visible(() -> BrowserFeature.getSchematicDialog().isEnabled());

                    }).left().row();

                    // Row 2+ (Expandable): Additional tools - only visible when expanded
                    pad.collapser(row2 -> {
                        row2.left();
                        row2.defaults().size(btnSize).pad(btnPad);

                        // Smart Drill toggle button
                        row2.button(Icon.production, Styles.clearNonei, () -> {
                            mindustrytool.features.content.browser.LazyComponent<?> comp = mindustrytool.features.gameplay.GameplayFeature
                                    .getSmartDrillComponent();
                            comp.setEnabled(!comp.isEnabled());
                        }).checked(b -> mindustrytool.features.gameplay.GameplayFeature.getSmartDrillComponent()
                                .isEnabled())
                                .tooltip("Smart Drill");

                        // Voice Settings button
                        row2.button(Icon.chat, Styles.clearNonei, () -> {
                            try {
                                // Get VoiceChatManager from PlayerConnectFeature's lazy component
                                mindustrytool.features.social.voice.VoiceChatManager vcManager = mindustrytool.features.social.multiplayer.PlayerConnectFeature
                                        .getVoiceChatManager();
                                if (vcManager != null) {
                                    vcManager.showSettings();
                                } else {
                                    Vars.ui.showInfo(
                                            "Voice Chat is not enabled.\nEnable it in 'Manage Components' first.");
                                }
                            } catch (Exception e) {
                                Vars.ui.showInfo("Voice Chat unavailable: " + e.getMessage());
                            }
                        }).tooltip("Voice Settings");

                    }, true, () -> !flipButton.fliped).left().row();

                }).margin(0f).update(pad -> {
                    if (waveInfoTable == null || !waveInfoTable.hasParent())
                        return;

                    // Get absolute screen coordinates of the Wave Info table (Bottom-Left corner)
                    // This fixes the issue where local coordinates are 0 or relative to a moving
                    // parent.
                    arc.math.geom.Vec2 v = new arc.math.geom.Vec2();
                    waveInfoTable.localToStageCoordinates(v.set(0, 0));
                    float absoluteBottomY = v.y;

                    // We want our Top to be at WaveInfo's Bottom.
                    // Pad is anchored Top-Left in a Full-Screen Group (hudGroup).
                    // Pad's default Y is ScreenHeight (Top).
                    // Translation needed = TargetY - ScreenHeight.
                    float screenHeight = Vars.ui.hudGroup.getHeight();
                    pad.setTranslation(0f, absoluteBottomY - screenHeight);
                });
            });
        });
    }
}
