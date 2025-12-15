package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.layout.*;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Custom Tools menu dialog that dynamically shows/hides items based on
 * component enabled state.
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

        // Map Browser - only show if enabled
        if (BrowserPlugin.getMapDialog().isEnabled()) {
            cont.button(Core.bundle.format("message.map-browser.title"), Icon.map, Styles.flatt, () -> {
                hide();
                var dialog = BrowserPlugin.getMapDialog().getIfEnabled();
                if (dialog != null)
                    dialog.show();
            }).row();
        }

        // Schematic Browser - only show if enabled
        if (BrowserPlugin.getSchematicDialog().isEnabled()) {
            cont.button(Core.bundle.format("message.schematic-browser.title", "Schematic Browser"), Icon.paste,
                    Styles.flatt, () -> {
                        hide();
                        var dialog = BrowserPlugin.getSchematicDialog().getIfEnabled();
                        if (dialog != null)
                            dialog.show();
                    }).row();
        }

        // Manage Components - always show
        cont.button(Core.bundle.get("message.lazy-components.title", "Manage Components"), Icon.settings, Styles.flatt,
                () -> {
                    hide();
                    new LazyComponentDialog(BrowserPlugin.getLazyComponents()).show();
                }).row();

        // Show message if all components disabled
        if (!BrowserPlugin.getMapDialog().isEnabled() && !BrowserPlugin.getSchematicDialog().isEnabled()) {
            cont.add(Core.bundle.get("message.lazy-components.all-disabled", "All components disabled"))
                    .color(arc.graphics.Color.gray).padTop(10);
        }
    }
}
