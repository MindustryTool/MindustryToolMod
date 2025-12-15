package mindustrytool.plugins.browser;

import arc.Core;
import arc.Events;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.Main;
import mindustrytool.Plugin;

/**
 * Self-contained Browser plugin for browsing maps and schematics
 * from the MindustryTool website.
 */
public class BrowserPlugin implements Plugin {
    public static BaseDialog mapDialog;
    public static BaseDialog schematicDialog;
    private static TextureRegionDrawable toolIcon;
    
    @Override public String getName() { return "Browser"; }
    @Override public int getPriority() { return 50; }
    
    @Override public void init() {
        BrowserDirInit.init();
        mapDialog = new BrowserDialog<>(ContentType.MAP, ContentData.class, new MapInfoDialog());
        schematicDialog = new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class, new SchematicInfoDialog());
        Events.on(ClientLoadEvent.class, e -> addButtons());
    }
    
    private static void addButtons() {
        loadIcon();
        
        // Add browse button to schematics dialog
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            schematicDialog.show();
        });
        
        // Add map browser to menu
        String map = Core.bundle.format("message.map-browser.title");
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(map, Icon.map, () -> mapDialog.show());
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Tools", toolIcon, () -> {},
                new MenuButton(map, Icon.map, () -> mapDialog.show())));
        }
    }
    
    private static void loadIcon() {
        try {
            Pixmap original = new Pixmap(Vars.mods.getMod(Main.class).root.child("icon.png"));
            int targetSize = 36;
            Pixmap scaled = new Pixmap(targetSize, targetSize);
            scaled.draw(original, 0, 0, original.width, original.height, 0, 0, targetSize, targetSize, true);
            original.dispose();
            Texture tex = new Texture(scaled);
            tex.setFilter(Texture.TextureFilter.linear);
            scaled.dispose();
            toolIcon = new TextureRegionDrawable(new TextureRegion(tex));
        } catch (Exception e) {
            toolIcon = new TextureRegionDrawable(Icon.menu.getRegion());
        }
    }
    
    /** Show the map browser dialog. */
    public static void showMapBrowser() {
        if (mapDialog != null) mapDialog.show();
    }
    
    /** Show the schematic browser dialog. */
    public static void showSchematicBrowser() {
        if (schematicDialog != null) schematicDialog.show();
    }
}
