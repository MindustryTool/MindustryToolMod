package mindustrytool.features.logicvisualizer;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.core.Component;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Logic Visualizer options.
 */
public class LogicVisualizerSettingsView extends Component {

    private final LogicVisualizerFeature feature;

    public LogicVisualizerSettingsView(LogicVisualizerFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            filtersCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "TACTICAL");
    }

    private Component filtersCard() {
        return SettingCard.section(Icon.units, Core.bundle.get("feature.logic-visualizer.settings.section.filters"), () -> {
            checkboxRow(
                    Core.bundle.get("feature.logic-visualizer.settings.logic-unit-ally"),
                    Core.bundle.get("feature.logic-visualizer.settings.logic-unit-ally.description"),
                    feature.showAllyUnitsConfig.signal()
            );

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(
                    Core.bundle.get("feature.logic-visualizer.settings.logic-unit-enemy"),
                    Core.bundle.get("feature.logic-visualizer.settings.logic-unit-enemy.description"),
                    feature.showEnemyUnitsConfig.signal()
            );
        });
    }

    private Component checkboxRow(String label, String description, Signal<Boolean> signal) {
        return row().growX().gap(unit(2)).center().children(() -> {
            column().growX().left().children(() -> {
                text(label).color(Color.white).left();
                text(description).color(WebStyles.Colors.GHOST_FG).wrap().left();
            });
            spacer();
            checkbox("", signal);
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> button()
                .style(WebStyles.outline())
                .growX()
                .height(unit(10))
                .onClick(feature::resetToDefaults)
                .children(() -> row().gap(unit(2)).center().children(() -> {
                    icon(Icon.refresh).size(unit(5)).color(WebStyles.Colors.GHOST_FG);
                    text(Core.bundle.get("feature.logic-visualizer.settings.reset"))
                            .color(WebStyles.Colors.GHOST_FG);
                })));
    }
}
