package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class ActiveSelectionBar extends Component {

    private final ArmyControlFeature feature;
    private final Readable<Float> scale;

    public ActiveSelectionBar(ArmyControlFeature feature, Readable<Float> scale) {
        this.feature = feature;
        this.scale = scale;
    }

    @Override
    protected Element build() {
        Readable<SelectionMatrixState> matrixSignal = Signal.computed(() ->
                new SelectionMatrixState(feature.discoveredTypesSignal().get(), feature.activeSelectionSignal().get())
        );

        Readable<Float> chipHeight = scale.map(s -> unit(9) * (s != null ? s : 1f));
        Readable<Float> chipIconSize = scale.map(s -> unit(5) * (s != null ? s : 1f));
        Readable<Float> batchButtonHeight = scale.map(s -> unit(7.5f) * (s != null ? s : 1f));
        Readable<Float> batchButtonWidth = scale.map(s -> unit(13f) * (s != null ? s : 1f));

        return column()
                .props(growX(), gap(unit(1.5f)))
                .children(() -> {
                    dynamic(matrixSignal, state -> {
                        if (state == null || state.pool == null || state.pool.size == 0) {
                            return;
                        }

                        Seq<UnitType> types = new Seq<>();
                        for (UnitType key : state.pool.keys()) {
                            types.add(key);
                        }
                        types.sort((a, b) -> Integer.compare(state.pool.get(b, 0), state.pool.get(a, 0)));

                        row()
                                .props(growX(), gap(unit(1.5f)), top())
                                .children(() -> {
                                    column()
                                            .props(gap(unit(1)), top())
                                            .children(() -> {
                                                button()
                                                        .props(
                                                                style(WebStyles.filterChip()),
                                                                padding(unit(1)),
                                                                minWidth(batchButtonWidth),
                                                                growX(),
                                                                height(batchButtonHeight),
                                                                prefHeight(batchButtonHeight),
                                                                tooltip(Core.bundle.get("feature.army-control.batch.all-tooltip", "Select all discovered units"))
                                                        )
                                                        .onClick(feature::selectAllPool)
                                                        .children(() -> text(Core.bundle.get("feature.army-control.batch.all", "All"))
                                                                .props(color(WebStyles.Colors.PRIMARY_FG), fontScale(scale), center()));

                                                button()
                                                        .props(
                                                                style(WebStyles.filterChip()),
                                                                padding(unit(1)),
                                                                minWidth(batchButtonWidth),
                                                                growX(),
                                                                height(batchButtonHeight),
                                                                prefHeight(batchButtonHeight),
                                                                tooltip(Core.bundle.get("feature.army-control.batch.none-tooltip", "Clear selection while keeping discovered units"))
                                                        )
                                                        .onClick(feature::selectNonePool)
                                                        .children(() -> text(Core.bundle.get("feature.army-control.batch.none", "None"))
                                                                .props(color(WebStyles.Colors.PRIMARY_FG), fontScale(scale), center()));

                                                button()
                                                        .props(
                                                                style(WebStyles.filterChip()),
                                                                padding(unit(1)),
                                                                minWidth(batchButtonWidth),
                                                                growX(),
                                                                height(batchButtonHeight),
                                                                prefHeight(batchButtonHeight),
                                                                tooltip(Core.bundle.get("feature.army-control.batch.invert-tooltip", "Invert active and inactive units"))
                                                        )
                                                        .onClick(feature::invertPool)
                                                        .children(() -> text(Core.bundle.get("feature.army-control.batch.invert", "Invert"))
                                                                .props(color(WebStyles.Colors.PRIMARY_FG), fontScale(scale), center()));
                                            });

                                    wrap()
                                            .props(growX(), gap(unit(1.5f)), left())
                                            .children(() -> {
                                                for (UnitType type : types) {
                                                    int poolCount = state.pool.get(type, 0);
                                                    int activeCount = state.active != null ? state.active.get(type, 0) : 0;
                                                    boolean isActive = activeCount > 0;
                                                    int displayCount = isActive ? activeCount : poolCount;

                                                    String hint = isActive
                                                            ? Core.bundle.get("feature.army-control.hint.toggle-off", "Tap to exclude")
                                                            : Core.bundle.get("feature.army-control.hint.toggle-on", "Tap to include");

                                                    button()
                                                            .props(
                                                                    style(isActive ? WebStyles.actionChip() : WebStyles.ghost()),
                                                                    padding(unit(1)),
                                                                    height(chipHeight),
                                                                    prefHeight(chipHeight),
                                                                    tooltip(type.localizedName + ": " + hint)
                                                            )
                                                            .onClick(() -> feature.toggleUnitType(type))
                                                            .children(() -> row().props(gap(unit(1)), center()).children(() -> {
                                                                if (type.uiIcon != null) {
                                                                    image(new TextureRegionDrawable(type.uiIcon))
                                                                            .props(
                                                                                    size(chipIconSize),
                                                                                    prefWidth(chipIconSize),
                                                                                    prefHeight(chipIconSize),
                                                                                    color(isActive ? Color.white : WebStyles.Colors.DISABLED_FG)
                                                                            );
                                                                }
                                                                text(String.valueOf(displayCount))
                                                                        .props(
                                                                                color(isActive ? WebStyles.Colors.PRIMARY_FG : WebStyles.Colors.DISABLED_FG),
                                                                                fontScale(scale)
                                                                        );
                                                            }));
                                                }
                                            });
                                });
                    });
                }).element();
    }

    private static final class SelectionMatrixState {
        final @Nullable ObjectIntMap<UnitType> pool;
        final @Nullable ObjectIntMap<UnitType> active;

        SelectionMatrixState(@Nullable ObjectIntMap<UnitType> pool, @Nullable ObjectIntMap<UnitType> active) {
            this.pool = pool;
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SelectionMatrixState)) {
                return false;
            }
            SelectionMatrixState other = (SelectionMatrixState) o;
            return ArmyControlFeature.sameSelection(pool, other.pool)
                    && ArmyControlFeature.sameSelection(active, other.active);
        }

        @Override
        public int hashCode() {
            return (pool != null ? pool.size : 0) * 31 + (active != null ? active.size : 0);
        }
    }
}
