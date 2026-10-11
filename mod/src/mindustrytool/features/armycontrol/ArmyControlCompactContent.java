package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Nullable;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Shared compact content for ArmyControl, embedded by both the QuickAccess
 * popup shell and the persistent HUD shell. Contains the icon toolbar,
 * the active selection chips, and the saved control groups — no title,
 * no section labels. Sections are separated by dividers that collapse
 * together with their section when empty.
 */
public class ArmyControlCompactContent extends BaseComponent {

    private final ArmyControlFeature feature;
    private final @Nullable Runnable onClose;

    public ArmyControlCompactContent(ArmyControlFeature feature, @Nullable Runnable onClose) {
        this.feature = feature;
        this.onClose = onClose;
    }

    @Override
    protected Element build() {
        Readable<Boolean> hasSelection = feature.activeSelectionSignal()
                .map(map -> map != null && map.size > 0);
        Readable<Boolean> hasGroups = feature.controlGroupsSignal()
                .map(groups -> groups != null && groups.size > 0);

        return column()
                .growX()
                .gap(unit(1.5f))
                .children(() -> {
                    new ArmyControlToolbar(feature, onClose);

                    // GrowX is load-bearing: without it the When container sizes to the
                    // divider's ~zero preferred width and the line renders invisible.
                    when(hasSelection)
                            .growX()
                            .thenDo(() -> divider().color(WebStyles.Colors.SECTION_BORDER));

                    new ActiveSelectionBar(feature);

                    when(hasGroups)
                            .growX()
                            .thenDo(() -> divider().color(WebStyles.Colors.SECTION_BORDER));

                    new ControlGroupList(feature);
                }).element();
    }
}

