package mindustrytool.plugins.voicechat;

import arc.Core;
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
import mindustry.ui.Fonts;
import mindustry.gen.Icon;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.g2d.GlyphLayout;
import mindustrytool.Plugin;

public class VoiceChatPlugin implements Plugin {

    // Use Object type to prevent VoiceChatManager class loading on Android
    private Object manager;
    private boolean buttonAdded = false;
    private TextButton voiceButton;
    private float debugTimer = 0f;

    private boolean isDesktop = false;
    private static final GlyphLayout layout = new GlyphLayout();


    @Override
    public String getName() {
        return "VoiceChat";
    }

    @Override
    public void init() {
        // Voice Chat now supports both Desktop and Android (via VoiceChatCompanion app)
        isDesktop = !Core.app.isMobile();

        try {
            manager = new VoiceChatManager();
            ((VoiceChatManager) manager).init();

            // Create a reusable button
            voiceButton = new TextButton("Voice Settings", Styles.defaultt);
            voiceButton.clicked(() -> ((VoiceChatManager) manager).showSettings());

            Events.run(EventType.Trigger.update, this::updateUI);
            
            Events.run(EventType.Trigger.draw, this::drawWorld);

            Log.info("[VoiceChat] Initialized successfully on @.", isDesktop ? "Desktop" : "Mobile");
        } catch (Throwable e) {
            Log.err("[VoiceChat] Failed to initialize: " + e.getMessage());
        }
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

        // Try to find the fixed container (Dialog body) to pin the button at the bottom
        // Logic: content -> ScrollPane -> Table (Dialog Body)
        Table fixedParent = null;
        Element scrollPane = content.parent;

        if (scrollPane instanceof ScrollPane && scrollPane.parent instanceof Table) {
            fixedParent = (Table) scrollPane.parent;
        }

        if (fixedParent != null) {
            // Desktop/Standard: Pin to bottom of dialog
            fixedParent.row();
            fixedParent.add(voiceButton).size(200f, 40f).pad(5f).center();
            if (shouldLog)
                Log.info("[VoiceChat] Added fixed button to @", fixedParent.getClass().getSimpleName());
        } else {
            // Mobile/Fallback: Add to end of scrolling list
            content.row();
            content.add(voiceButton).size(200f, 40f).pad(5f).center();
            if (shouldLog)
                Log.info("[VoiceChat] Added fallback button to content list");
        }

        buttonAdded = true;
    }

    private void drawWorld() {
        if (manager == null || !((VoiceChatManager) manager).isEnabled()) return;
        
        VoiceChatManager voiceManager = (VoiceChatManager) manager;

        for (mindustry.gen.Player player : mindustry.gen.Groups.player) {
            if (player.unit() == null) continue;

            boolean isSpeaking = voiceManager.isSpeaking(player.uuid());
            boolean isSelf = (player == Vars.player);
            
            if (isSelf) {
                 isSpeaking = voiceManager.isSelfSpeaking();
            }

            // Only show if speaking
            if (isSpeaking) {
                float x = player.unit().x;
                float y = player.unit().y;
                
                float iconSize = 5f; // Small icon to avoid distraction
                
                TextureRegion micIcon = Core.atlas.find("mindustry-tool-mic-on", Icon.chat.getRegion());
                
                // Use a softer green (Pastel)
                Draw.color(Color.valueOf("88ff88"));
                Draw.rect(micIcon, x, y, iconSize, iconSize);
                Draw.reset();
            }
        }
    }

    @Override
    public void dispose() {
        if (isDesktop && manager != null) {
            ((VoiceChatManager) manager).dispose();
        }
        if (voiceButton != null) {
            voiceButton.remove();
        }
    }
}
