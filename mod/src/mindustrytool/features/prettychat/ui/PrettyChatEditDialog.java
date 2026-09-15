package mindustrytool.features.prettychat.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.TextArea;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.prettychat.Prettier;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;

/**
 * Dialog allowing users to customize an editable Prettier's script or template.
 */
public class PrettyChatEditDialog extends SolimDialog {

    public PrettyChatEditDialog(Prettier prettier, Runnable onSave) {
        super(Core.bundle != null
                ? Core.bundle.format("feature.pretty-chat.settings.edit-script-title", prettier.name())
                : "Edit Script - " + prettier.name());

        name("prettyChatEditDialog");
        addCloseButton();
        closeOnBack();

        TextArea area = new TextArea(prettier.getScript() != null ? prettier.getScript() : "");

        children(() -> {
            column().grow().maxWidth(dvw(90f).map(w -> Math.min(w, 750f))).padding(unit(3)).gap(unit(2)).children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.pretty-chat.settings.script-hint",
                                "Use <message> as a placeholder for the chat text. JavaScript expressions are supported on Desktop.")
                        : "Use <message> as a placeholder for the chat text. JavaScript expressions are supported on Desktop.")
                        .color(Color.lightGray)
                        .wrap()
                        .growX();

                new BaseComponent() {
                    @Override
                    protected Element build() {
                        Table table = new Table(Styles.black6);
                        table.margin(8f);
                        table.add(area).grow().minHeight(180f);
                        return table;
                    }
                };
            });
        });

        actionButton(
                Core.bundle != null
                        ? Core.bundle.get("feature.pretty-chat.settings.reset-default", "Reset to Default")
                        : "Reset to Default",
                Icon.refresh,
                200f,
                54f,
                () -> {
                    String def = prettier.getDefaultScript();
                    area.setText(def != null ? def : "");
                });

        actionButton(
                Core.bundle != null
                        ? Core.bundle.get("feature.pretty-chat.settings.save", "Save")
                        : "Save",
                Icon.save,
                160f,
                54f,
                () -> {
                    prettier.setScript(area.getText());
                    onSave.run();
                    hide();
                });
    }
}
