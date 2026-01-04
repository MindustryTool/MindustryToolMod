package mindustrytool.features.content.browser;

import arc.Core;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.ui.dialogs.*;

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

        // Manage Components - always show
        cont.button(Core.bundle.get("mdt.message.lazy-components.title", "Manage Components"), Icon.settings,
                Styles.flatt,
                () -> {
                    hide();
                    // Use centralized component registry
                    new LazyComponentDialog(mindustrytool.utils.ComponentRegistry.getAllComponents()).show();
                }).row();

        // Mod Info - Opens the README documentation
        cont.button("Mod Info", Icon.info, Styles.flatt, () -> {
            hide();
            ModInfoDialog.open();
        }).row();

        // Update Center - Opens the Premium Update Center
        cont.button("Update Center", Icon.refresh, Styles.flatt, () -> {
            hide();
            UpdateCenterDialog.open();
        }).row();
    }
}
