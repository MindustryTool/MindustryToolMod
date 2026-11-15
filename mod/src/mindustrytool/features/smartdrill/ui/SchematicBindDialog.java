package mindustrytool.features.smartdrill.ui;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import solim.core.Component;
import solim.overlay.SolimDialog;

public class SchematicBindDialog extends SolimDialog {

    public SchematicBindDialog(SmartDrillFeature feature) {
        super(Core.bundle.get("feature.smart-drill.panel.rig.bind.title"));
        name("smart-drill-bind-dialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);
        children(() -> new BindView(feature));
    }

    private final class BindView extends Component {

        private final SmartDrillFeature feature;

        BindView(SmartDrillFeature feature) {
            this.feature = feature;
        }

        @Override
        protected Element build() {
            return SettingScaffold.build(() -> {
                SettingCard.header(feature, "AUTOMATION");
                SettingCard.section(Icon.link, Core.bundle.get("feature.smart-drill.panel.rig.bind.section"),
                        () -> {
                            reactiveGrid(schematics())
                                    .props(gap(unit(1.5f)))
                                    .key(Schematic::name)
                                    .items(this::row);
                        });
                SettingCard.section(Icon.cancel, Core.bundle.get("feature.smart-drill.panel.rig.bind.clear"),
                        () -> {
                            button(Core.bundle.get("feature.smart-drill.panel.rig.bind.clear.action"),
                                    this::clearBinding)
                                    .props(style(WebStyles.outlineText()), height(unit(11)), growX());
                        });
            });
        }

        private List<Schematic> schematics() {
            List<Schematic> result = new ArrayList<>();
            if (Vars.schematics == null) {
                return result;
            }
            Seq<Schematic> all = Vars.schematics.all();
            for (int i = 0; i < all.size; i++) {
                Schematic schematic = all.get(i);
                if (schematic != null && schematic.file != null) {
                    result.add(schematic);
                }
            }
            return result;
        }

        private Component row(Schematic schematic) {
            String name = schematic.name();
            String file = schematic.file != null ? schematic.file.name() : "";
            boolean bound = file.equals(feature.getBinding());
            return button()
                    .props(
                            style(bound ? WebStyles.secondary() : WebStyles.ghost()),
                            growX(),
                            height(unit(11)),
                            checked(bound),
                            tooltip(name))
                    .onClick(() -> bind(file))
                    .children(() -> text(name).props(growX(), left()));
        }

        private void bind(String file) {
            feature.bind(file);
            hide();
        }

        private void clearBinding() {
            feature.clearBinding();
            hide();
        }
    }
}