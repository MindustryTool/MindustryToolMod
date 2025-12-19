package mindustrytool.plugins.tools;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.Plugin;
import mindustrytool.plugins.browser.LazyComponent;
import mindustrytool.plugins.browser.LazyComponentDialog;
import mindustrytool.plugins.browser.BrowserPlugin;

/**
 * Tools menu dialog now lives in its own plugin. It shows quick entries
 * (Map/Schematic browsers) and a "Manage Components" dialog combining
 * lazy components from all plugins.
 */
public class ToolsMenuDialog extends BaseDialog {

    public ToolsMenuDialog() {
        super("Tools");
        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().size(280, 60).pad(4);

        boolean hasAnyEnabled = false;

        // Map Browser - only show if enabled
        if (BrowserPlugin.getMapDialog().isEnabled()) {
            hasAnyEnabled = true;
            cont.button(Core.bundle.format("message.map-browser.title"), Icon.map, Styles.flatt, () -> {
                hide();
                var dialog = BrowserPlugin.getMapDialog().getIfEnabled();
                if (dialog != null)
                    dialog.show();
            }).row();
        }

        // Schematic Browser - only show if enabled
        if (BrowserPlugin.getSchematicDialog().isEnabled()) {
            hasAnyEnabled = true;
            cont.button(Core.bundle.format("message.schematic-browser.title", "Schematic Browser"), Icon.paste,
                    Styles.flatt, () -> {
                        hide();
                        var dialog = BrowserPlugin.getSchematicDialog().getIfEnabled();
                        if (dialog != null)
                            dialog.show();
                    }).row();
        }

        // Plugin-provided settings (auto-discovered from lazy components)
        cont.add(Core.bundle.get("message.tools.plugin-settings", "Plugin Settings")).color(arc.graphics.Color.gold).center().padTop(6).row();

        for (Plugin p : Main.getPlugins()) {
            Seq<?> comps = p.getLazyComponentsInstance();
            if (comps == null) continue;
            for (Object o : comps) {
                if (!(o instanceof LazyComponent)) continue;
                LazyComponent<?> comp = (LazyComponent<?>) o;
                // Skip components that already have dedicated quick entries (Map/Schematic)
                if ("MapBrowser".equals(comp.getName()) || "SchematicBrowser".equals(comp.getName()))
                    continue;
                if (!comp.hasSettings()) continue;
                String label = p.getName() + ": " + comp.getName();
                cont.button(label, Icon.settings, Styles.flatt, () -> {
                    hide();
                    try {
                        comp.openSettings();
                    } catch (Exception e) {
                        arc.util.Log.err(e);
                    }
                }).row();
            }
        }

        // Manage Components - always show, combines lazy components from all plugins
        cont.button(Core.bundle.get("message.lazy-components.title", "Manage Components"), Icon.settings, Styles.flatt,
                () -> {
                    hide();

                    Seq<LazyComponent<?>> allComponents = new Seq<>();
                    for (Plugin p : Main.getPlugins()) {
                        Seq<?> comps = p.getLazyComponentsInstance();
                        if (comps == null) continue;
                        for (Object o : comps) {
                            if (o instanceof LazyComponent) {
                                allComponents.add((LazyComponent<?>) o);
                            }
                        }
                    }

                    new LazyComponentDialog(allComponents).show();
                }).row();

        // Show message if no quick items are available
        if (!hasAnyEnabled) {
            cont.add(Core.bundle.get("message.lazy-components.all-disabled", "All components disabled"))
                    .color(Color.gray).padTop(10);
        }
    }
}