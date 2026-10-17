package mindustrytool.features.prettychat.ui;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.padding;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.TextArea;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.prettychat.Prettier;
import solim.modifier.Values;
import solim.core.Units;
import solim.core.Component;
import solim.overlay.SolimDialog;

public class PrettyChatEditDialog extends SolimDialog {

    public PrettyChatEditDialog(Prettier prettier, Runnable onSave) {
        super(Core.bundle.format("feature.pretty-chat.settings.edit-script-title", prettier.name()) + prettier.name());

        name("prettyChatEditDialog");
        addCloseButton();
        closeOnBack();

        TextArea area = new TextArea(prettier.getScript() != null ? prettier.getScript() : "");

        String hint;
        if ("gradient".equals(prettier.id())) {
            hint = Core.bundle.has("feature.pretty-chat.settings.gradient-hint")
                    ? Core.bundle.get("feature.pretty-chat.settings.gradient-hint")
                    : "Enter two comma-separated hex colors (e.g. #ff5e36,#9b51e0) or preset: cyberpunk, ocean, fire, neon.";
        } else if ("fancybrackets".equals(prettier.id())) {
            hint = Core.bundle.has("feature.pretty-chat.settings.brackets-hint")
                    ? Core.bundle.get("feature.pretty-chat.settings.brackets-hint")
                    : "Enter decorative template containing <message>, e.g. \u3010 <message> \u3011 or \u2726 <message> \u2726.";
        } else {
            hint = Core.bundle.get("feature.pretty-chat.settings.script-hint");
        }

        children(() -> {
            column().props(grow(), Values.maxWidth(Units.dvw.map(w -> Math.min(w * 0.9f, 750f))), padding(unit(3)), gap(unit(2)))
                    .children(() -> {
                        text(hint)
                                .color(Color.lightGray)
                                .wrap()
                                .growX();

                        new Component() {
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
                Core.bundle.get("feature.pretty-chat.settings.reset-default"),
                Icon.refresh,
                200f,
                54f,
                () -> {
                    String def = prettier.getDefaultScript();
                    area.setText(def != null ? def : "");
                });

        actionButton(
                Core.bundle.get("feature.pretty-chat.settings.save"),
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