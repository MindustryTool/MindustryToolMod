package mindustrytool.plugins.voicechat;

import arc.Events;
import arc.scene.Element;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.ui.Styles;
import mindustrytool.Plugin;

public class VoiceChatPlugin implements Plugin {

    private VoiceChatManager manager;
    private boolean buttonAdded = false;
    private TextButton voiceButton;
    private float debugTimer = 0f;

    @Override
    public String getName() {
        return "VoiceChat";
    }

    @Override
    public void init() {
        manager = new VoiceChatManager();
        manager.init();

        // Create a reusable button
        voiceButton = new TextButton("Voice Settings", Styles.defaultt);
        voiceButton.clicked(() -> manager.showSettings());

        Events.run(EventType.Trigger.update, this::updateUI);
    }

    private void updateUI() {
        if (Vars.ui == null || Vars.ui.listfrag == null || Vars.ui.listfrag.content == null)
            return;

        Table content = Vars.ui.listfrag.content;

        // Debug logging (throttled)
        debugTimer += Time.delta;
        boolean shouldLog = debugTimer > 180f; // every 3 sec
        if (shouldLog)
            debugTimer = 0f;

        if (!content.hasParent()) {
            if (buttonAdded && shouldLog) {
                Log.info("[VoiceChat] List closed, removing button");
            }
            // List is closed, reset flag
            buttonAdded = false;
            if (voiceButton != null)
                voiceButton.remove();
            return;
        }

        if (buttonAdded && voiceButton.hasParent())
            return;

        // Debug hierarchy
        if (shouldLog) {
            Log.info("[VoiceChat] Hierarchy traversal:");
            Element current = content;
            while (current != null) {
                Log.info("  - @ (visible: @)", current.getClass().getSimpleName(), current.visible);
                current = current.parent;
            }
        }

        // Navigate up the hierarchy
        Element scrollPane = content.parent;
        if (scrollPane instanceof ScrollPane) {
            Element dialogTable = scrollPane.parent;
            if (dialogTable instanceof Table) {
                Table parent = (Table) dialogTable;

                // Add button at the bottom
                parent.row();
                parent.add(voiceButton).size(200f, 40f).pad(5f).center();
                buttonAdded = true;
                Log.info("[VoiceChat] Voice Settings button added to @", parent.getClass().getSimpleName());
            } else {
                if (shouldLog)
                    Log.warn("[VoiceChat] Expected Table parent of ScrollPane, found: @",
                            (dialogTable == null ? "null" : dialogTable.getClass().getSimpleName()));
            }
        } else {
            if (shouldLog)
                Log.warn("[VoiceChat] Expected ScrollPane parent of content, found: @",
                        (scrollPane == null ? "null" : scrollPane.getClass().getSimpleName()));
        }
    }

    @Override
    public void dispose() {
        if (manager != null) {
            manager.dispose();
        }
        if (voiceButton != null) {
            voiceButton.remove();
        }
    }
}
