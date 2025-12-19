package mindustrytool.plugins.tools;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.Plugin;

/**
 * Tools plugin: responsible for the global Tools menu and management UI.
 */
public class ToolsPlugin implements Plugin {

    @Override
    public String getName() {
        return "Tools";
    }

    @Override
    public int getPriority() {
        // High priority so Tools button is available early
        return 100;
    }

    @Override
    public void init() {
        Events.on(ClientLoadEvent.class, e -> {
            addButtons();
        });
    }

    private static arc.scene.style.TextureRegionDrawable toolIcon;

    private static void loadIcon() {
        try {
            arc.graphics.Pixmap original = new arc.graphics.Pixmap(Vars.mods.getMod(mindustrytool.Main.class).root.child("icon.png"));
            int targetSize = 36;
            arc.graphics.Pixmap scaled = new arc.graphics.Pixmap(targetSize, targetSize);
            scaled.draw(original, 0, 0, original.width, original.height, 0, 0, targetSize, targetSize, true);
            original.dispose();
            arc.graphics.Texture tex = new arc.graphics.Texture(scaled);
            tex.setFilter(arc.graphics.Texture.TextureFilter.linear);
            scaled.dispose();
            toolIcon = new arc.scene.style.TextureRegionDrawable(new arc.graphics.g2d.TextureRegion(tex));
        } catch (Exception e) {
            toolIcon = null;
        }
    }

    private static void addButtons() {
        loadIcon();
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton("Tools", Icon.settings, () -> new ToolsMenuDialog().show());
        } else {
            // Use plugin icon if available, otherwise default to menu icon
            Vars.ui.menufrag.addButton("Tools", toolIcon != null ? toolIcon : Icon.menu, () -> new ToolsMenuDialog().show());
        }
    }
}