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
import solim.reactive.Signal;

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
        return SettingCard.section(Icon.units, Core.bundle.get("feature.logic-visualizer.settings.section.filters"),
                () -> {
                    checkboxRow(
                            Core.bundle.get("feature.logic-visualizer.settings.logic-unit-ally"),
                            Core.bundle.get("feature.logic-visualizer.settings.logic-unit-ally.description"),
                            feature.showAllyUnitsConfig.signal());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkboxRow(
                            Core.bundle.get("feature.logic-visualizer.settings.logic-unit-enemy"),
                            Core.bundle.get("feature.logic-visualizer.settings.logic-unit-enemy.description"),
                            feature.showEnemyUnitsConfig.signal());
                });
    }

    private Component checkboxRow(String label, String description, Signal<Boolean> signal) {
        return row().props(growX(), gap(unit(2)), center()).children(() -> {
            column().props(growX(), left()).children(() -> {
                text(label).props(color(Color.white), left());
                text(description).props(color(WebStyles.Colors.GHOST_FG), textWrap(true), left());
            });
            spacer();
            checkbox("", signal);
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> button()
                .props(style(WebStyles.outline()), growX(), height(unit(10)), prefHeight(unit(10)))
                .onClick(feature::resetToDefaults)
                .children(() -> row().props(gap(unit(2)), center()).children(() -> {
                    icon(Icon.refresh).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(WebStyles.Colors.GHOST_FG));
                    text(Core.bundle.get("feature.logic-visualizer.settings.reset"))
                            .props(color(WebStyles.Colors.GHOST_FG));
                })));
    }
}
