package mindustrytool.plugins.voicechat;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.SnapshotSeq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.ui.Styles;
import mindustrytool.Plugin;

public class VoiceChatPlugin implements Plugin {

    private VoiceChatManager manager;

    @Override
    public String getName() {
        return "VoiceChat";
    }

    @Override
    public void init() {
        manager = new VoiceChatManager();
        manager.init();

        // UI Injection logic
        Events.run(EventType.Trigger.update, () -> {
            if (Vars.ui.listfrag.content.hasParent()) {
                updatePlayerListUI();
            }
        });
    }

    private void updatePlayerListUI() {
        Table content = Vars.ui.listfrag.content;
        if (content == null) return;

        SnapshotSeq<Element> children = content.getChildren();
        for (Element child : children) {
            if (!(child instanceof Table)) continue;
            Table row = (Table) child;

            Label nameLabel = row.find(e -> e instanceof Label);
            if (nameLabel == null) continue;
            
            // Identify player by name (fallback as uuid isn't easily in row)
            String rawName = Strings.stripColors(nameLabel.getText().toString());
            
            // Only add button to OUR OWN row (Self) for settings
            if (rawName.equals(Strings.stripColors(Vars.player.name))) {
                if (row.find("voice-settings-btn") != null) continue;

                row.button(Icon.settings, Styles.clearNonei, () -> {
                    manager.showSettings();
                    Vars.ui.listfrag.rebuild(); 
                }).name("voice-settings-btn").size(45f).padLeft(5f);
            }
        }
    }

    @Override
    public void dispose() {
        if (manager != null) {
            manager.dispose();
        }
    }
}
