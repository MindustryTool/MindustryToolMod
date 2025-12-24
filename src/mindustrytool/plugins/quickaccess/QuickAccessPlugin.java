package mindustrytool.plugins.quickaccess;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.Plugin;
import mindustrytool.plugins.browser.BrowserPlugin;

public class QuickAccessPlugin implements Plugin {

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
                    // Row 1 (Always visible): FlipButton + first set of tools
                    pad.table(row1 -> {
                        row1.left();
                        row1.defaults().size(40f);
                        row1.add(flipButton);

                        // First tool always visible next to FlipButton
                        row1.button(Icon.settings, Styles.clearNonei, () -> {
                            BrowserPlugin.getComponentDialog().show();
                        }).tooltip(Core.bundle.get("message.lazy-components.title", "Components"));

                        row1.button(Icon.paste, Styles.clearNonei, () -> {
                            var dialog = BrowserPlugin.getSchematicDialog().getIfEnabled();
                            if (dialog != null) {
                                dialog.show();
                            }
                        }).tooltip("Schematic Browser")
                                .visible(() -> BrowserPlugin.getSchematicDialog().isEnabled());

                    }).left().row();

                    // Row 2+ (Expandable): Additional tools - only visible when expanded
                    pad.table(row2 -> {
                        row2.left();
                        row2.defaults().size(40f);
                        // Placeholder for more tools in the future
                    }).left().visible(() -> flipButton.fliped).row();

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
