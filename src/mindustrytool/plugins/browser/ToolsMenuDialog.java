package mindustrytool.plugins.browser;

import arc.Core;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.plugins.playerconnect.PlayerConnectPlugin;

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

        boolean hasAnyEnabled = false;

        // Map Browser - only show if enabled
        if (BrowserPlugin.getMapDialog().isEnabled()) {
            hasAnyEnabled = true;
            cont.button(Core.bundle.format("message.map-browser.title"), Icon.map, Styles.flatt, () -> {
                hide();
                BaseDialog dialog = BrowserPlugin.getMapDialog().getIfEnabled();
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
                        BaseDialog dialog = BrowserPlugin.getSchematicDialog().getIfEnabled();
                        if (dialog != null)
                            dialog.show();
                    }).row();
        }

        // Manage Components - always show
        cont.button(Core.bundle.get("message.lazy-components.title", "Manage Components"), Icon.settings, Styles.flatt,
                () -> {
                    hide();
                    // Combine all lazy components from both plugins
                    Seq<LazyComponent<?>> allComponents = new Seq<>();
                    allComponents.addAll(BrowserPlugin.getLazyComponents());
                    allComponents.addAll(PlayerConnectPlugin.getLazyComponents());
                    new LazyComponentDialog(allComponents).show();
                }).row();

        // Mod Info - Opens the README documentation
        cont.button("Mod Info", Icon.info, Styles.flatt, () -> {
            hide();
            mindustrytool.ui.ModInfoDialog.open();
        }).row();

        // Update Center - Opens the Premium Update Center
        cont.button("Update Center", Icon.refresh, Styles.flatt, () -> {
            hide();
            mindustrytool.ui.UpdateCenterDialog.open();
        }).row();

        // Show message if all components disabled
        if (!hasAnyEnabled) {
            cont.add(Core.bundle.get("message.lazy-components.all-disabled", "All components disabled"))
                    .color(arc.graphics.Color.gray).padTop(10);
        }
    }
}
